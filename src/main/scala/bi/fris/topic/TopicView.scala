package bi.fris
package topic

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.{Actor, ActorLogging, ActorRef, ExtendedActorSystem, Extension, ExtensionKey, Props}
import akka.stream.scaladsl.Source
import bi.fris.common.TextExtractor
import com.github.nscala_time.time.Imports.DateTime
import TopicProtocol._
import akka.stream.ActorMaterializer



object TopicsConsumer extends ExtensionKey[TopicsConsumer]

class TopicsConsumer (protected val system: ExtendedActorSystem) extends Extension with Sharding {
  
   implicit val materializer = ActorMaterializer()(system)
    def consume(source: Source[TopicEvent, Unit])(implicit sender: ActorRef) {
    source.runForeach { x =>
      tellEntry("Topics", x)
    }
  }

  override protected def typeName: String = "TopicConsumer"
  override protected def props: Props = TopicConsumer.props
} 

class TopicConsumer extends TopicRepository with TextExtractor with Actor with ActorLogging {
  override def receive: Receive = {
    case TopicCreated(topic) => {
      createTopic(topic)
      extractHashtags(topic.title) match {
        case tags @ (head :: _) => addTags(topic.id, tags, topic.author.username)
        case _                  => 
      }
    }
    case TopicDeleted(id, update_at)                => deleteTopic(id, update_at)
    case ParticipantJoined(id, username, timestamp) => addParticipant(id, username, timestamp)
    case ParticipantLeft(id, username, timestamp)   => removeParticipant(id, username, timestamp)
    case BackgroundUpdated(id, url, timestamp)      => updateBackground(id, url, timestamp)
  }

}


object TopicConsumer {
  def props = Props(new TopicConsumer())  
}

object TopicView extends ExtensionKey[TopicView]

class TopicView (protected val system: ExtendedActorSystem) extends Extension with TopicRepository {

  def topic(id:String, username:Option[String]=None)(implicit ec: ExecutionContext):Future[Option[Topic]]={
    findTopic(id, username)
  }
  def userTopics(username:String, count:Option[Int]=Some(20), since:Option[Long]=Some(DateTime.now.getMillis), secret:Boolean=false)(implicit ec: ExecutionContext):Future[List[Topic]] = {
      findUserTopics(username, count.getOrElse(20), since.getOrElse(DateTime.now.getMillis), secret)
  }
  def topics(count:Option[Int]=Some(20), since:Option[Long]=Some(DateTime.now.getMillis), order:Option[String]=Some("latest") )(implicit ec: ExecutionContext):Future[List[Topic]] = {
      findTopics(count.getOrElse(20), since.getOrElse(DateTime.now.getMillis), order.filterNot(_ == "").getOrElse("latest"))
  }
  
}