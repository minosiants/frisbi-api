package bi.fris
package signalling

import akka.actor.{Actor, ActorRef}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

import akka.util.Timeout
import bi.fris.TokenHttpAuthenticationDirectives.TokenAuth._
import bi.fris.common.ErrorMessage._
import bi.fris.common.FutureO
import bi.fris.signalling.Peers.{CreatePeerEventSource, GetPeer, GetPeersOfTopic}
import bi.fris.signalling.TopicSignallingProtocol._
import bi.fris.topic.Topics
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import de.heikoseeberger.akkasse.{ServerSentEvent, EventStreamMarshalling}

import scala.concurrent.ExecutionContext

object TopicSignallingRoute {
  type PeerSocketO = Option[PeerSocket]
  type PeerSocketOL = Option[List[PeerSocket]]
}

trait TopicSignallingRoute extends CORSDirectives with JSONWebToken {
  this: Actor =>

  import PlayJsonSupport._
  import TopicSignallingRoute._

  def topicSignallingRoute(peers:ActorRef)(implicit ec: ExecutionContext, mat: ActorMaterializer, askTimeout: Timeout) = {
    respondWithCors {
      pathPrefix("signalling" / "topics" / Segment) { topicId =>
        path("offer") {
          optionsForCors ~
            post {
              entity(as[MakeOffer]) { offer =>
                tokenAuth(ec) { user =>
                  onSuccess {
                    (for {
                      toPeer <- FutureO((peers ? GetPeer(offer.dst)).mapTo[PeerSocketO])
                      fromPeer <- FutureO((peers ? GetPeer(offer.src)).mapTo[PeerSocketO])
                    } yield toPeer.ssePublisher ! offer.copy(fromUser = Some(fromPeer.username))).future
                  } {
                    case Some(a) => complete(OK)
                    case None => complete(Unable_Send_Offer)
                  }
                }
              }
            }
        } ~ // offer
          path("answer") {
            optionsForCors ~
              post {
                tokenAuth(ec) { user =>
                  entity(as[MakeAnswer]) { answer =>
                    onSuccess((peers ? GetPeer(answer.dst)).mapTo[PeerSocketO]) {
                      case Some(ps) => {
                        ps.ssePublisher ! answer
                        complete(OK)
                      }
                      case None => complete(Unable_Send_Answer)
                    }
                  }
                }
              }
          } ~ // answer
          path("candidate") {
            optionsForCors ~
              post {
                tokenAuth(ec) { user =>
                  entity(as[MakeCandidate]) { candidate =>
                    onSuccess((peers ? GetPeer(candidate.dst)).mapTo[PeerSocketO]) {
                      case Some(ps) => {
                        ps.ssePublisher ! candidate
                        complete(OK)
                      }
                      case None => complete(OK)
                    }
                  }
                }
              }
          } ~ // candidate
          path("connect") {
            optionsForCors ~
              post {
                tokenAuth(ec) { user =>
                  parameters('peerId.as[String]) { peerId =>
                    complete(OK)
                  }
                }
              }
          } ~ // connect
          path("join") {
            optionsForCors ~
              post {
                tokenAuth(ec) { user =>
                  entity(as[Join]) { join =>
                    onSuccess {
                      (for {
                        p <- FutureO((peers ? GetPeer(join.peerId)).mapTo[PeerSocketO])
                        peers <- FutureO((peers ? GetPeersOfTopic(topicId, List(join.peerId))).mapTo[PeerSocketOL])
                      } yield peers).future
                    } {
                      case Some(peers) => {
                        Topics(context.system).addParticipant(topicId, user.username)
                        complete(OK, PeerJoined(join.peerId, peers))
                      }
                      case None => complete(Unable_Join)
                    }
                  }
                }
              }
          } // join
      }
    }
  }


  def topicSignallingSseRoute(peers:ActorRef)(implicit ec: ExecutionContext, mat: ActorMaterializer, askTimeout: Timeout) = {
    import EventStreamMarshalling._
    respondWithCors {
      path("signalling" / "topics" / "sse" / Segment) { topicId =>
        optionsForCors ~
          get {
            parameters('peerId.as[String], 'token.as[String]) { (peerId, token) =>
              onSuccess(decodeF(token)) {
                case Some(username) => {
                  complete {
                    (peers ? CreatePeerEventSource(peerId, username, topicId)).mapTo[Source[ServerSentEvent, Any]]
                  }
                }
                case None => complete(Invalid_Token)
              }
            }
          }
      }
    }
  }

}

