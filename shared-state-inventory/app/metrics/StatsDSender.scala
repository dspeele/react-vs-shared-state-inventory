package metrics

import akka.actor.Actor
import com.typesafe.config.ConfigFactory
import java.net.{InetSocketAddress, InetAddress}
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

object StatsDSender {
  case class IncrementCounter (bucket: String)
  case class SendTimer (bucket: String, value: Long)
}

class StatsDSender extends Actor {

  import StatsDSender._

  val sendBuffer = ByteBuffer.allocate(1024)
  val config = ConfigFactory.load()
  val address = new InetSocketAddress(InetAddress.getByName(config.getString("statsd.server")), config.getInt("statsd.port"))
  val channel = DatagramChannel.open()

  def send(message: String) = {
    sendBuffer.put(message.getBytes("utf-8"))
    sendBuffer.flip()
    channel.send(sendBuffer, address)
    sendBuffer.limit(sendBuffer.capacity())
    sendBuffer.rewind()
  }

  def receive = {
    case SendTimer (bucket, value) =>
      send(s"$bucket:$value|ms")
    case IncrementCounter (bucket) =>
      send(s"$bucket:1|c")
  }
}