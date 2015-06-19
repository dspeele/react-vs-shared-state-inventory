package models

/*import java.util.concurrent.locks.ReentrantLock

class InventoryQuantity(private var quantity: Int) {

  private val writeLock = new ReentrantLock()

  def getQuantity() = {
    quantity
  }

  def updateQuantity(quantityUpdate: Int): (Boolean, Int) = {
    var returnValue: (Boolean, Int) = (false, quantity)
    writeLock.lock()
      quantityUpdate >= 0 || quantity + quantityUpdate >= 0 match {
        case true =>
          quantity += quantityUpdate
          returnValue = (true, quantity)
        case _ =>
      }
    writeLock.unlock()
    returnValue
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
