package pl.jaca.ircsy.clientnode.messagecollection.cassandra

import java.net.{InetAddress, InetSocketAddress}

import com.datastax.driver.core.{Cluster, ConsistencyLevel, RegularStatement, SimpleStatement}
import pl.jaca.ircsy.chat.ServerDesc
import pl.jaca.ircsy.chat.messages.{ChannelMessage, PrivateMessage}
import pl.jaca.ircsy.clientnode.messagecollection.repository.MessageRepository

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * @author Jaca777
  *         Created 2016-05-10 at 19
  */
class CassandraMessageRepository(contactPoints: Set[InetSocketAddress],
                                 keyspace: String,
                                 channelMessagesTable: String,
                                 privateMessagesTable: String) extends MessageRepository {

  val cluster = Cluster.builder()
    .addContactPointsWithPorts(contactPoints.asJava)
    .build()

  val session = cluster.connect(keyspace)

  val channelMessageStatement = {
    val unprepared: RegularStatement =
      new SimpleStatement(s"INSERT INTO $channelMessagesTable VALUES (?, ?, ?, ?, ?, ?, ? uuid())").setConsistencyLevel(ConsistencyLevel.QUORUM).asInstanceOf[RegularStatement]
    session.prepare(unprepared)
  }

  override def addChannelMessage(server: ServerDesc, message: ChannelMessage): Try[Unit] = Try {
    val statement = channelMessageStatement.bind(server.toString,
      message.getChannel,
      message.getTime,
      message.getAuthor.getNick,
      message.getAuthor.getHostname,
      message.getAuthor.getIdent,
      message.getText)
    session.execute(statement)
  }

  val privateMessageStatement = {
    val unprepared: RegularStatement =
      new SimpleStatement(s"INSERT INTO $privateMessagesTable VALUES (?, ?, ?, ?, ?, ?, ?, ? uuid())").setConsistencyLevel(ConsistencyLevel.QUORUM).asInstanceOf[RegularStatement]
    session.prepare(unprepared)
  }

  override def addPrivateMessage(server: ServerDesc, message: PrivateMessage): Try[Unit] = Try {
    val statement = privateMessageStatement.bind(server.toString,
      message.getChat.getMainParticipantName,
      message.getChat.getSecondParticipantName,
      message.getTime,
      message.getAuthor.getNick,
      message.getAuthor.getHostname,
      message.getAuthor.getIdent,
      message.getText)
    session.execute(statement)
  }

  override def close() {
    session.close()
  }
}
