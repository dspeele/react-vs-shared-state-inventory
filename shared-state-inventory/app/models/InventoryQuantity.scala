package models

class InventoryQuantity(private var quantity: Int) {
  def getQuantity() = {
    quantity
  }

  def updateQuantity(quantityUpdate: Int): (Boolean, Int) = {
    synchronized {
      quantityUpdate >= 0 match {
        case true =>
          quantity += quantityUpdate
          (true, quantity)
        case _ =>
          quantity >= Math.abs(quantityUpdate) match {
            case true =>
              quantity += quantityUpdate
              (true, quantity)
            case _ =>
              (false, quantity)
          }
      }
    }
  }
}
