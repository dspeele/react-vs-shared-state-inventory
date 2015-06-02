package reactive.inventory

import akka.actor.{Actor, ActorLogging}
import scala.util.Failure

//Object that stores message classes for the InventoryManager
object InventoryManager {
  case class GetInventory(id: Int)
  case class UpdateInventory(id: Int, quantity: Int)
  case class InventoryResponse(id: Int, action: String, sku: String, success: Boolean, quantity: Int, message: String)
}

class InventoryManager(sku: String, quantity: Int, mongoRepo : MongoRepoLike) extends Actor
with ActorLogging {

  import InventoryManager._

  implicit val executor = context.dispatcher

  //Persist inventory to Mongo
  def callSetInventory (sku: String, quantity: Int) = {
    mongoRepo.setInventory(sku, quantity).onComplete{
      case Failure(e) =>
        log.error("Mongo error: {}", e.getMessage)
        throw e
      case _ =>
    }
  }

  //Set initial state of message handler
  def receive = inventory(sku,quantity)
  callSetInventory (sku, quantity)

  def inventory (sku: String, quantity: Int): Receive = {
    //Retrieve the current inventory for this sku
    case GetInventory(id) =>
      sender ! InventoryResponse(id, "get", sku, success = true, quantity, "")
    //Update the inventory for this sku, persist to db, use tail recursive call to save state inside message handler
    case UpdateInventory(id, modQuantity) =>
      modQuantity >= 0 match {
        case true =>
          context.become(inventory(sku, quantity + modQuantity))
          sender ! InventoryResponse(id, "update", sku, success = true, modQuantity, "")
          callSetInventory(sku, quantity + modQuantity)
        case _ =>
          val absModQuantity = modQuantity * -1
          quantity >= absModQuantity match {
            case true =>
              context.become(inventory(sku, quantity + modQuantity))
              sender ! InventoryResponse(id, "update", sku, success = true, modQuantity, "")
              callSetInventory(sku, quantity + modQuantity)
            //Don't allow user to reserve more than we have on hand.
            case _ => sender ! InventoryResponse(id, "update", sku, success = false, modQuantity, s"Only $quantity left")
          }
      }
  }
}
