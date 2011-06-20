package smelt.event

import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.bootstrap.ClientBootstrap
import java.util.concurrent.Executors
import org.jboss.netty.handler.codec.serialization.{ObjectDecoder, ObjectEncoder}
import java.net.InetSocketAddress
import org.jboss.netty.channel._
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong
import javax.management.remote.rmi._RMIConnection_Stub

class EventBusLocal(val brokerAddr: String = "localhost", val brokerPort: Int = 63210) extends EventBus {

  val logger = LoggerFactory.getLogger(classOf[EventBusLocal])
  var channel: Option[Channel] = None

  def start() {
    // Configure the client.
    val bootstrap = new ClientBootstrap(
      new NioClientSocketChannelFactory(
        Executors.newCachedThreadPool(),
        Executors.newCachedThreadPool()));

    // Set up the pipeline factory.
    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      def getPipeline: ChannelPipeline = {
        Channels.pipeline(
          new ObjectEncoder,
          new ObjectDecoder,
          new EventBusLocalHandler)
      }
    });

    // Start the connection attempt.
    val channelFuture: ChannelFuture = bootstrap.connect(new InetSocketAddress(brokerAddr, brokerPort))
    channelFuture.addListener(new ChannelFutureListener {
      def operationComplete(cf: ChannelFuture) {
        channel = Some(cf.getChannel)
      }
    })
  }

  def stop() {
    logger.info("Stoping bus...")
    channel.foreach(_.close().addListener(new ChannelFutureListener {
      def operationComplete(p1: ChannelFuture) {
        logger.info("Bus stopped!")
      }
    }))
  }


  def publish(event: Event) {
    channel.foreach { ch =>
      ch.write(event)
      logger.info("Event published to {}", ch)
    }
  }

  class EventBusLocalHandler extends SimpleChannelUpstreamHandler {
    val logger = LoggerFactory.getLogger(classOf[EventBusLocalHandler])
    val messageReceivedCount = new AtomicLong

    def firstMessage = "Hello!"

    override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
      // Send the first message if this handler is a client-side handler.
      e.getChannel.write(firstMessage)
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
      logger.warn("Unexpected exception from downstream.", e.getCause)
      e.getChannel.close
    }

    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
      messageReceivedCount.incrementAndGet
      //e.getChannel.write(e.getMessage)
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