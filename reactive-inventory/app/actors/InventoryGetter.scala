package actors

import akka.actor.{Actor, ActorLogging}
import models.{InventoryResponse, InventoryResponseModel}
import actors.InventoryUpdater.UpdateInventory
import play.api.mvc.Results.Ok
import play.api.libs.json.Json

//Object that stores message classes for the InventoryGetter
object InventoryGetter {
  case class GetInventory()
}

class InventoryGetter(sku: String, var quantity: Int) extends Actor
    with ActorLogging
    with InventoryResponse {
  this: EventSource =>

  import InventoryGetter._

  implicit val executor = context.dispatcher

  //Set initial state of message handler
  def receive = {
    //update the current inventory for this sku
    case UpdateInventory(newQuantity) =>
      quantity = newQuantity
    //Retrieve the current inventory for this sku
    case GetInventory() =>
      sender ! Ok(Json.toJson(InventoryResponseModel("get", sku, success = true, quantity, "")))
  }
}
