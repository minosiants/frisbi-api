package bi.fris

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, SupervisorStrategy, Terminated}
import akka.event.LoggingAdapter
import bi.fris.account.{Accounts, AccountsConsumer}
import bi.fris.signalling.Peers
import bi.fris.topic.{Topics, TopicsConsumer}

object FrisbiApp extends BaseApp[Unit] {
  override def run(system: ActorSystem, log: LoggingAdapter): Unit = {
    Accounts(system).start()    
    AccountsConsumer(system).start()
    Topics(system).start()
    TopicsConsumer(system).start()
    
    implicit val sender: ActorRef = system.actorOf(EmptyLoger.props)    
    AccountsConsumer(system).consume(Accounts(system).eventsSource())
    TopicsConsumer(system).consume(Topics(system).eventsSource())
    
    system.actorOf(HttpServerSup.props, "http-server-sup")
  }
}

object HttpServerSup {
  def props = Props(new HttpServerSup())
}
class HttpServerSup extends Actor with ActorLogging with SettingsActor {

  override val supervisorStrategy: SupervisorStrategy =
    SupervisorStrategy.stoppingStrategy

  context.watch(createHttpService())
  override def receive: Receive = {
    case Terminated(ref) =>
      log.warning("Shutting down, because {} has terminated!", ref.path)
      context.system.shutdown()
  }
  protected def createHttpService(): ActorRef = {
    import settings.httpService._
    context.actorOf(HttpService.props(interface, port, askTimeout), "http-service")
  }
}

class EmptyLoger extends Actor with ActorLogging {

  override def receive: Receive = {
    case _ => println("..........")
  }

}

object EmptyLoger {
  def props = Props(new EmptyLoger())

}