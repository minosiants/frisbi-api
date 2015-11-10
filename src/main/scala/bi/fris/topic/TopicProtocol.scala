package bi.fris
package topic


import bi.fris.account.AccountProtocol.Profile
import de.heikoseeberger.akkasse.ServerSentEvent
import org.joda.time.DateTime
import play.api.libs.json.Json

object TopicProtocol {

  case class Participant(username:String, active:Boolean=false)
  case class Topic(
    id: String,
    title: String,
    author: Profile,
    secret:Boolean,
    background: Option[String] = None,
    participants: List[Participant] = Nil,
    countActive:Int = 0,
    created_at: DateTime = DateTime.now,
    updated_at: DateTime = DateTime.now)

  sealed trait TopicEvent
  case class TopicCreated(topic: Topic) extends TopicEvent
  case class TopicDeleted(id: String, updated_at: DateTime) extends TopicEvent
  case class ParticipantJoined(id: String, username: String, timestamp: DateTime = DateTime.now) extends TopicEvent
  case class ParticipantLeft(id: String, username: String, timestamp: DateTime = DateTime.now) extends TopicEvent
  case class BackgroundUpdated(id: String, url:String, timestamp: DateTime = DateTime.now) extends TopicEvent

  val TopicEventKey = "topic-event"

  sealed trait TopicCommand {
    def id: String
  }
  
  case class CreateTopic(topic: Topic) extends TopicCommand {
    def id: String = topic.id
  }
  case class DeleteTopic(id: String, updated_at: DateTime = DateTime.now) extends TopicCommand
  case class AddParticipant(id: String, username: String, timestamp: DateTime = DateTime.now) extends TopicCommand
  case class DeleteParticipant(id: String, username: String, timestamp: DateTime = DateTime.now) extends TopicCommand
  case class UpdateBackground(id: String, url:String, timestamp: DateTime = DateTime.now) extends TopicCommand


  sealed trait TopicQuery
  case class GetTopic(id: String) extends TopicQuery
  case class GetUserTopic(id: String, author: String) extends TopicQuery
  case class GetAllTopics() extends TopicQuery
  case class GetAllUserTopic(username: String) extends TopicQuery

  implicit val participantFormat = Json.format[Participant]
  implicit val topicFormat = Json.format[Topic]
  implicit val createTopicFormat = Json.format[CreateTopic]
  implicit val participantJoinedFormat = Json.format[ParticipantJoined]
  implicit val participantLeftFormat = Json.format[ParticipantLeft]
  implicit val topicDeletedFormat = Json.format[TopicDeleted]


  def flowTopicEventToServerSentEvent(event: TopicEvent): ServerSentEvent =
    event match {
      case t:TopicCreated =>
        ServerSentEvent(Json.toJson(t.topic).toString(), "topic-created")
      case t:TopicDeleted =>
        ServerSentEvent(Json.toJson(t).toString(), "topic-deleted")
      case p:ParticipantJoined =>
        ServerSentEvent(Json.toJson(p).toString(), "topic-participant-joined")
      case p:ParticipantLeft =>
        ServerSentEvent(Json.toJson(p).toString(), "topic-participant-left")
     // case _ => ServerSentEvent("unsupported event", "error")
    }
}


