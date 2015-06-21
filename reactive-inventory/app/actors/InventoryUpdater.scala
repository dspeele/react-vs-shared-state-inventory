package actors

import akka.actor.{Actor, ActorLogging}
import scala.util.Failure
import models.{InventoryResponse, InventoryResponseModel}
import mongo.MongoRepoLike
import play.api.libs.json.Json
import play.api.mvc.Results.Ok


//Object that stores message classes for the InventoryUpdater
object InventoryUpdater {
  case class UpdateInventory(quantity: Int)
}

class InventoryUpdater(sku: String, var quantity: Int, mongoRepo : MongoRepoLike) extends Actor
    with EventSource
    with ActorLogging
    with InventoryResponse{

  import InventoryUpdater._

  implicit val executor = context.dispatcher

  //Persist inventory to Mongo
  def callSetInventory (sku: String, quantity: Int) = {
    mongoRepo.setInventory(sku, quantity).onComplete{
      case Failure(e) =>
        log.error("Mongo error: {}", e.getMessage)
      case _ =>
    }
  }

  callSetInventory (sku, quantity)

  def inventoryReceive: Receive = {
    //update inventory
    case UpdateInventory(modQuantity) =>
      var message: String = ""
      var success: Boolean = true
      modQuantity >= 0 || quantity + modQuantity >= 0 match {
        case true =>
          quantity += modQuantity
          sendEvent(UpdateInventory(quantity))
          callSetInventory(sku, quantity)
        case _ =>
          //Don't allow user to reserve more than we have on hand.
          message = s"Only $quantity left"
          success = false
      }
      sender ! Ok(Json.toJson(InventoryResponseModel("update", sku, success = success, modQuantity, message)))
  }

  def receive = eventSourceReceive orElse inventoryReceive
}
