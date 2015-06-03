package models

import java.util.concurrent.locks.{Lock, ReentrantLock}

class InventoryQuantity(private var quantity: Int) {

  private val inventoryLock: Lock = new ReentrantLock()
  def getQuantity() = {
    inventoryLock.lock()
    val q = quantity
    inventoryLock.unlock()
    q
  }

  def updateQuantity(quantityUpdate: Int): (Boolean, Int) = {
    var returnValue: (Boolean, Int) = (false, quantity)
    inventoryLock.lock()
      quantityUpdate >= 0 match {
        case true =>
          quantity += quantityUpdate
          returnValue = (true, quantity)
        case _ =>
          quantity >= Math.abs(quantityUpdate) match {
            case true =>
              quantity += quantityUpdate
              returnValue = (true, quantity)
            case _ =>
          }
      }
    inventoryLock.unlock()
    returnValue
  }
}
