package models

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._

case class InventoryResponseModel (
  action: String,
  sku: String,
  success: Boolean,
  quantity: Int,
  message: String
)

trait InventoryResponse {

  implicit val readMarketingSignUpJsonObject: Reads[InventoryResponseModel] = (
    (__ \ "action").read[String] and
    (__ \ "sku").read[String] and
    (__ \ "success").read[Boolean] and
    (__ \ "quantity").read[Int] and
    (__ \ "message").read[String]
  )(InventoryResponseModel)

  implicit val writeMarketingSignUpModel: Writes[InventoryResponseModel] = (
    (__ \ "action").write[String] and
    (__ \ "sku").write[String] and
    (__ \ "success").write[Boolean] and
    (__ \ "quantity").write[Int] and
    (__ \ "message").write[String]
  )(unlift(InventoryResponseModel.unapply))
}

object InventoryResponse extends InventoryResponse