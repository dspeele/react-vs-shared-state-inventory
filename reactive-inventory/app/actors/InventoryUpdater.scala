package actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import scala.util.Failure
import metrics.StatsDSender.{IncrementCounter, SendTimer}
import models.{InventoryResponse, InventoryResponseModel}
import mongo.MongoRepoLike
import play.api.libs.json.Json
import play.api.mvc.Results.Ok


//Object that stores message classes for the InventoryUpdater
object InventoryUpdater {
  case class UpdateInventory(startTime: Long, quantity: Int)
  case class InventoryUpdate(quantity: Int)
}

class InventoryUpdater(sku: String, var quantity: Int, mongoRepo : MongoRepoLike, statsDSender: ActorRef) extends Actor
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

  //Set initial state of message handler
  def inventoryReceive: Receive = {
    //update inventory
    case UpdateInventory(startTime, modQuantity) =>
      var message: String = ""
      var success: Boolean = true
      modQuantity >= 0 || quantity + modQuantity >= 0 match {
        case true =>
          quantity += modQuantity
          sendEvent(InventoryUpdate(quantity))
          callSetInventory(sku, quantity)
        case _ =>
          //Don't allow user to reserve more than we have on hand.
          message = s"Only $quantity left"
          success = false
      }
      sender ! Ok(Json.toJson(InventoryResponseModel("update", sku, success = success, modQuantity, message)))
      statsDSender ! SendTimer("reactive.update.duration", System.currentTimeMillis - startTime)
      statsDSender ! IncrementCounter("reactive.update.count")
  }

  def receive = eventSourceReceive orElse inventoryReceive
}
