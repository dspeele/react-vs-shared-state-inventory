package reactive.inventory

import akka.actor._
import akka.http.scaladsl.Http
import akka.stream.ActorFlowMaterializer
import com.typesafe.config.ConfigFactory

//Start http server
object ReactiveInventoryService extends App
with Service {
  override implicit val system = ActorSystem("ReactiveInventory", ConfigFactory.load().getConfig("ReactiveInventory"))
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorFlowMaterializer()

  initializeManagers()
  initializeMetrics()

  val config = ConfigFactory.load()

  //Create server binding listening on specified interface and port and bind/handle via route object
  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
