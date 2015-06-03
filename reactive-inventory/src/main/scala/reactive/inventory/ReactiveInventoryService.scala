package reactive.inventory

import akka.actor._
import akka.http.scaladsl.Http
import akka.stream.ActorFlowMaterializer
import com.typesafe.config.ConfigFactory

//Start http server
object ReactiveInventoryService extends App
with Service {

  val config = ConfigFactory.load()

  override implicit val system = ActorSystem("ReactiveInventory", config.getConfig("ReactiveInventory"))
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorFlowMaterializer()

  initializeMetrics()
  initializeManagers()

  //Create server binding listening on specified interface and port and bind/handle via route object
  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
