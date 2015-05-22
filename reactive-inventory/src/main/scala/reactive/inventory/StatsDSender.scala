package reactive.inventory

import akka.actor.Actor
import akka.io.Udp.Send
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import java.net.InetSocketAddress

object StatsDSender {
  case class IncrementCounter (bucket: String)
  case class SendTimer (bucket: String, value: Long)
}

class StatsDSender extends Actor {

  import StatsDSender._

  val config = ConfigFactory.load()

  val address = new InetSocketAddress(config.getString("statsd.server"), config.getInt("statsd.port"))

  def sendMessage(message: String) = {
    Send(ByteString(message), address)
  }

  def receive = {
    case SendTimer (bucket, value) => sendMessage(s"$bucket:$value|ms")
    case IncrementCounter (bucket) => sendMessage(s"$bucket:1|c")
  }
}