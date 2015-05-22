package reactive.inventory

import akka.actor.{Actor, ActorLogging}
import scala.util.Failure


//object that stores message classes for the InventoryManager
object InventoryManager {
  case class GetInventory(id: Int)
  case class UpdateInventory(id: Int, quantity: Int)
  case class SetSkuAndQuantity(sku: String, quantity: Int)
  case class InventoryResponse(id: Int, action: String, sku: String, success: Boolean, quantity: Int, message: String)
}

class InventoryManager extends Actor
with ActorLogging {

  import InventoryManager._

  var mongoRepo: MongoRepoLike = MongoRepo

  implicit val executor = context.dispatcher

  def callSetInventory (sku: String, quantity: Int) = {
    mongoRepo.setInventory(sku, quantity).onComplete(_ match {
      case Failure(e) => {
        log.debug("Mongo error: {}", e.getMessage)
        throw e
      }
      case _ =>
    })
  }

  def receive = inventory("",0)

  def inventory (sku: String, quantity: Int): Receive = {
    case newMongoRepo: MongoRepoLike => {
      mongoRepo = newMongoRepo
    }
    case SetSkuAndQuantity(newSku, newQuantity) => {
      log.debug("Set quantity = {} for sku = {}", newSku, newQuantity)
      //essentially asynchronous tail recursion! Stateful without var
      context.become(inventory(newSku, newQuantity))
      mongoRepo.setInventory(newSku, newQuantity)
    }
    case GetInventory(id) => {
      log.debug("Get quantity ({}) for sku = {}", sku, quantity)
      sender ! InventoryResponse(id, "get inventory", sku, true, quantity, "")
    }
    case UpdateInventory(id, modQuantity) => {
      log.debug("Update quantity by {} for sku = {}", sku, modQuantity)
      modQuantity >= 0 match {
        case true => {
          context.become(inventory(sku, quantity + modQuantity))
          sender ! InventoryResponse(id, "add inventory", sku, true, modQuantity, "")
          callSetInventory(sku, quantity + modQuantity)
        }
        case _ => {
          val absModQuantity = modQuantity * -1
          quantity >= absModQuantity match {
            case true => {
              context.become(inventory(sku, quantity + modQuantity))
              sender ! InventoryResponse(id, "buy inventory", sku, true, absModQuantity, "")
              callSetInventory(sku, quantity + modQuantity)
            }
            case _ => sender ! InventoryResponse(id, "buy inventory", sku, false, absModQuantity, s"Only $quantity left")
          }
        }
      }
    }
  }
}
