package bi.fris
package topic

import java.util.UUID

import akka.actor.{Props, ActorLogging, ActorRef, Actor}
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.FlowMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import akka.util.Timeout
import bi.fris.TokenHttpAuthenticationDirectives.TokenAuth._
import bi.fris.account.AccountProtocol.Profile
import bi.fris.aws.S3Client
import bi.fris.common.ErrorMessage._
import bi.fris.common.Validation
import bi.fris.signalling.Peers.CreatePeerEventSource
import bi.fris.signalling.TopicSignallingEventPublisher.RemovePublisher
import bi.fris.signalling.TopicSignallingProtocol.SignallingEvent
import bi.fris.topic.TopicProtocol._
import de.heikoseeberger.akkahttpjsonplay.PlayJsonMarshalling
import de.heikoseeberger.akkasse._
import org.apache.commons.codec.binary.Base64
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

object TopicRoute {

  case class CreateTopicReq(id: String, title: String, secret:Boolean) extends Validation {
    require(validText(id, 50), "id invalid")
    require(validText(title, 140), "title invalid")
  }


  case class PatchTopicReq(op: String, path: String, value: String) {
    require(op == "replace" && path == "background", "op invalid")

    def decode() = {
      Base64.decodeBase64(value.substring(value.indexOf(',') + 1))
    }
  }

  implicit val createTopicReqFormat = Json.format[CreateTopicReq]
  implicit val patchTopicReqFormat = Json.format[PatchTopicReq]
}

trait TopicsRoute extends CORSDirectives with EventStreamMarshalling{
  this: Actor =>

  import PlayJsonMarshalling._
  import TopicRoute._

  def topicsRoute(implicit ec: ExecutionContext, mat: FlowMaterializer, askTimeout: Timeout) = {
    respondWithCors {
      pathPrefix("topics") {
        pathEndOrSingleSlash {
          optionsForCors ~
            post {
              tokenAuth(ec) { user =>
                entity(as[CreateTopicReq]) { req =>
                  onSuccess(Topics(context.system).createTopic(Topic(id = req.id, title = req.title, author = user, secret = req.secret))) {
                    case TopicCreated(_) => complete(Created -> Topic(id = req.id, title = req.title, author = user, secret = req.secret))
                    case _ => complete(BadRequest)
                  }
                }
              }
            }
        } ~
          path("timeline") {
            optionsForCors ~
              get {
                parameters('count.as[Int].?, 'since.as[Long].?, 'order.as[String].?) { (count, since, order) =>
                  onSuccess(TopicView(context.system).topics(count, since, order)) {
                    case List() | Nil => complete(NotFound)
                    case l => complete(l)
                  }
                }
              }
          } ~
          path("user_timeline") {
            optionsForCors ~
              get {
                parameters('username, 'count.as[Int].?, 'since.as[Long].?) { (username, count, since) =>
                  tokenAuthO(ec) { user =>
                  val secret = user.exists(_.username == username)
                  onSuccess(TopicView(context.system).userTopics(username, count, since, secret)) {
                    case List() | Nil => complete(NotFound)
                    case l => complete(l)
                  }
                }
                }
              }
          } ~
          path(Segment) { topicId =>
            optionsForCors ~
              get {
                onSuccess(TopicView(context.system).topic(topicId)) {
                  case Some(topic) => complete(topic)
                  case None => complete(Unable_Find_Topic)
                }
              } ~
              delete {
                tokenAuth(ec) { user =>
                  onSuccess(TopicView(context.system).topic(topicId, Some(user.username))) {
                    case Some(topic) =>
                      Topics(context.system).deleteTopic(topicId)
                      complete(OK)
                    case None => complete(Unable_Find_Topic)
                  }
                }
              } ~
              patch {
                tokenAuth(ec) { user =>
                  entity(as[PatchTopicReq]) { req =>
                    onSuccess(
                      TopicView(context.system)
                        .topic(topicId, Some(user.username))
                        .flatMap {
                        case None => Future.successful(Unable_Find_Topic)
                        case Some(topic) =>
                          for {
                            uri <- S3Client(context.system).upload(UUID.randomUUID().toString, req.decode())
                            _ <- Topics(context.system).updateBackground(topicId, uri)
                          } yield uri
                      }) {
                      case Unable_Find_Topic => complete(Unable_Find_Topic)
                      case uri: String => complete(uri)
                    }
                  }
                }
              }
          }
      }
    }
  }
  private val mediator = DistributedPubSubExtension(context.system).mediator
  def topicsStreamingRoute(implicit ec: ExecutionContext, mat: FlowMaterializer, askTimeout: Timeout) = {

    respondWithCors {
      path("streaming"/"topics") {
        optionsForCors ~
          get {
            tokenAuthO(ec) { user =>
              complete {
                Source(ActorPublisher[ServerSentEvent](
                  context.actorOf(TopicsEventPublisher.props(mediator, user))))
              }
            }
          }
      }
    }
  }
}

import scala.concurrent.duration.DurationInt

class TopicsEventPublisher(mediator: ActorRef, user:Option[Profile]) extends EventPublisher[TopicEvent](100, 5 seconds ) with ActorLogging {

  mediator ! DistributedPubSubMediator.Subscribe(TopicEventKey, self)

  override def receiveEvent = {
    case event: TopicEvent => onEvent(event)
  }
  override def postStop(): Unit = {
    mediator ! DistributedPubSubMediator.Unsubscribe(TopicEventKey, self)
  }

}

object TopicsEventPublisher {
  def props(mediator: ActorRef, user:Option[Profile]) = Props(new TopicsEventPublisher(mediator, user))
}

//curl 'http://localhost:8080/topics/'  -H 'Authorization: talkopedia sgdfgdfgdg'

//curl 'http://localhost:8080/users/timeline'  -H 'Authorization: talkopedia eyJ0eXBlIjoiSldUIiwiYWxnIjoiSFMyNTYifQ.eyJleHBpcmUiOiIxNDIyMzQyMDk2NzIzIiwidXNlcm5hbWUiOiJrYXNwYXIifQ.ltBeLSzS_wYGfc4UcdvAriBvjjCx7fxCGCMYFzu6OS4'

