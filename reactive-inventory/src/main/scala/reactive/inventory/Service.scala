package reactive.inventory

import akka.actor.{ActorRef, Props, ActorSystem}
import scala.concurrent.ExecutionContextExecutor
import akka.stream.FlowMaterializer
import akka.http.scaladsl.server.Directives._
import reactive.inventory.InventoryUpdater.{InventoryResponse, UpdateInventory}
import reactive.inventory.InventoryGetter.GetInventory
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import scala.collection.concurrent.TrieMap
import akka.util.Timeout
import scala.concurrent.duration._
import akka.routing.RoundRobinRouter
import reactive.inventory.EventSource.RegisterListener
import scala.language.postfixOps

//Create pool of Receptionist actors to handle requests
//Create an InventoryUpdater actor for each sku
//Create route object
trait Service extends Router with Protocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: FlowMaterializer
  implicit val timeout = Timeout(1 second)

  val r = scala.util.Random

  //initialize inventory randomly by creating an Inventory manager for each sku
  lazy val inventoryUpdaters = TrieMap[String, ActorRef]()
  lazy val inventoryGetters = TrieMap[String, ActorRef]()

  def initializeManagers() = {
    for (sku <- 1 to 25) {
      val quantity = r.nextInt(10000) + 10000
      val skuString = sku.toString
      val inventoryUpdater = system.actorOf(
        Props(classOf[InventoryUpdater], skuString, quantity, MongoRepo))
      inventoryUpdaters.put(sku.toString, inventoryUpdater)
      val inventoryGetterRoutees =
        for (routeeId <- 1 to 10) yield {
          val routee = system.actorOf(
            Props(classOf[InventoryGetter], skuString, quantity))
          inventoryUpdater ! RegisterListener(routee)
          routee
        }
      inventoryGetters.put(
        sku.toString,
        system.actorOf(
          Props.empty.withRouter(
            RoundRobinRouter(routees = inventoryGetterRoutees))))
    }
  }

  //Actor to send metrics
  def initializeMetrics() = {
    system.actorOf(Props[StatsDSender], "StatsDSender")
  }

  val routes =
    get {
      path("reactive-inventory" / Segment) {
        sku =>
          //Send a callback to complete the response for this request to a Receptionist
          completeWith[InventoryResponse](implicitly[ToResponseMarshaller[InventoryResponse]]) {
            completer: (InventoryResponse => Unit) =>
              inventoryGetters.getOrElse(sku,system.deadLetters) ! GetInventory(System.currentTimeMillis(), completer)
          }
      }
    } ~
      put {
        path("reactive-inventory" / Segment / Segment) {
          (sku, quantity) =>
            //Send a callback to complete the response for this request to a Receptionist
            completeWith[InventoryResponse](implicitly[ToResponseMarshaller[InventoryResponse]]) {
              completer: (InventoryResponse => Unit) =>
                inventoryUpdaters.getOrElse(sku,system.deadLetters) ! UpdateInventory(System.currentTimeMillis(), quantity.toInt, completer)
            }
        }
      } ~ {
      complete {
        InventoryResponse("", "", success = false, 0, "404- Route unknown")
      }
    }
}
