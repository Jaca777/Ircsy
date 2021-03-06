package pl.jaca.ircsy.clientnode.messagecollection

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable}
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import pl.jaca.ircsy.chat.messages.ChannelMessage
import pl.jaca.ircsy.chat.{ConnectionDesc, ServerDesc}
import pl.jaca.ircsy.clientnode.connection.ConnectionObservableProxy.{ChannelMessageReceived, LeftChannel}
import pl.jaca.ircsy.clientnode.connection.ConnectionProxyPublisher.{ChannelConnectionFound, FindChannelConnection}
import pl.jaca.ircsy.clientnode.messagecollection.ChannelMessageCollector.{ChannelMessageCollectorSubject, Stop}
import pl.jaca.ircsy.clientnode.messagecollection.repository.MessageRepositoryFactory
import pl.jaca.ircsy.clientnode.observableactor.ObservableActorProtocol.{Observer, ObserverSubject, RegisterObserver, UnregisterObserver}

import scala.concurrent.duration.Duration

/**
  * @author Jaca777
  *         Created 2016-05-01 at 23
  */
class ChannelMessageCollector(serverDesc: ServerDesc, channelName: String, pubSubMediator: ActorRef, repositoryFactory: MessageRepositoryFactory) extends Actor with ActorLogging {


  implicit val executionContext = context.dispatcher
  val config = context.system.settings.config
  val broadcastInterval = Duration.fromNanos(config.getDuration("app.collector.broadcast-interval").toNanos)
  val repository = repositoryFactory.newRepository()
  val observer = Observer(self, Set(ChannelMessageCollectorSubject(channelName)))

  log.debug(s"Starting channel message collector ($serverDesc channel $channelName), subscribing to channel topic...")
  pubSubMediator ! Subscribe(s"channels-$serverDesc", self)

  override def receive: Receive = lookingForProxy()

  def lookingForProxy(broadcasting: Cancellable): Receive = {
    case ChannelConnectionFound(`channelName`, connection, proxy) if connection.getServer == serverDesc =>
      log.debug(s"Channel connection found ($serverDesc channel $channelName), registering observer...")
      proxy ! RegisterObserver(observer)
      context become collecting(connection, proxy)
    case Stop =>
      log.debug(s"Stopping collector ($serverDesc channel $channelName) during looking for proxy...")
      stop()
  }

  def lookingForProxy(): Receive = {
    log.debug(s"Starting looking for proxy connected to ($serverDesc channel $channelName)...")
    val broadcastMessage = Publish(s"channels-$serverDesc", FindChannelConnection(serverDesc, channelName))
    val broadcasting = context.system.scheduler.schedule(Duration.Zero, broadcastInterval, pubSubMediator, broadcastMessage)
    lookingForProxy(broadcasting)
  }

  def collecting(connection: ConnectionDesc, proxy: ActorRef): Receive = {
    case ChannelMessageReceived(message) =>
      repository.addChannelMessage(serverDesc, message)
    case LeftChannel => context become lookingForProxy()
    case Stop =>
      log.debug(s"Stopping collector ($serverDesc channel $channelName) during collection...")
      proxy ! UnregisterObserver(observer)
      stop()
  }

  def stop() = context.stop(self)

}

object ChannelMessageCollector {

  object Stop

  case class ChannelMessageCollectorSubject(channelName: String) extends ObserverSubject {
    override def isInterestedIn(notification: Any): Boolean = notification match {
      case ChannelMessageReceived(msg: ChannelMessage) if msg.getChannel == channelName => true
      case LeftChannel(_, _) => true
    }
  }

}

