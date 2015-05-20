package reactive.inventory

import scala.concurrent.{Promise, Future}
import reactivemongo.core.commands.LastError
import reactivemongo.bson.BSONDocument
import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.MongoDriver
import com.typesafe.config.ConfigFactory

trait MongoRepo {

  val driver = new MongoDriver

  implicit val collection: BSONCollection = {
    val config = ConfigFactory.load()
    val connection = driver.connection(List(config.getString("mongo.interface") + ":" + config.getInt("mongo.port")))
    val db = connection(config.getString("mongo.database"))
    db.collection(config.getString("mongo.collection"))
  }

  def setInventory(sku: String, count: Int)(implicit collection: BSONCollection): Future[LastError] = {
    val document = BSONDocument("sku" -> sku, "count" -> count)

    // which returns Future[LastError]
    collection.save(document)
  }

  def findAllInventory()(implicit collection: BSONCollection): Future[List[BSONDocument]] = {
    val query = BSONDocument()

    // which returns Future[List[BSONDocument]]
    collection.find(query)
      .cursor[BSONDocument]
      .collect[List]()
  }

  def findInventoryBySku(sku: String)(implicit collection: BSONCollection): Future[Option[BSONDocument]] = {
    val query = BSONDocument("sku" -> sku)

    // which returns Future[Option[BSONDocument]]
    collection.find(query)
      .one
  }
}