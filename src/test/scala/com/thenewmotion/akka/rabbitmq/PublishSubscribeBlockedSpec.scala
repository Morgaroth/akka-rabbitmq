package com.thenewmotion.akka.rabbitmq

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.testkit.TestProbe
import com.thenewmotion.akka.rabbitmq.BlockedConnectionHandler.{ QueueBlocked, QueueUnblocked }
import com.thenewmotion.akka.rabbitmq.ChannelActor.{ ConnectionIsBlocked, SuccessfullyQueued }

import scala.concurrent.duration.FiniteDuration

/**
 * @author Mateusz Jaje
 */
class PublishSubscribeBlockedSpec extends ActorSpec {
  "PublishSubscribeBlock" should {

    "Be aware of Blocked Connection" in new TestScope {
      val factory = new ConnectionFactory()

      private val props = ConnectionActor.props(
        factory,
        setupConnection = BlockedConnectionSupport.setupConnection)
      val connection = system.actorOf(props, "rabbitmq")
      val exchange = "amq.fanout"

      def setupPublisher(channel: Channel, self: ActorRef) {
        val queue = channel.queueDeclare().getQueue
        channel.queueBind(queue, exchange, "")
      }

      connection ! CreateChannel(ChannelActor.props(setupPublisher), Some("publisher"))
      val ChannelCreated(publisher) = expectMsgType[ChannelCreated]

      def setupSubscriber(channel: Channel, self: ActorRef) {
        val queue = channel.queueDeclare().getQueue
        channel.queueBind(queue, exchange, "")
        val consumer = new DefaultConsumer(channel) {
          override def handleDelivery(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]) {
            messageCollector.ref ! fromBytes(body)
          }
        }
        channel.basicConsume(queue, true, consumer)
      }

      connection ! CreateChannel(ChannelActor.props(setupSubscriber), Some("subscriber"))
      val ChannelCreated(subscriber) = expectMsgType[ChannelCreated]

      val msgs = 1 to 33
      msgs.map { x =>
        publisher ! ChannelMessage(_.basicPublish(exchange, "", null, toBytes(x)), dropIfNoChannel = false)
        expectMsg(SuccessfullyQueued)
      }
      connection ! QueueBlocked("test block")
      Thread.sleep(10)
      msgs.map { x =>
        publisher ! ChannelMessage(_.basicPublish(exchange, "", null, toBytes(x)), dropIfNoChannel = false)
        expectMsg(ConnectionIsBlocked)
      }
      connection ! QueueUnblocked
      Thread.sleep(10)
      msgs.map { x =>
        publisher ! ChannelMessage(_.basicPublish(exchange, "", null, toBytes(x)), dropIfNoChannel = false)
        expectMsg(SuccessfullyQueued)
      }
      messageCollector.expectMsgAllOf(FiniteDuration(200, TimeUnit.SECONDS), List(msgs, msgs).flatten: _*)

      def fromBytes(x: Array[Byte]) = new String(x, "UTF-8").toLong

      def toBytes(x: Long) = x.toString.getBytes("UTF-8")
    }

  }

  private abstract class TestScope extends ActorScope {
    val messageCollector = TestProbe()
  }
}