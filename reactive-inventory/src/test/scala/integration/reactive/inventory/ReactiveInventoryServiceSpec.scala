package integration.reactive.inventory

import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import reactive.inventory.InventoryUpdater._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import reactive.inventory.Service
import scala.concurrent.duration._
import scala.language.postfixOps

class ReactiveInventoryServiceSpec extends WordSpecLike
with ScalatestRouteTest
with Matchers
with Service
with BeforeAndAfterAll {

  //we run into an issue with the first test not completing within the
  //default 1 second so we set an implicit timeout of 5
  implicit val routTestTimeout = RouteTestTimeout(5 second)

  override def beforeAll() {
    initializeMetrics()
    initializeManagers()
  }

  "ReactiveInventoryService" should {

    "allow purchase of inventory" in {
      Put("/reactive-inventory/1/-1") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val inventoryResponse: InventoryResponse = responseAs[InventoryResponse]
        inventoryResponse.sku == "1" shouldBe true
        inventoryResponse.quantity == -1 shouldBe true
        inventoryResponse.success shouldBe true
      }
    }

    "get inventory value" in {
      Get("/reactive-inventory/1") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        responseAs[InventoryResponse].sku == "1" shouldBe true
      }
    }

    "disallow purchase of more inventory than is available" in {
      var moreThanAvailableQuantity = 0
      Get("/reactive-inventory/1") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val inventoryResponse = responseAs[InventoryResponse]
        moreThanAvailableQuantity = inventoryResponse.quantity + 1
      }
      Put(s"/reactive-inventory/1/-$moreThanAvailableQuantity") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val inventoryResponse = responseAs[InventoryResponse]
        inventoryResponse.sku == "1" shouldBe true
        inventoryResponse.quantity == -moreThanAvailableQuantity shouldBe true
        inventoryResponse.success shouldBe false
      }
    }

    "purchase should decrement inventory" in {
      var availableQuantity = 0
      Get("/reactive-inventory/1") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val inventoryResponse = responseAs[InventoryResponse]
        availableQuantity = inventoryResponse.quantity
      }
      Put(s"/reactive-inventory/1/-1") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val inventoryResponse = responseAs[InventoryResponse]
        inventoryResponse.sku == "1" shouldBe true
        inventoryResponse.quantity == -1 shouldBe true
        inventoryResponse.success shouldBe true
      }
      Get("/reactive-inventory/1") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val inventoryResponse = responseAs[InventoryResponse]
        availableQuantity == (inventoryResponse.quantity + 1) shouldBe true
      }
      Thread.sleep(1000) //we need this to allow database connections to finish
    }
  }
}
