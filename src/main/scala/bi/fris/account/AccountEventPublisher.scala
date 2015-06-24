package bi.fris
package account

import akka.actor.{ ActorLogging, Props }
import akka.contrib.pattern.{ DistributedPubSubExtension, DistributedPubSubMediator }
import akka.stream.actor.{ ActorPublisher, ActorPublisherMessage }
import AccountProtocol._

object AccountEventPublisher {
  def props: Props = Props(new AccountEventPublisher)
}

class AccountEventPublisher extends ActorPublisher[AccountEvent] with ActorLogging {

  private val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(AccountEventKey, self)
  log.debug("Subscribed to AccountEvent events")

  override def receive: Receive = {
    case event: AccountEvent if isActive && totalDemand > 0 => sourceEvent(event)
    case event: AccountEvent                                => log.warning("Can't source event [{}]", event)
    case ActorPublisherMessage.Cancel                       => context.stop(self)
  }

  private def sourceEvent(event: AccountEvent): Unit = {
    onNext(event)
    log.debug("Sourced event [{}]", event)
  }
}