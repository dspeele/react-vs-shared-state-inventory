package reactive.inventory

import akka.actor.{ActorRef, Props, ActorSystem}
import scala.concurrent.ExecutionContextExecutor
import akka.stream.FlowMaterializer
import akka.http.scaladsl.server.Directives._
import reactive.inventory.InventoryManager.InventoryResponse
import akka.routing.RoundRobinPool
import reactive.inventory.InventoryManager.SetSkuAndQuantity
import reactive.inventory.Receptionist.{PutRequest, GetRequest}
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

//Create pool of Receptionist actors to handle requests
//Create an InventoryManager actor for each sku
//Create route object
trait Service extends Router with Protocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: FlowMaterializer

  val r = scala.util.Random

  //initialize inventory randomly by creating an Inventory manager for each sku
  lazy val inventoryManagers: Map[String, ActorRef] =
    (for (sku <- 1 to 100) yield
      sku.toString -> system.actorOf(
        Props(InventoryManager.getClass,sku.toString, r.nextInt(1000) + 10, MongoRepo), sku.toString)).toMap

  //Actor to send metrics
  def initializeMetrics() = {
    system.actorOf(Props[StatsDSender], "StatsDSender")
  }

  //create pool of 100 Receptionists to handle incoming requests with SupervisorStrategy from mixed in Router trait
  lazy val receptionistRouter: ActorRef = system.actorOf(RoundRobinPool(750, supervisorStrategy = oneForOneSupervisorStrategy).props(
    routeeProps = Props(Receptionist.getClass, inventoryManagers)))

  val routes =
    get {
      path("reactive-inventory" / Segment) {
        sku =>
          //Send a callback to complete the response for this request to a Receptionist
          completeWith[InventoryResponse](implicitly[ToResponseMarshaller[InventoryResponse]]) {
            completer: (InventoryResponse => Unit) =>
              receptionistRouter ! GetRequest(sku, completer)
          }
      }
    } ~
      put {
        path("reactive-inventory" / Segment / Segment) {
          (sku, quantity) =>
            //Send a callback to complete the response for this request to a Receptionist
            completeWith[InventoryResponse](implicitly[ToResponseMarshaller[InventoryResponse]]) {
              completer: (InventoryResponse => Unit) =>
                receptionistRouter ! PutRequest(sku, quantity.toInt, completer)
            }
        }
      } ~ {
      complete {
        InventoryResponse(0, "", "", success = false, 0, "404- Route unknown")
      }
    }
}
