package reactive.inventory

import akka.actor.{Actor, ActorLogging}
import reactive.inventory.StatsDSender.{IncrementCounter, SendTimer}
import reactive.inventory.InventoryUpdater.{InventoryUpdate, InventoryResponse}

//Object that stores message classes for the InventoryGetter
object InventoryGetter {
  case class GetInventory(startTime: Long, completer: InventoryResponse => Unit)
}

class InventoryGetter(sku: String, var quantity: Int) extends Actor
with ActorLogging {
  this: EventSource =>

  import InventoryGetter._

  implicit val executor = context.dispatcher

  val statsDSender = context.actorSelection("/user/StatsDSender")

  //Set initial state of message handler
  def receive = {
    //update the current inventory for this sku
    case InventoryUpdate(newQuantity) =>
      quantity = newQuantity
    //Retrieve the current inventory for this sku
    case GetInventory(startTime: Long, completer) =>
      completer(InventoryResponse("get", sku, success = true, quantity, ""))
      statsDSender ! SendTimer("reactive.get.duration", System.currentTimeMillis - startTime)
      statsDSender ! IncrementCounter("reactive.get.count")
  }
}
