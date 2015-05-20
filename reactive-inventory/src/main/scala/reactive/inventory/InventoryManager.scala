package reactive.inventory

import akka.actor.{Actor, ActorLogging}
import scala.async.Async._
import reactivemongo.core.commands.LastError
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.util.Failure

//object that stores message classes for the InventoryManager
object InventoryManager {

  case class GetInventory(id: Int)

  case class UpdateInventory(id: Int, quantity: Int)

  case class SetSkuAndQuantity(sku: String, quantity: Int)

  case class InventoryResponseModel(id: Int, action: String, sku: String, success: Boolean, quantity: Int, message: String)

  trait InventoryResponse {

    implicit val readInventoryResponseJsonObject: Reads[InventoryResponseModel] = (
      (__ \ "id").read[Int] and
      (__ \ "action").read[String] and
        (__ \ "sku").read[String] and
        (__ \ "success").read[Boolean] and
        (__ \ "quantity").read[Int] and
        (__ \ "message").read[String]
      )(InventoryResponseModel)

    implicit val writeInventoryResponseModel: Writes[InventoryResponseModel] = (
      (__ \ "id").write[Int] and
      (__ \ "action").write[String] and
        (__ \ "sku").write[String] and
        (__ \ "success").write[Boolean] and
        (__ \ "quantity").write[Int] and
        (__ \ "message").write[String]
      )(unlift(InventoryResponseModel.unapply))
  }

  object InventoryResponse extends InventoryResponse {
  }
}

class InventoryManager extends Actor
with ActorLogging
with MongoRepo {
  import InventoryManager._

  implicit val executor = context.dispatcher

  override def preRestart(reason: Throwable,
                          message: Option[Any]) = {
    // The default behaviour was to stop the children
    // here but we don't want to do that
    // We still want to postStop() however.
    postStop()
  }

  def callSetInventory (sku: String, quantity: Int) = {
    setInventory(sku, quantity).onComplete(_ match {
      case Failure(e) => throw e
      case _ =>
    })
  }

  def inventory (sku: String, quantity: Int): Receive = {
    case SetSkuAndQuantity(newSku, newQuantity) => {
      log.debug("Set quantity = {} for sku = {}", newSku, newQuantity)
      //essentially asynchronous tail recursion! Stateful without var
      context.become(inventory(newSku, newQuantity))
      sender ! InventoryResponseModel(0, "set inventory", newSku, true, newQuantity, "")
      setInventory(newSku, newQuantity)
    }
    case GetInventory(id) => {
      log.debug("Get quantity ({}) for sku = {}", sku, quantity)
      sender ! InventoryResponseModel(id, "get inventory", sku, true, quantity, "")
    }
    case UpdateInventory(id, modQuantity) => {
      log.debug("Update quantity by {} for sku = {}", sku, modQuantity)
      modQuantity >= 0 match {
        case true => {
          context.become(inventory(sku, quantity + modQuantity))
          sender ! InventoryResponseModel(id, "add inventory", sku, true, modQuantity, "")

          callSetInventory(sku, quantity + modQuantity)
        }
        case _ => {
          val absModQuantity = modQuantity * -1
          quantity >= absModQuantity match {
            case true => {
              context.become(inventory(sku, quantity + modQuantity))
              sender ! InventoryResponseModel(id, "buy inventory", sku, true, absModQuantity, "")
              callSetInventory(sku, quantity + modQuantity)
            }
            case _ => sender ! InventoryResponseModel(id, "buy inventory", sku, false, absModQuantity, s"Only $quantity left")
          }
        }
      }
    }
  }

  def receive = inventory("",0)
}
