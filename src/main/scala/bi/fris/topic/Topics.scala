package bi.fris
package topic

import akka.actor.{ActorLogging, ActorRef, ExtendedActorSystem, Extension, ExtensionKey, Props, actorRef2Scala}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess}
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import akka.util.Timeout
import bi.fris.account.AccountProtocol.Profile
import bi.fris.topic.TopicProtocol._
import com.github.nscala_time.time.Imports.DateTime

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object Topics extends ExtensionKey[Topics]

class Topics(protected val system: ExtendedActorSystem) extends Extension with Sharding {

  def createTopic(topic: Topic)(implicit timeout: Timeout, sender: ActorRef): Future[TopicCreated] = {
    val cmd = CreateTopic(topic)
    askEntry(cmd.id, cmd).mapTo[TopicCreated]
  }
  def deleteTopic(id: String)(implicit timeout: Timeout, sender: ActorRef): Future[TopicDeleted] = {
    askEntry(id, DeleteTopic(id)).mapTo[TopicDeleted]
  }

  def updateBackground(id: String, url:String)(implicit timeout: Timeout, sender: ActorRef): Future[BackgroundUpdated] = {
    askEntry(id, UpdateBackground(id, url)).mapTo[BackgroundUpdated]
  }


  def addParticipant(id: String, username: String )(implicit timeout: Timeout, sender: ActorRef): Future[ParticipantJoined] = {
    askEntry(id, AddParticipant(id, username)).mapTo[ParticipantJoined]
  }
  def deleteParticipant(id: String, username: String)(implicit timeout: Timeout, sender: ActorRef): Future[ParticipantLeft] = {
	  askEntry(id, DeleteParticipant(id, username)).mapTo[ParticipantLeft]
  }

  def eventsSource() = {
    Source(ActorPublisher[TopicEvent](system.actorOf(TopicEventPublisher.props)))
  }

  override protected def typeName: String = "TopicProcessor"
  override protected def props: Props = TopicPersistent.props

}

class TopicPersistent extends PersistentActor with ActorLogging {

  var state = TopicPersistent.TopicState(Topic("", "", Profile("", "", None, confirmed = false, null, null), secret=false), deleted = false)

  override def persistenceId: String = self.path.parent.name + "-" + self.path.name

  private val mediator = DistributedPubSub(context.system).mediator

  context.setReceiveTimeout(2.minutes)

  private val  handler = (sender:ActorRef) => (event:TopicEvent) => {
    state.updated(event)
    mediator ! DistributedPubSubMediator.Publish(TopicEventKey, event)
    sender ! event
  }

  override def receiveCommand = {
    case CreateTopic(topic) =>
      persist(TopicCreated(topic))(handler(sender()))

    case DeleteTopic(id, updated_at) =>
      persist(TopicDeleted(id, updated_at))(handler(sender()))


    case AddParticipant(id, username, timestamp) =>
      persist(ParticipantJoined(id, username, timestamp))(handler(sender()))

    case UpdateBackground(id, url, timestamp) =>
      persist(BackgroundUpdated(id, url, timestamp))(handler(sender()))

    case DeleteParticipant(id, username, timestamp) =>
      persist(ParticipantLeft(id, username, timestamp))(handler(sender()))

    case "snap" => saveSnapshot(state)
    case SaveSnapshotSuccess(md) =>
      println(s"snapshot saved (metadata = $md)")
    case SaveSnapshotFailure(md, e) =>
      println(s"snapshot saving failed (metadata = $md, error = ${e.getMessage})")

  }
  override def receiveRecover = {
    case evt: TopicEvent => {
      state = state.updated(evt)
    }
  }
}
object TopicPersistent {
  type Participant = (String, DateTime)
  case class TopicState(topic: Topic, deleted: Boolean = false, activeUsers: List[Participant] = Nil, inactiveUsers: List[Participant] = Nil) {
    def updated(evt: TopicEvent): TopicState = evt match {

      case TopicCreated(t) =>
        copy(t)
      case BackgroundUpdated(id, url, updated_at) =>
        copy(topic.copy(updated_at = updated_at, background = Some(url)))

      case TopicDeleted(id, updated_at) => copy(topic.copy(updated_at = updated_at), deleted = true)

      case ParticipantJoined(id, username, timestamp) =>
        val newActiveUsers = (username, timestamp) :: activeUsers.filterNot(_._1 == username)
        val newInactiveUsers = inactiveUsers.filterNot(_._1 == username)
        copy(activeUsers = newActiveUsers, inactiveUsers = newInactiveUsers)

      case ParticipantLeft(id, username, timestamp) =>
        val newInactiveUsers = (username, timestamp) :: inactiveUsers.filterNot(_._1 == username)
        val newActiveUsers = activeUsers.filterNot(_._1 == username)
        copy(activeUsers = newActiveUsers, inactiveUsers = newInactiveUsers)

      case _ => this
    }
  }
  def props = Props(new TopicPersistent())
}