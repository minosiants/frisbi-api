package bi.fris
package twitter

import java.util.Date

import akka.actor._
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.Directives._
import akka.stream.FlowMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import akka.util.Timeout
import bi.fris.TokenHttpAuthenticationDirectives.TokenAuth._
import bi.fris.account.AccountProtocol._
import bi.fris.account.{AccountView, Accounts}
import bi.fris.twitter.TwitterProtocol._
import de.heikoseeberger.akkahttpjsonplay.PlayJsonMarshalling._
import de.heikoseeberger.akkasse.{EventPublisher, EventStreamMarshalling, ServerSentEvent}
import play.api.libs.json._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait TwitterRoute extends CORSDirectives with EventStreamMarshalling with Signature {
  this: Actor =>

  import common.Html._

  private val twitterSetting = Settings(context.system).twitter
  private val mediator = DistributedPubSubExtension(context.system).mediator
  private val twitterClient = TwitterClient(context.system)
  private val accounts = Accounts(context.system)
  private val accountView = AccountView(context.system)

  case class AuthResponce(userId: Option[String], username: Option[String], secret: String)

  case class StatusUpdate(status: String, str: Option[String])

  implicit val authResponceFormat = Json.format[AuthResponce]
  implicit val statusUpdateFormat = Json.format[StatusUpdate]


  def twitterRoute(implicit ec: ExecutionContext, mat: FlowMaterializer, askTimeout: Timeout) = {
    respondWithCors {
      path("twitter" / "authorization") {
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
                Source(ActorPublisher[ServerSentEvent](
                  context.actorOf(TwitterEventPublisher.props(mediator, twitterClient, accounts, accountView, sessionId)))
                )
              }
            }
          }
      } ~
        path("twitter" / "authorization" / "callback") {
          get {
            parameters('oauth_token, 'oauth_verifier, 'sessionId) { (oauth_token, oauth_verifier, sessionId) =>
              mediator ! DistributedPubSubMediator.Publish(sessionId, AuthCallback(oauth_token, oauth_verifier))
              complete(windowClose)
            }
          }
        } ~
        path("twitter" / "status") {
          optionsForCors ~
            post {
              entity(as[StatusUpdate]) { req =>
                tokenAuth(ec) { user =>
                  val result = (for {

                    signedToken <- user.twitter
                    tokenParts <- extract(signedToken)
                    token <- tokenParts.headOption
                    secret <- tokenParts.tail.headOption
                  } yield TwitterClient(context.system).postStatus(token, secret, req.status)).getOrElse(Future.successful(None))
                  onSuccess(result) {
                    case _ => complete(OK)
                  }
                }
              }
            }
        }
    }
  }

}

class TwitterEventPublisher(mediator: ActorRef, twitter: TwitterClient, accounts: Accounts, accountView: AccountView, sessionId: String)
  extends EventPublisher[TwitterAuthEvent](100, 5 seconds) with ActorLogging {

  import TwitterProtocol._

  val twitterSetting = Settings(context.system).twitter
  var user: Option[Profile] = None
  var requestToken: Option[(String, String)] = None
  implicit val ec = context.dispatcher
  implicit val askTimeout: Timeout = 5.seconds

  mediator ! DistributedPubSubMediator.Subscribe(sessionId, self)

  override def postStop(): Unit = {
    mediator ! DistributedPubSubMediator.Unsubscribe(sessionId, self)
  }

  override def receiveEvent = {
    case event: TwitterAuthEvent => onEvent(event)

    case GetAuthUrl(profile) =>
      user = profile
      twitter.requestToken(s"${twitterSetting.auth_callback}?sessionId=$sessionId").onComplete {
        case Success((t, url)) =>
          requestToken = Some((t.getToken, t.getSecret))
          onEvent(AuthUrlObtained(url))
        case Failure(f) =>
          onEvent(EventError(f.getMessage))
      }
    case AuthCallback(token, verifier) =>
      val secret = requestToken.get._2
      twitter.verifyCredentials(token, secret, verifier).flatMap {
        case Left(f) => Future.successful(EventError(f.getMessage))
        case Right(twitterUser) => saveOrCreate(twitterUser).mapTo[TwitterAuthEvent]
      }.onComplete {
        case Success(event) => onEvent(event)
        case Failure(f) =>
          log.error(s"unable to verify credentials ${f.getMessage}")
          onEvent(EventError("unable to verify credentials"))
      }

  }

  private def saveOrCreate(twitterUser: TwitterUser) =
    accountView.profileByTwitter(twitterUser.id_str).flatMap {
      case None => createAccount(twitterUser.screen_name, twitterUser)
      case Some(profile) => accounts.addTwitter(profile.id, twitterUser.id_str, twitterUser.screen_name, twitterUser.profile_image_url_https, twitterUser.token.get, twitterUser.secret.get)
        .map{a => profile.username}
        .map(accountView.createToken)
        .map(CredentialsSaved)
    }

  private def createAccount(username: String, twitterUser: TwitterUser):Future[TwitterAuthEvent] =
    accountView.profile(username).flatMap{
      case Some(profile) => Future.successful(UsernameIsTaken(Json.toJson(twitterUser).toString()))
      case None =>
        accounts
          .createAccountFromTwitter(username, twitterUser.id_str, twitterUser.screen_name, twitterUser.profile_image_url_https, twitterUser.token.get, twitterUser.secret.get)
          .map(_.username)
          .map(accountView.createToken)
          .map(AccountCreated)


    }
}

object TwitterEventPublisher {
  type TwitterToken = (String, String)

  case class TwitterAuthSession(sessionId: String, ssePublisher: ActorRef, user: Option[Profile] = None, token: Option[TwitterToken] = None)

  def props(mediator: ActorRef, twitterClient: TwitterClient, accounts: Accounts, accountView: AccountView, sessionId: String) =
    Props(new TwitterEventPublisher(mediator, twitterClient, accounts, accountView, sessionId))
}