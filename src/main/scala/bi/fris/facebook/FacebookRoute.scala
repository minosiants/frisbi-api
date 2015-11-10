package bi.fris.facebook

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.Timeout
import bi.fris.TokenHttpAuthenticationDirectives.TokenAuth._
import bi.fris.account.AccountProtocol.Profile
import bi.fris.account.{AccountView, Accounts}
import bi.fris.facebook.FacebookProtocol.{AuthCallback, AuthUrlObtained, FacebookAuthEvent, GetAuthUrl, _}
import bi.fris.{CORSDirectives, Settings, Signature, common}
import de.heikoseeberger.akkasse.{EventStreamMarshalling, WithHeartbeats}
import play.api.libs.json.Json

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait FacebookRoute extends CORSDirectives with Signature {
  this: Actor =>

  import common.Html._

  private val mediator = DistributedPubSub(context.system).mediator
  private val fb = FacebookClient(context.system)
  private val accounts = Accounts(context.system)
  private val accountView = AccountView(context.system)

  def facebookRoute(implicit ec: ExecutionContext, mat: ActorMaterializer, askTimeout: Timeout) = {
    import EventStreamMarshalling._
    respondWithCors {
      path("facebook" / "authorization") {
        optionsForCors ~
          post {
            tokenAuthO(ec) { user =>
              parameters('sessionId) { sessionId =>
                mediator ! DistributedPubSubMediator.Publish(sessionId, GetAuthUrl(user))
                complete(OK)
              }
            }
          } ~
          get {
            parameters('sessionId) { sessionId =>
              complete {
                Source.actorRef[FacebookAuthEvent](100, OverflowStrategy.dropHead)
                  .map(FacebookProtocol.flowEventToServerSentEvent)
                  .via(WithHeartbeats(5.second))
                  .mapMaterializedValue(source =>
                    context.actorOf(FacebookEventPublisher.props(mediator, fb, accounts, accountView, sessionId, source))
                  )
              }
            }
          }
      } ~
        path("facebook" / "authorization" / "callback") {
          get {
            parameters('code, 'sessionId) { (code, sessionId) =>
              mediator ! DistributedPubSubMediator.Publish(sessionId, AuthCallback(code))
              complete(windowClose)
            }
          }
        }
    }
  }
}


class FacebookEventPublisher(mediator: ActorRef, fb: FacebookClient, accounts: Accounts, accountView: AccountView, sessionId: String, sseActor: ActorRef)
  extends Actor with ActorLogging {

  val facebookSetting = Settings(context.system).facebook
  var user: Option[Profile] = None
  val callback = s"${facebookSetting.auth_callback}?sessionId=$sessionId"
  implicit val ec = context.dispatcher
  implicit val askTimeout: Timeout = 5.seconds

  mediator ! DistributedPubSubMediator.Subscribe(sessionId, self)

  override def receive: Receive = {
    case event: FacebookAuthEvent => sseActor ! event
    case GetAuthUrl(profile) =>
      user = profile
      sseActor ! AuthUrlObtained(fb.authorizationUrl(callback))
    case AuthCallback(verifier) =>
      fb.verifyCredentials(callback, verifier).flatMap {
        case Left(f) => Future.successful(EventError(f.getMessage))
        case Right(fbUser) => saveOrCreate(fbUser).mapTo[FacebookAuthEvent]
      }.onComplete {
        case Success(event) => sseActor ! event
        case Failure(f) =>
          log.error(s"unable to verify credentials ${f.getMessage}")
          sseActor ! EventError("unable to verify credentials")
      }
  }


  private def saveOrCreate(fbUser: FacebookUser) =
    accountView.profileByFacebook(fbUser.id).flatMap {
      case None => createAccount(toUsername(fbUser.name), fbUser)
      case Some(profile) =>
        accounts.addFacebook(profile.id, fbUser.id, fbUser.name, fbUser.picture.get, fbUser.token.get, fbUser.secret.get, fbUser.email)
          .map { a => profile.username }
          .map(accountView.createToken)
          .map(CredentialsSaved)
    }

  private def createAccount(username: String, fbUser: FacebookUser): Future[FacebookAuthEvent] =
    accountView.profile(username).flatMap {
      case Some(profile) => Future.successful(UsernameIsTaken(Json.toJson(fbUser).toString()))
      case None =>
        accounts
          .createAccountFromFacebook(username, fbUser.id, fbUser.name, fbUser.picture.get, fbUser.email, fbUser.token.get, fbUser.secret.get)
          .map(_.username)
          .map(accountView.createToken)
          .map(AccountCreated)


    }
  private def toUsername(name:String)=name.toLowerCase.trim.replaceAll("\\s", ".")
}

object FacebookEventPublisher {
  def props(mediator: ActorRef, fb: FacebookClient, accounts: Accounts, accountView: AccountView, sessionId: String, sseActor: ActorRef) =
    Props(new FacebookEventPublisher(mediator, fb, accounts, accountView, sessionId, sseActor: ActorRef))
}

