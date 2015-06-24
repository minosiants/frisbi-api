package bi.fris
package signalling

import akka.actor.{ActorRef, ActorLogging, Actor, Props}
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import scala.concurrent.duration.DurationInt
import akka.util.Timeout
import bi.fris.signalling.TopicSignallingProtocol._
import bi.fris.topic.Topics
import de.heikoseeberger.akkasse.{EventPublisher, ServerSentEvent}


object Peers {
  def props = Props(new Peers ())

  import scala.collection._
  var peers: mutable.Map[PeerId, PeerSocket] = mutable.Map.empty

  def findPreerSoket(username: String, topicId: String): Option[PeerSocket] = {
    peers.values.find(ps => ps.topicId == topicId && ps.username == username)
  }

  def topicPeers(joinedPeer: PeerId, topicId: String)(p: PeerSocket) = {
    p.peerId != joinedPeer && p.topicId == topicId
  }

  sealed trait PeerEvent
  case class CreatePeerEventSource(peerId:String, username:String, topicId:String) extends PeerEvent
  case class GetPeer(peerId:String) extends PeerEvent
  case class GetPeersOfTopic(topicId:String, filter:List[PeerId]=Nil) extends PeerEvent
  object RemoveInactivePeers


}

class Peers extends Actor with ActorLogging{
  import Peers._
  implicit val askTimeout: Timeout = 15 seconds
  override def receive: Receive = {

    case CreatePeerEventSource(peerId, username, topicId) => {
      val ps = peers.values.filter { p => p.username == username && p.topicId == topicId}
      ps.map { p =>
        peers -= p.peerId
      }
      val publisher = context.actorOf(TopicSignallingEventPublisher.props(peerId, self))
      peers += peerId -> PeerSocket(peerId, username, publisher, topicId)
      sender() ! Source(ActorPublisher[ServerSentEvent](publisher))
    }
    case GetPeersOfTopic(topicId, without) => {

      val result = Some(peers.values.filter { p => !without.contains(p.peerId) && p.topicId == topicId})
      sender() ! result
    }
    case GetPeer(peerId) =>  {
      sender() ! peers.get(peerId)
    }
    case TopicSignallingEventPublisher.RemovePublisher(peerId) =>  {
      peers.get(peerId).map{pc =>
        Topics(context.system).deleteParticipant(pc.topicId, pc.username)
      }
      peers -= peerId
    }
   // case Update

  }
}

class TopicSignallingEventPublisher(peerId:String, owner:ActorRef) extends EventPublisher[SignallingEvent](100, 5 seconds ) with ActorLogging {
  import TopicSignallingEventPublisher._

  override def receiveEvent = {
    case event: SignallingEvent => onEvent(event)
  }
  override def postStop(): Unit = {
    owner ! RemovePublisher(peerId)
  }

}

object TopicSignallingEventPublisher {
  case class RemovePublisher(peerId:String)
  def props(peerId:String, parent:ActorRef) = Props(new TopicSignallingEventPublisher(peerId, parent))
}
