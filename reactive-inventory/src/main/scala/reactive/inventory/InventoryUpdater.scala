package reactive.inventory

import akka.actor.{Actor, ActorLogging}
import scala.util.Failure
import reactive.inventory.StatsDSender.{IncrementCounter, SendTimer}

//Object that stores message classes for the InventoryUpdater
object InventoryUpdater {
  case class UpdateInventory(startTime: Long, quantity: Int, completer: InventoryResponse => Unit)
  case class InventoryUpdate(quantity: Int)
  case class InventoryResponse(action: String, sku: String, success: Boolean, quantity: Int, message: String)
}

class InventoryUpdater(sku: String, var quantity: Int, mongoRepo : MongoRepoLike) extends Actor
with ActorLogging
with EventSource {

  import InventoryUpdater._

  implicit val executor = context.dispatcher

  val statsDSender = context.actorSelection("/user/StatsDSender")

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
    case UpdateInventory(startTime, modQuantity, completer) =>
      var message: String = ""
      var success: Boolean = true
      modQuantity >= 0 || quantity + modQuantity >= 0 match {
        case true =>
          quantity += modQuantity
        case _ =>
          //Don't allow user to reserve more than we have on hand.
          message = s"Only $quantity left"
          success = false
      }
      completer(InventoryResponse("update", sku, success = success, modQuantity, message))
      sendEvent(InventoryUpdate(quantity))
      callSetInventory(sku, quantity + modQuantity)
      statsDSender ! SendTimer("reactive.update.duration", System.currentTimeMillis - startTime)
      statsDSender ! IncrementCounter("reactive.update.count")
  }

  def receive = eventSourceReceive orElse inventoryReceive
}
