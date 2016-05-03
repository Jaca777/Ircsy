package pl.jaca.ircsy.clientnode.messagescollection

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.WordSpecLike
import org.scalatest.mockito.MockitoSugar
import pl.jaca.ircsy.util.test.MoreMockitoSugar

/**
  * @author Jaca777
  *         Created 2016-05-02 at 12
  */
class ServerMessageCollectorSpec extends TestKit(ActorSystem("ServerMessageCollectorSpec")) with WordSpecLike with MockitoSugar with MoreMockitoSugar {
  "ServerMessageCollector" should {

  }
}
