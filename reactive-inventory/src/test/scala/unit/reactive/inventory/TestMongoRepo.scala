package unit.reactive.inventory

import reactive.inventory.MongoRepoLike
import reactivemongo.api.collections.default.BSONCollection
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.simplyscala.{MongoEmbedDatabase, MongodProps}

object TestMongoRepo extends MongoRepoLike
with MongoEmbedDatabase{

  var mongoProps: MongodProps = mongoStart() //by default port = 12345

  override val collection: BSONCollection = {
    val connection = driver.connection(List(s"localhost:12345"))
    val db = connection("inventory")
    db.collection("inventory")
  }

  def stopMongo() {
    mongoStop(mongoProps)
  }
}
