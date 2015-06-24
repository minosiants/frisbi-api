package bi.fris

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.pattern.pipe
import akka.stream.scaladsl.ImplicitFlowMaterializer
import akka.util.Timeout
import bi.fris.account.AccountRoute
import bi.fris.email.EmailRoute
import bi.fris.facebook.FacebookRoute
import bi.fris.signalling.{Peers, TopicSignallingRoute}
import bi.fris.topic.TopicsRoute
import bi.fris.twitter.TwitterRoute
import bi.fris.user.UserRoute

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.DurationInt

class HttpService(interface: String, port: Int)
  extends Actor with ActorLogging with SettingsActor with ImplicitFlowMaterializer with UserRoute
  with TopicsRoute
  with AccountRoute
  with EmailRoute
  with TopicSignallingRoute
  with TwitterRoute
  with FacebookRoute {

  import context.dispatcher
  implicit val askTimeout: Timeout = 1.minute

  serveHttp()

  override def receive = {
    case Http.ServerBinding(address) => log.info("Listening on {}", address)
  //  case Status.Failure(_)           => context.stop(self)
    //case Shutdown                    => context.stop(self)
  }

  protected def serveHttp(): Unit = {
    val peers = context.actorOf(Peers.props, "peers")
    Http(context.system)
      .bindAndHandle(accountRoute ~ topicsRoute ~ topicSignallingRoute(peers) ~ topicSignallingSseRoute(peers) ~ topicsStreamingRoute ~ userRoute ~ emailRoute ~ twitterRoute ~ facebookRoute, interface, port)
      .pipeTo(self)
  }

}


object HttpService {
  def props(interface: String, port: Int, askTimeout: FiniteDuration) = Props(new HttpService(interface, port))
}
