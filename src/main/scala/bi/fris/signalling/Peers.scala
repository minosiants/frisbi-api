package bi.fris
package signalling

import akka.actor._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.util.Timeout
import bi.fris.signalling.TopicSignallingProtocol._
import bi.fris.topic.Topics
import de.heikoseeberger.akkasse.WithHeartbeats

import scala.concurrent.duration.DurationInt


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
  case class RemovePublisher(peerId:String)
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

      sender() ! Source.actorRef[SignallingEvent](100, OverflowStrategy.dropHead)
        .map(TopicSignallingProtocol.flowEventToServerSentEvent)
        .via(WithHeartbeats(5.second))
        .mapMaterializedValue(source =>  {
          peers += peerId -> PeerSocket(peerId, username, context.watch(source), topicId)
        })


    }
    case Terminated(ref) => {
      context.unwatch(ref)
      peers.values find(ps => ps.ssePublisher == ref) foreach(ps => removePublisher(ps.peerId))
    }
    case GetPeersOfTopic(topicId, without) => {

      val result = Some(peers.values.filter { p => !without.contains(p.peerId) && p.topicId == topicId})
      sender() ! result
    }
    case GetPeer(peerId) =>  {
      sender() ! peers.get(peerId)
    }
    case RemovePublisher(peerId) =>  {
      removePublisher(peerId)
    }

   // case Update

  }

  private def removePublisher(peerId:String): Unit = {
    peers.get(peerId).map{pc =>
      Topics(context.system).deleteParticipant(pc.topicId, pc.username)
    }
    peers -= peerId
  }


}
