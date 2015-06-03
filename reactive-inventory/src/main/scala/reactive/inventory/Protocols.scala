package reactive.inventory

import spray.json.DefaultJsonProtocol
import reactive.inventory.InventoryUpdater.InventoryResponse

//Json marshaller/unmarshaller
trait Protocols extends DefaultJsonProtocol {
  implicit val inventoryResponseFormat = jsonFormat5(InventoryResponse.apply)
}