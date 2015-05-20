package integration.reactive.inventory

import akka.util.Timeout

import scala.concurrent.Future
import org.scalatest.{Matchers, WordSpecLike}
import play.api.test.FakeRequest
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers._
import play.api.mvc.Result
import scala.concurrent.duration._
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import scala.util.Try
import reactive.inventory.InventoryManager.{InventoryResponseModel, InventoryResponse}


trait ReactiveInventoryHelpersSpecLike extends WordSpecLike
  with Matchers
  with InventoryResponse {

  def statusAndContentTypeCheck(result: Future[Result], expectedStatus: Int) = {
    status(result)(Timeout(15000 milliseconds)) should be(expectedStatus)
  }

  def generateFakeRequest(methodType: String, endpointUri: String): FakeRequest[AnyContentAsEmpty.type] = {
    FakeRequest(
      method = methodType,
      path = endpointUri
    )
  }

  def convertJsonToInventoryResponseModel(inventoryResponseJson: JsValue): Try[InventoryResponseModel] = Try {
    inventoryResponseJson.validate[InventoryResponseModel] match {
      case JsSuccess(inventoryResponse, _) => inventoryResponse
      case JsError(e) => throw new ClassCastException("Could not cast input into proper type")
    }
  }
}

