package reactive.inventory

import org.scalatest.{Matchers, WordSpecLike}
import reactive.inventory.InventoryManager._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

class ReactiveInventoryServiceSpec extends WordSpecLike
with ScalatestRouteTest
with Matchers
with Service {

  "ReactiveInventoryService" should {
    "get inventory value" in {
      Get("/inventory/1") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        responseAs[InventoryResponse].sku == "1" shouldBe true
      }
    }

    "allow purchase of inventory" in {
      Put("/inventory/1/-1") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val inventoryResponse: InventoryResponse = responseAs[InventoryResponse]
        inventoryResponse.sku == "1" shouldBe true
        inventoryResponse.quantity == 1 shouldBe true
        inventoryResponse.success shouldBe true
      }
    }

    "disallow purchase of more inventory than is available" in {
      var moreThanAvailableQuantity = 0
      Get("/inventory/1") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val inventoryResponse = responseAs[InventoryResponse]
        moreThanAvailableQuantity = inventoryResponse.quantity + 1
      }
      Put(s"/inventory/1/-$moreThanAvailableQuantity") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val inventoryResponse = responseAs[InventoryResponse]
        inventoryResponse.sku == "1" shouldBe true
        inventoryResponse.quantity == moreThanAvailableQuantity shouldBe true
        inventoryResponse.success shouldBe false
      }
    }

    "purchase should decrement inventory" in {
      var availableQuantity = 0
      Get("/inventory/1") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val inventoryResponse = responseAs[InventoryResponse]
        availableQuantity = inventoryResponse.quantity
      }
      Put(s"/inventory/1/-1") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val inventoryResponse = responseAs[InventoryResponse]
        inventoryResponse.sku == "1" shouldBe true
        inventoryResponse.quantity == 1 shouldBe true
        inventoryResponse.success shouldBe true
      }
      Get("/inventory/1") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val inventoryResponse = responseAs[InventoryResponse]
        availableQuantity == (inventoryResponse.quantity + 1) shouldBe true
      }
      Thread.sleep(1000) //we need this to allow database connections to finish
    }
  }
}
