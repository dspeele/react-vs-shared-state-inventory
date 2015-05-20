package reactive.inventory

import org.scalatest.{Matchers, WordSpecLike}
import play.api.test.Helpers._
import play.api.http.Status
import integration.reactive.inventory.ReactiveInventoryHelpersSpecLike
import scala.util.Success
import scala.util.Failure
import akka.http.scaladsl.Http


class ReactiveInventoryServiceSpec extends WordSpecLike
with Matchers
with ReactiveInventoryHelpersSpecLike {

  val reactiveInventoryService = ReactiveInventoryService

  "TestHttp" should {
    "get response from http" in {
    }
  }

  "ReactiveInventoryService" should {
    "get inventory value" in {
      val fakeRequest = generateFakeRequest(GET, "/inventory/1")
      val result = route(fakeRequest).get
      statusAndContentTypeCheck(result, Status.OK)
      convertJsonToInventoryResponseModel(contentAsJson(result)) match {
        case Success(inventoryResponse) => {
          inventoryResponse.sku == "1" shouldBe true
        }
        case Failure(e) => throw e
      }
    }

    "allow purchase of inventory" in {
      val fakeRequest = generateFakeRequest(PUT, "/inventory/1/1")
      val result = route(fakeRequest).get
      statusAndContentTypeCheck(result, Status.OK)
      convertJsonToInventoryResponseModel(contentAsJson(result)) match {
        case Success(inventoryResponse) => {
          inventoryResponse.sku == "1" shouldBe true
          inventoryResponse.quantity == 1 shouldBe true
          inventoryResponse.success shouldBe true
        }
        case Failure(e) => throw e
      }
    }

    "disallow purchase of more inventory than is available" in {
      val fakeRequestGet = generateFakeRequest(GET, "/inventory/1")
      val resultGet = route(fakeRequestGet).get
      statusAndContentTypeCheck(resultGet, Status.OK)
      val responseGet = contentAsJson(resultGet)
      val availableQuantity = (responseGet \ "quantity").asInstanceOf[Int] + 1
      val fakeRequestPut = generateFakeRequest(PUT, s"/inventory/1/$availableQuantity")
      val resultPut = route(fakeRequestPut).get
      statusAndContentTypeCheck(resultPut, Status.OK)
      convertJsonToInventoryResponseModel(contentAsJson(resultPut)) match {
        case Success(inventoryResponse) => {
          inventoryResponse.sku == "1" shouldBe true
          inventoryResponse.quantity == availableQuantity shouldBe true
          inventoryResponse.success shouldBe false
        }
        case Failure(e) => throw e
      }
    }

    "purchase should decrement inventory" in {
      val fakeRequestGet = generateFakeRequest(GET, "/inventory/1")
      val resultGet = route(fakeRequestGet).get
      statusAndContentTypeCheck(resultGet, Status.OK)
      val responseGet = contentAsJson(resultGet)
      val availableQuantity = (responseGet \ "quantity").asInstanceOf[Int]
      val fakeRequestPut = generateFakeRequest(PUT, s"/inventory/1/1")
      val resultPut = route(fakeRequestPut).get
      statusAndContentTypeCheck(resultPut, Status.OK)
      val fakeRequestGetNew = generateFakeRequest(GET, "/inventory/1")
      val resultGetNew = route(fakeRequestGetNew).get
      statusAndContentTypeCheck(resultGetNew, Status.OK)
      convertJsonToInventoryResponseModel(contentAsJson(resultGetNew)) match {
        case Success(inventoryResponse) => {
          inventoryResponse.quantity == availableQuantity-1 shouldBe true
        }
        case Failure(e) => throw e
      }
    }
  }
}
