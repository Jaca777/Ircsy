package pl.jaca.ircsy.clientnode.observableactor

import akka.actor.ActorRef

/**
  * @author Jaca777
  *         Created 2016-05-04 at 20
  */
object ObservableActorProtocol {
  case class Observer(ref: ActorRef, subjects: ObserverSubject*)


  abstract class ObserverSubject {
    def isInterestedIn(notification: Any): Boolean
  }

  case class ClassFilterSubject(classes: Class[_]*) extends ObserverSubject{
    override def isInterestedIn(notification: Any): Boolean = classes contains notification.getClass
  }

  case class RegisterObserver(observer: Observer)

  case class UnregisterObserver(observer: Observer)
}