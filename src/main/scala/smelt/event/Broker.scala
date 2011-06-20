package smelt.event

import java.util.concurrent.Executors
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.handler.codec.serialization.{ObjectDecoder, ObjectEncoder}
import java.net.InetSocketAddress
import org.jboss.netty.channel._
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong
import java.lang.IllegalStateException

class Broker(val port: Int = 63210) {

  val logger = LoggerFactory.getLogger(classOf[Broker])
  var channel: Option[Channel] = None

  def start() {
    if (channel.isDefined)
      throw new IllegalStateException("Already started")

    // Configure the server.
    val bootstrap = new ServerBootstrap(
      new NioServerSocketChannelFactory(
        Executors.newCachedThreadPool(),
        Executors.newCachedThreadPool()))

    // Set up the pipeline factory.
    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      def getPipeline: ChannelPipeline = {
        Channels.pipeline(
          new ObjectEncoder,
          new ObjectDecoder,
          new BrokerHandler)
      }
    })

    // Bind and start to accept incoming connections.
    channel = Some(bootstrap.bind(new InetSocketAddress(port)))
  }

  def stop() {
    logger.info("Stoping broker...")
    channel.foreach(_.close().addListener(new ChannelFutureListener {
      def operationComplete(p1: ChannelFuture) {
        logger.info("Broker stopped!")
      }
    }))
  }

  class BrokerHandler extends SimpleChannelUpstreamHandler {
    val logger = LoggerFactory.getLogger(classOf[BrokerHandler])
    val messageReceivedCount = new AtomicLong

    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
      logger.warn("Unexpected exception from downstream.", e.getCause)
      e.getChannel.close
    }

    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
      // Echo back the received object to the client.
      messageReceivedCount.incrementAndGet
      val message = e.getMessage
      e.getChannel.write(message)
      logger.info("Broker received: {}", message)
    }

    override def handleUpstream(ctx: ChannelHandlerContext, e: ChannelEvent) {
      if (e.isInstanceOf[ChannelStateEvent] &&
        e.asInstanceOf[ChannelStateEvent].getState != ChannelState.INTEREST_OPS) {
        logger.info(e.toString)
      }
      super.handleUpstream(ctx, e);
    }
  }

}
