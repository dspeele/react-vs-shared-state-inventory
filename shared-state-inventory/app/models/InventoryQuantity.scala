package models

/*import java.util.concurrent.atomic.AtomicInteger

class InventoryQuantity(private var initialQuantity: Int) {
  var quantity: AtomicInteger = new AtomicInteger(initialQuantity)

  def getQuantity() = {
    quantity.get()
  }

  def foreverWhile[A](body: => A):Nothing = {
    body
    foreverWhile(body)
  }

  def updateQuantity(quantityUpdate: Int): (Boolean, Int) = {
    var currentQuantity = quantity.get()
    foreverWhile {
      quantityUpdate >= 0 || currentQuantity + quantityUpdate >= 0 match {
        case true =>
          quantity.compareAndSet(currentQuantity, quantityUpdate) match {
            case true =>
              (true, quantity)
            case _ =>
          }
        case _ =>
          (false, quantity)
      }
      currentQuantity = quantity.get()
    }
  }
}*/

import java.util.concurrent.atomic.AtomicInteger

class InventoryQuantity(private var initialQuantity: Int) {
  var quantity: AtomicInteger = new AtomicInteger(initialQuantity)

  def getQuantity() = {
    quantity.get()
  }

  def updateQuantity(quantityUpdate: Int): (Boolean, Int) = {
    var returnValue = (false, getQuantity)
    var notDone = true
    do {
      val currentQuantity = getQuantity
      val newQuantity = currentQuantity + quantityUpdate
      quantityUpdate >= 0 || newQuantity >= 0 match {
        case true =>
          quantity.compareAndSet(currentQuantity, newQuantity) match {
            case true =>
              returnValue = (true, getQuantity)
              notDone = false
            case _ =>
          }
        case _ =>
          notDone = false
      }
    } while(notDone);
    returnValue
  }
}
