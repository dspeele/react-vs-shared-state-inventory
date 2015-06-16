package models

import java.util.concurrent.locks.ReentrantLock

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
}
