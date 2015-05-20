package reactive.inventory

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Source
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Try
import InventoryManager._
import Receptionist._
import reactive.inventory.StatsDSender.SendTimer
import akka.http.scaladsl.model.HttpResponse
import akka.routing.RoundRobinPool
import akka.http.scaladsl.Http.IncomingConnection
import reactive.inventory.StatsDSender.IncrementCounter
import akka.http.scaladsl.Http.ServerBinding
import play.api.libs.json.Json
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshalling.ToResponseMarshaller

object ReactiveInventoryService extends App with Router {
  implicit val system = ActorSystem("ReactiveInventory", ConfigFactory.load().getConfig("ReactiveInventory"))
  implicit val executor = system.dispatcher
  implicit val materializer = ActorFlowMaterializer()
  implicit val timeout = Timeout(5 seconds)

  val config = ConfigFactory.load()

  val statsDSender = system.actorOf(Props[StatsDSender])

  val r = scala.util.Random

  for (sku <- 1 until 100) {
    val inventoryManager = system.actorOf(Props[InventoryManager with MongoRepo],sku.toString)
    inventoryManager ! SetSkuAndQuantity(sku.toString, r.nextInt(1000)+10)
  }

  //create pool of 100 Receptionists to handle incoming requests with SupervisorStrategy from mixed in Router trait
  val receptionistRouter: ActorRef = system.actorOf(RoundRobinPool(100, supervisorStrategy = oneForOneSupervisorStrategy).props(
    routeeProps = Props[Receptionist]))

  val routes =
    path("inventory" / Segment) { sku =>
      get {
        //nonblocking - spawns thread that waits for completer callback to be called by receptionist actor
        completeWith[String](implicitly[ToResponseMarshaller[String]]) { completer: (String => Unit) =>
          receptionistRouter ! GetRequest(sku, completer)
        }
      } ~
      put {
        path(Segment) { quantity =>
          completeWith[String] (implicitly[ToResponseMarshaller[String]]) { completer: (String => Unit) =>
            receptionistRouter ! PutRequest(sku, quantity.asInstanceOf[Int], completer)
          }
        }
      }
    } ~
    {
      complete {
        Json.stringify(Json.toJson(InventoryResponseModel(0, "", "", false, 0, "404- Route unknown")))
      }
    }

  //Create server binding listening on specified interface and port and bind/handle via route object
  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
