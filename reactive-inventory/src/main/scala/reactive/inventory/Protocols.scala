package reactive.inventory

import spray.json.DefaultJsonProtocol
import reactive.inventory.InventoryManager.InventoryResponse

//Json marshaller/unmarshaller
trait Protocols extends DefaultJsonProtocol {
  implicit val inventoryResponseFormat = jsonFormat6(InventoryResponse.apply)
}