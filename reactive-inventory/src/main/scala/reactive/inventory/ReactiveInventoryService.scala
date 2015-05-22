package reactive.inventory

import akka.actor._
import akka.http.scaladsl.Http
import akka.stream.{FlowMaterializer, ActorFlowMaterializer}
import com.typesafe.config.ConfigFactory
import InventoryManager._
import Receptionist._
import akka.routing.RoundRobinPool
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol
import scala.concurrent.ExecutionContextExecutor
import akka.http.scaladsl.server.{Directives, MissingCookieRejection}
import akka.http.scaladsl.model.HttpResponse


trait Protocols extends DefaultJsonProtocol {
  implicit val inventoryResponseFormat = jsonFormat6(InventoryResponse.apply)
}

trait Service extends Router with Protocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: FlowMaterializer

  val config = ConfigFactory.load()

  val r = scala.util.Random

  for (sku <- 1 until 100) {
    val inventoryManager = system.actorOf(Props[InventoryManager], sku.toString)
    inventoryManager ! SetSkuAndQuantity(sku.toString, r.nextInt(1000) + 10)
  }

  //create pool of 100 Receptionists to handle incoming requests with SupervisorStrategy from mixed in Router trait
  val receptionistRouter: ActorRef = system.actorOf(RoundRobinPool(100, supervisorStrategy = oneForOneSupervisorStrategy).props(
    routeeProps = Props[Receptionist]))

  val routes =
    get {
      path("inventory" / Segment) {
        sku =>
          completeWith[InventoryResponse](implicitly[ToResponseMarshaller[InventoryResponse]]) {
            completer: (InventoryResponse => Unit) =>
              receptionistRouter ! GetRequest(sku, completer)
          }
      }
    } ~
    put {
      path("inventory" / Segment / Segment) {
        (sku, quantity) =>
          completeWith[InventoryResponse](implicitly[ToResponseMarshaller[InventoryResponse]]) {
            completer: (InventoryResponse => Unit) =>
              receptionistRouter ! PutRequest(sku, quantity.toInt, completer)
          }
      }
    } ~ {
      complete {
        InventoryResponse(0, "", "", false, 0, "404- Route unknown")
      }
    }
}

object ReactiveInventoryService extends App
with Service {
  override implicit val system = ActorSystem("ReactiveInventory", ConfigFactory.load().getConfig("ReactiveInventory"))
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorFlowMaterializer()

  //Create server binding listening on specified interface and port and bind/handle via route object
  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
