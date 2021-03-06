package controllers

import akka.actor.Props
import play.api._
import play.api.mvc._

import mongo.{MongoRepo, MongoRepoLike}
import scala.concurrent.Future
import scala.collection.concurrent.TrieMap
import models.{InventoryResponseModel, InventoryResponse, InventoryQuantity}
import metrics.StatsDSender
import metrics.StatsDSender._
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.concurrent.Akka
import play.api.Play.current


class ApplicationLike(mongoRepo: MongoRepoLike) extends Controller
    with InventoryResponse {

  val akkaSystem = Akka.system

  val statsDSender = akkaSystem.actorOf(Props[StatsDSender])
  val inventory = TrieMap[String, InventoryQuantity]()

  val r = scala.util.Random

  //initialize inventory randomly by creating an Inventory manager for each sku and
  //sending it a message to assign inventory and sku
  for (sku <- 1 to 100) {
    val quantity = r.nextInt(10000) + 10000
    inventory.put(sku.toString, new InventoryQuantity(quantity))
    mongoRepo.setInventory(sku.toString, quantity)
  }

  def index = Action.async ({
    request =>
      Future(Ok("Inventory service is up and running!"))
  })

  def getInventory(sku: String) = Action.async ({
    request =>
      val startTime = System.currentTimeMillis
      val f = Future {
        Logger.debug("get inventory called")
        val skuInventoryOption = inventory.get(sku)
        var result = NotFound(Json.toJson(InventoryResponseModel("get", sku, success = false, 0, "sku not found")))
        skuInventoryOption match {
          case Some(skuInventory) => result = Ok(Json.toJson(InventoryResponseModel("get", sku, success = true, skuInventory.getQuantity(), "")))
          case _ =>
        }
        result
      }
      f.onComplete({case _ =>
        statsDSender ! SendTimer("shared-state.get.duration", System.currentTimeMillis - startTime)
        statsDSender ! IncrementCounter("shared-state.get.count")
      })
      f
  })

  def updateInventory(sku: String, quantityChange: Int) = Action.async ({
    request =>
      val startTime = System.currentTimeMillis
      val f = Future {
        Logger.debug("update inventory called")
        val skuInventoryOption = inventory.get(sku)
        var result = NotFound(Json.toJson(InventoryResponseModel("update", sku, success = false, 0, "sku not found")))
        skuInventoryOption match {
          case Some(skuInventory) =>
            val response = skuInventory.updateQuantity(quantityChange)
            result = Ok(
              Json.toJson(
                InventoryResponseModel(
                  "update", sku, success = response._1, quantityChange, if (!response._1) s"Only ${response._2} left" else ""
            )))
            response._1 match {
              case true => mongoRepo.setInventory(sku.toString, response._2)
              case _ =>
          }
          case _ =>
        }
        result
      }
      f.onComplete({case _ =>
        statsDSender ! SendTimer("shared-state.update.duration", System.currentTimeMillis - startTime)
        statsDSender ! IncrementCounter("shared-state.update.count")
      })
      f
  })
}

object Application extends ApplicationLike(MongoRepo)
