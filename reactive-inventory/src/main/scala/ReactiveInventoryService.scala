import InventoryManager._
import StatsDSender._
import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.{ServerBinding, IncomingConnection}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpResponse
import akka.io.Udp._
import akka.routing.RoundRobinPool
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Source
import akka.util.{ByteString, Timeout}
import com.typesafe.config.ConfigFactory
import java.net.InetSocketAddress
import reactivemongo.core.commands.LastError
import akka.pattern.ask
import scala.concurrent.duration._
import reactivemongo.api._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try
import scala.async.Async.{async, await}

trait Router {
  //restart member of pool on error
  val oneForOneSupervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case e ⇒ SupervisorStrategy.Restart
  }
}

object StatsDSender {
  case class IncrementCounter (bucket: String)
  case class SendTimer (bucket: String, value: Long)
}

class StatsDSender extends Actor {

  import StatsDSender._

  val config = ConfigFactory.load()

  val address = new InetSocketAddress(config.getString("statsd.interface"), config.getInt("statsd.port"))

  def sendMessage(message: String) = {
    Send(ByteString(message), address)
  }

  def receive = {
    case SendTimer (bucket, value) => sendMessage(s"$bucket:$value|ms")
    case IncrementCounter (bucket) => sendMessage(s"$bucket:1|c")
  }
}

object Database {

  val config = ConfigFactory.load()

  val collection = connect()


  def connect(): BSONCollection = {

    val driver = new MongoDriver
    val connection = driver.connection(List(config.getString("mongo.interface") + ":" + config.getInt("mongo.port")))

    val db = connection(config.getString("mongo.database"))
    db.collection(config.getString("mongo.collection"))
  }

  def setInventory(sku: String, count: Int): Future[LastError] = {
    val document = BSONDocument("sku" -> sku, "count" -> count)

    // which returns Future[LastError]
    Database.collection
      .save(document)
  }

  def findAllInventory(): Future[List[BSONDocument]] = {
    val query = BSONDocument()

    // which returns Future[List[BSONDocument]]
    Database.collection
      .find(query)
      .cursor[BSONDocument]
      .collect[List]()
  }

  def findInventoryBySku(sku: String) : Future[Option[BSONDocument]] = {
    val query = BSONDocument("sku" -> sku)

    // which returns Future[Option[BSONDocument]]
    Database.collection
      .find(query)
      .one
  }
}

//object that stores message classes for the InventoryManager
object InventoryManager {
  case class GetInventory()
  case class UpdateInventory(quantity: Int)
  case class setSkuAndQuantity(sku: String, quantity: Int)
  case class InventoryMessage(message: String)
}


class InventoryManager extends Actor with Router {
  import InventoryManager._

  var sku: String = _
  var quantity: Int = 0

  override def preRestart(reason: Throwable,
                          message: Option[Any]) = {
    // The default behaviour was to stop the children
    // here but we don't want to do that
    // We still want to postStop() however.
    postStop()
  }

  def receive = {
    case setSkuAndQuantity(sku, quantity) => {
      this.sku = sku
      this.quantity = quantity
      async {
        await(Database.setInventory(sku, this.quantity)) match {
          case e: LastError => throw e
        }
      }
    }
    case GetInventory() => sender ! InventoryMessage(s"Inventory for sku $sku = $quantity")
    case UpdateInventory(quantity) => quantity >= quantity match {
      case true => {
        sender ! InventoryMessage(s"$quantity of sku $sku have been reserved")
        this.quantity -= quantity
        async {
          await(Database.setInventory(sku, this.quantity)) match {
            case e: LastError => throw e
          }
        }
      }
    }
  }
}

object RequestHandler {
  case class inventoryResponse ()
}

class RequestHandler extends Actor with Router {
  import InventoryManager._

  var inventoryRouter: ActorRef = context.system.deadLetters

  override def receive = {
    case HttpRequest(verb, uri, _, _, _) =>
      uri.path.toString().split( """/""") match {
        case service :: restOfPath if service == "inventory" => restOfPath match {
          case sku :: restOfPath => verb match {
            case get => context.actorSelection("/user/" + sku) forward GetInventory()
            case put => restOfPath match {
              case quantity :: restOfPath if Try(
                quantity.asInstanceOf[Int]).isSuccess =>
                context.actorSelection("/user/" + sku) forward UpdateInventory(quantity.asInstanceOf[Int])
              case _ => sender ! InventoryMessage("404- Unknown resource!")
            }
            case _ => sender ! InventoryMessage("404- Unknown resource!")
          }
          case _ => sender ! InventoryMessage("404- Unknown resource!")
        }
        case _ => sender ! InventoryMessage("404- Unknown resource!")
      }
  }
}

object ReactiveInventoryService extends App with Router {
  implicit val system = ActorSystem("ReactiveInventory", ConfigFactory.load().getConfig("ReactiveInventory"))
  implicit val executor = system.dispatcher
  implicit val materializer = ActorFlowMaterializer()
  implicit val timeout = Timeout(5 seconds)

  val config = ConfigFactory.load()

  val statsDSender = system.actorOf(Props[StatsDSender])

  val r = scala.util.Random

  for (sku <- 1 until 10000) {
    val inventoryManager = system.actorOf(Props[InventoryManager],sku.toString)
    inventoryManager ! setSkuAndQuantity(sku.toString, r.nextInt(100))
  }

  //create pool of 5 RequestHandlers to handle incoming requests with SupervisorStrategy from mixed in Router trait
  val httpRequestRouter: ActorRef = system.actorOf(RoundRobinPool(5, supervisorStrategy = oneForOneSupervisorStrategy).props(
    routeeProps = Props[RequestHandler]))

  //request handler method called on each IncomingConnection
  val requestHandler: HttpRequest => Future[HttpResponse] = {
    httpRequest: HttpRequest =>
      val startTime = System.currentTimeMillis
      val response: Future[HttpResponse] = async {
      HttpResponse(entity =
        Try(
          await(
            ask(httpRequestRouter, httpRequest))
            .asInstanceOf[InventoryMessage].message)
          .getOrElse("Error with request"))
    }
    response.onComplete(_ => {
      statsDSender ! SendTimer("reactive.duration", System.currentTimeMillis - startTime)
      statsDSender ! IncrementCounter("reactive.count")
    })
    response
  }

  //Create server binding listening on specified interface and port
  val serverBinding: Source[IncomingConnection, Future[ServerBinding]] = Http().bind(config.getString("http.interface"), config.getInt("http.port"))

  //For each incoming connection, pass it to handler to handle asynchronously
  serverBinding.runForeach { connection: IncomingConnection =>
    connection handleWithAsyncHandler requestHandler
  }
}
