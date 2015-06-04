package models

import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock

class InventoryQuantity(private var quantity: Int) {

  private val inventoryLock = new ReentrantReadWriteLock()
  private val readLock = inventoryLock.readLock()
  private val writeLock = inventoryLock.writeLock()
  def getQuantity() = {
    readLock.lock()
    val q = quantity
    readLock.unlock()
    q
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
