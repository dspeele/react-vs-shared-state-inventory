package actors

import akka.actor.{ActorRef, Actor, ActorLogging}
import metrics.StatsDSender.{IncrementCounter, SendTimer}
import models.{InventoryResponse, InventoryResponseModel}
import actors.InventoryUpdater.InventoryUpdate
import scala.concurrent.Promise
import play.api.libs.json.{Json, JsValue}

//Object that stores message classes for the InventoryGetter
object InventoryGetter {
  case class GetInventory(startTime: Long, completer: Promise[JsValue])
}

class InventoryGetter(sku: String, var quantity: Int, statsDSender: ActorRef) extends Actor
    with ActorLogging
    with InventoryResponse {
  this: EventSource =>

  import InventoryGetter._

  implicit val executor = context.dispatcher

  //Set initial state of message handler
  def receive = {
    //update the current inventory for this sku
    case InventoryUpdate(newQuantity) =>
      quantity = newQuantity
    //Retrieve the current inventory for this sku
    case GetInventory(startTime: Long, completer) =>
      completer.success(Json.toJson(InventoryResponseModel("get", sku, success = true, quantity, "")))
      statsDSender ! SendTimer("reactive.get.duration", System.currentTimeMillis - startTime)
      statsDSender ! IncrementCounter("reactive.get.count")
  }
}
