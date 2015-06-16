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

class ApplicationLike(mongoRepo: MongoRepoLike) extends Controller {

  val r = scala.util.Random

  val inventoryUpdaters = TrieMap[String, ActorRef]()
  val inventoryGetters = TrieMap[String, ActorRef]()

  val akkaSystem = ActorSystem("reactive-inventory")

  //Actor to send metrics
  val statsDSender = akkaSystem.actorOf(Props[StatsDSender], "StatsDSender")

  implicit val timeout = Timeout(20 millis)

  //initialize inventory randomly by creating an Inventory manager for each sku{
  for (sku <- 1 to 100) {
    val quantity = r.nextInt(10000) + 10000
    val skuString = sku.toString
    val inventoryUpdater = akkaSystem.actorOf(
      Props(classOf[InventoryUpdater], skuString, quantity, MongoRepo, statsDSender))
    inventoryUpdaters.put(sku.toString, inventoryUpdater)
    val routees: Seq[ActorRef] =
      for (routeeId <- 1 to 10) yield {
        val routee = akkaSystem.actorOf(
          Props(classOf[InventoryGetter], skuString, quantity, statsDSender))
        inventoryUpdater ! RegisterListener(routee)
        routee
      }
    inventoryGetters.put(
      sku.toString,
      akkaSystem.actorOf(RoundRobinGroup(routees.map(_.path.toStringWithoutAddress).toList).props()))



  }

  //make sure Actors are done being created
  Thread.sleep (10000)

  def index = Action.async ({
    request =>
      Future(Ok("inventory is up and running!"))
  })

  def getInventory(sku: String) = Action.async ({
    val future = inventoryGetters.getOrElse(sku,akkaSystem.deadLetters) ? GetInventory(System.currentTimeMillis())
    future.mapTo[Result]
  })

  def updateInventory(sku: String, quantityChange: Int) = Action.async ({
    (inventoryUpdaters.getOrElse(sku,akkaSystem.deadLetters) ? UpdateInventory(System.currentTimeMillis(), quantityChange)).mapTo[Result]
  })
}

object Application extends ApplicationLike(MongoRepo)
