package controllers

import akka.actor.{Props, ActorSystem, ActorRef}
import akka.routing.RoundRobinGroup
import play.api.mvc._

import mongo.{MongoRepo, MongoRepoLike}
import scala.concurrent.Future
import akka.pattern.ask
import actors.InventoryGetter.GetInventory
import actors.InventoryUpdater.UpdateInventory
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.concurrent.TrieMap
import metrics.StatsDSender
import actors.{InventoryGetter, InventoryUpdater}
import actors.EventSource.RegisterListener
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.util.Timeout
import metrics.StatsDSender.{IncrementCounter, SendTimer}

class ApplicationLike(mongoRepo: MongoRepoLike) extends Controller {

  val r = scala.util.Random

  val inventoryUpdaters = TrieMap[String, ActorRef]()
  val inventoryGetters = TrieMap[String, ActorRef]()

  val akkaSystem = ActorSystem("reactive-inventory")

  //Actor to send metrics
  val statsDSender = akkaSystem.actorOf(Props[StatsDSender], "StatsDSender")

  implicit val timeout = new Timeout(20 seconds)

  //initialize inventory randomly by creating an Inventory manager for each sku
  for (sku <- 1 to 100) {
    val quantity = r.nextInt(10000) + 10000
    val skuString = sku.toString
    val inventoryUpdater = akkaSystem.actorOf(
      Props(classOf[InventoryUpdater], skuString, quantity, MongoRepo))
    inventoryUpdaters.put(sku.toString, inventoryUpdater)
    val routees: Seq[ActorRef] =
      for (routeeId <- 1 to 10) yield {
        val routee = akkaSystem.actorOf(
          Props(classOf[InventoryGetter], skuString, quantity))
        inventoryUpdater ! RegisterListener(routee)
        routee
      }
    inventoryGetters.put(
      sku.toString,
      akkaSystem.actorOf(RoundRobinGroup(routees.map(_.path.toStringWithoutAddress).toList).props()))
  }

  def index = Action.async ({
    request =>
      Future(Ok("Inventory service is up and running!"))
  })

  def getInventory(sku: String) = Action.async ({
    val startTime = System.currentTimeMillis()
    val f = (inventoryGetters.getOrElse(sku,akkaSystem.deadLetters) ? GetInventory()).mapTo[Result]
    f.onComplete({case _ =>
      statsDSender ! SendTimer("reactive.get.duration", System.currentTimeMillis - startTime)
      statsDSender ! IncrementCounter("reactive.get.count")
    })
    f
  })

  def updateInventory(sku: String, quantityChange: Int) = Action.async ({
    val startTime = System.currentTimeMillis()
    val f = (inventoryUpdaters.getOrElse(sku,akkaSystem.deadLetters) ? UpdateInventory(quantityChange)).mapTo[Result]
    f.onComplete({case _ =>
      statsDSender ! SendTimer("reactive.update.duration", System.currentTimeMillis - startTime)
      statsDSender ! IncrementCounter("reactive.update.count")
    })
    f
  })
}

object Application extends ApplicationLike(MongoRepo)
