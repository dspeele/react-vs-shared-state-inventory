package unit.reactive.inventory

import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import akka.actor.{Props, ActorSystem}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import reactivemongo.bson.BSONDocument
import scala.concurrent.Await
import scala.concurrent.duration._
import reactive.inventory.InventoryManager._
import reactive.inventory.InventoryManager
import scala.concurrent.ExecutionContext.Implicits.global

class   InventoryManagerSpec extends
  TestKit(
      ActorSystem("InventoryManagerSpec"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with TestMongoRepo {

  override def afterAll() {
    stopMongo()
  }

  "InventoryManager" should {
    "set and get inventory for Sku when asked" in {
      val inventoryManager = TestActorRef(Props[InventoryManager with TestMongoRepo])
      val document = BSONDocument("sku" -> 1, "count" -> 5)
      Await.result(collection.save(document), 5 seconds)
      inventoryManager ! SetSkuAndQuantity("1", 5)
      expectMsg(InventoryResponseModel("set inventory", "1", true, 5, ""))
      inventoryManager ! GetInventory()
      expectMsg(InventoryResponseModel("get inventory", "1", true, 5, ""))
    }

    "remove inventory for Sku when purchased" in {
      val inventoryManager = TestActorRef(Props[InventoryManager with TestMongoRepo])
      val document = BSONDocument("sku" -> 1, "count" -> 5)
      Await.result(collection.save(document), 5 seconds)
      inventoryManager ! SetSkuAndQuantity("1", 5)
      expectMsg(InventoryResponseModel("set inventory", "1", true, 5, ""))
      inventoryManager ! UpdateInventory(-3)
      expectMsg(InventoryResponseModel("buy inventory", "1", true, 3, ""))
    }

    "return appropriate failure message for Sku when too many purchased" in {
      val inventoryManager = TestActorRef(Props[InventoryManager with TestMongoRepo])
      val document = BSONDocument("sku" -> 1, "count" -> 5)
      Await.result(collection.save(document), 5 seconds)
      inventoryManager ! SetSkuAndQuantity("1", 5)
      expectMsg(InventoryResponseModel("set inventory", "1", true, 5, ""))
      inventoryManager ! UpdateInventory(-6)
      expectMsg(InventoryResponseModel("buy inventory", "1", false, 6, "Only 5 left"))
    }

    "add inventory for Sku" in {
      val inventoryManager = TestActorRef(Props[InventoryManager with TestMongoRepo])
      val document = BSONDocument("sku" -> 1, "count" -> 5)
      Await.result(collection.save(document), 5 seconds)
      inventoryManager ! SetSkuAndQuantity("1", 5)
      expectMsg(InventoryResponseModel("set inventory", "1", true, 5, ""))
      inventoryManager ! UpdateInventory(2)
      expectMsg(InventoryResponseModel("add inventory", "1", true, 2, ""))
    }

    "removing inventory for Sku should decrement inventory" in {
      val inventoryManager = TestActorRef(Props[InventoryManager with TestMongoRepo])
      val document = BSONDocument("sku" -> 1, "count" -> 5)
      Await.result(collection.save(document), 5 seconds)
      inventoryManager ! SetSkuAndQuantity("1", 5)
      expectMsg(InventoryResponseModel("set inventory", "1", true, 5, ""))
      inventoryManager ! UpdateInventory(-3)
      expectMsg(InventoryResponseModel("buy inventory", "1", true, 3, ""))
      inventoryManager ! GetInventory()
      expectMsg(InventoryResponseModel("get inventory", "1", true, 2, ""))
    }
  }
}