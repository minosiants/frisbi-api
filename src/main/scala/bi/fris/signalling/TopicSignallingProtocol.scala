package bi.fris
package signalling

import akka.actor.ActorRef
import de.heikoseeberger.akkasse.ServerSentEvent
import play.api.libs.functional.syntax._
import play.api.libs.json.{Writes, _}

object TopicSignallingProtocol extends TopicSignallingProtocol

trait TopicSignallingProtocol {

  import play.api.libs.json.Json

  import scala.collection._

  sealed trait SignallingEvent

  case class Join(peerId: PeerId, topicId: String, token: String)

  case class PeerJoined(peerId: String, activePeers: Iterable[PeerSocket])

  case class Sdp(sdp: String, `type`: String)
  case class Payload(sdp: Sdp, connectionId: String)

  case class MakeOffer(`type`: String, payload: Payload, dst: String, src: String, fromUser: Option[String] = None) extends SignallingEvent

  case class MakeAnswer(`type`: String, payload: Payload, dst: String, src: String, dstCon: String) extends SignallingEvent
  case class Candidate(candidate: String, sdpMid: String, sdpMLineIndex:Int)
  case class CandidatePayload(candidate:Candidate,  connectionId:String)
  case class MakeCandidate(`type`: String, dst: String, src: String, dstCon:Option[String], payload:CandidatePayload) extends SignallingEvent

  type PeerId = String
  type Username = String
  type Peer = (PeerId, Username)

  case class PeerSocket(peerId: PeerId, username: String, ssePublisher: ActorRef, topicId: String)

  implicit val sdpFormat = Json.format[Sdp]
  implicit val payloadFormat = Json.format[Payload]
  implicit val makeOfferFormat = Json.format[MakeOffer]
  implicit val makeAnswerFormat = Json.format[MakeAnswer]
  implicit val candidateFormat = Json.format[Candidate]
  implicit val candidatePayloadFormat = Json.format[CandidatePayload]
  implicit val makeCandidateFormat = Json.format[MakeCandidate]

  //implicit val sockedOpenedFormat = Json.format[SockedOpened]
  implicit val joinFormat = Json.format[Join]

  implicit val peerSocketFormat: Writes[PeerSocket] = (
    (__ \ 'peerId).write[String] ~
      (__ \ 'username).write[String] ~
      (__ \ 'topicId).write[String]
    )(ps => (ps.peerId, ps.username, ps.topicId))

  implicit val peerJoinedFormat = Json.writes[PeerJoined]

  implicit def flowEventToServerSentEvent(event: SignallingEvent): ServerSentEvent =
    event match {
      case offer: MakeOffer =>
        ServerSentEvent(Json.toJson(offer).toString(), "offer")
      case answer: MakeAnswer =>
        ServerSentEvent(Json.toJson(answer).toString(), "answer")
      case candidate: MakeCandidate =>
        ServerSentEvent(Json.toJson(candidate).toString(), "candidate")
      case _ =>   ServerSentEvent("unsupported event", "error")
    }


}
