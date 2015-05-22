package reactive.inventory

import akka.actor.{Props, ActorLogging, ActorRef, Actor}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import scala.util.Try
import play.api.libs.json.{JsValue, Json}
import java.util.Date
import reactive.inventory.StatsDSender.{IncrementCounter, SendTimer}
import reactive.inventory.InventoryManager.InventoryResponse

object Receptionist {
  trait Request

  case class GetRequest(sku: String, completer: InventoryResponse => Unit) extends Request
  case class PutRequest(sku: String, quantity: Int, completer: InventoryResponse => Unit) extends Request
}

class Receptionist extends Actor
  with ActorLogging
  with Router {

  import InventoryManager._
  import Receptionist._

  val statsDSender = context.actorOf(Props[StatsDSender])

  def receive = handleRequests(Map[Int, (InventoryResponse => Unit, Long)](), 0)

  def handleRequests(requests: Map[Int, (InventoryResponse => Unit, Long)], nextKey: Int): Receive = {
    case GetRequest (sku, completer) => {
      context.actorSelection("/user/" + sku) ! GetInventory(nextKey)
      context.become(handleRequests(requests + (nextKey -> (completer, System.currentTimeMillis)), nextKey + 1))
    }
    case PutRequest (sku, quantity, completer) => {
      context.actorSelection("/user/" + sku) ! UpdateInventory(nextKey, quantity)
      context.become(handleRequests(requests + (nextKey -> (completer, System.currentTimeMillis)), nextKey + 1))
    }
    case InventoryResponse(id, action, sku, success, quantity, message) => {
      requests.get(id) match {
        case Some((completer, startTime)) => {
          completer(InventoryResponse(id, action, sku, success, quantity, message))
          statsDSender ! SendTimer("reactive.duration", System.currentTimeMillis - startTime)
          statsDSender ! IncrementCounter("reactive.count")
          context.become(handleRequests(requests - id, nextKey))
        }
        case _ =>
      }
    }
  }
}