package bi.fris
package topic

import akka.actor.{ ActorLogging, Props }
import akka.contrib.pattern.{ DistributedPubSubExtension, DistributedPubSubMediator }
import akka.stream.actor.{ ActorPublisher, ActorPublisherMessage }
import TopicProtocol._

object TopicEventPublisher {
  def props: Props = Props(new TopicEventPublisher)
}

class TopicEventPublisher extends ActorPublisher[TopicEvent] with ActorLogging {

  private val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(TopicEventKey, self)
  log.debug("Subscribed to TopicEvent events")

  override def receive: Receive = {
    case event: TopicEvent if isActive && totalDemand > 0 => sourceEvent(event)
    case event: TopicEvent                                => log.warning("Can't source event [{}]", event)
    case ActorPublisherMessage.Cancel                       => context.stop(self)
  }

  private def sourceEvent(event: TopicEvent): Unit = {
    onNext(event)
    log.debug("Sourced event [{}]", event)
  }
}