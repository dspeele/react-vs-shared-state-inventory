package mongo

import scala.concurrent.Future
import reactivemongo.core.commands.LastError
import reactivemongo.bson.BSONDocument
import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.MongoDriver
import com.typesafe.config.ConfigFactory

//Our Mongo repo is completely non-blocking thanks to ReactiveMongo
trait MongoRepoLike {

  val driver = new MongoDriver

  val collection: BSONCollection = {
    val config = ConfigFactory.load()
    val connection = driver.connection(List(config.getString("mongo.server") + ":" + config.getInt("mongo.port")))
    val db = connection(config.getString("mongo.database"))
    db.collection(config.getString("mongo.collection"))
  }

  def setInventory(sku: String, count: Int): Future[LastError] = {
    val query = BSONDocument("sku" -> sku)
    val document = BSONDocument("sku" -> sku, "count" -> count)

    //Returns Future[LastError]
    collection.update(query, document, upsert = true)
  }

  def findAllInventory(): Future[List[BSONDocument]] = {
    val query = BSONDocument()

    //Returns Future[List[BSONDocument]]
    collection.find(query)
      .cursor[BSONDocument]
      .collect[List]()
  }

  def findInventoryBySku(sku: String): Future[Option[BSONDocument]] = {
    val query = BSONDocument("sku" -> sku)

    //Returns Future[Option[BSONDocument]]
    collection.find(query)
      .one
  }
}

object MongoRepo extends MongoRepoLike