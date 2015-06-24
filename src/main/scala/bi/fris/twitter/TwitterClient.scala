package bi.fris
package twitter

import akka.actor.{ExtendedActorSystem, Extension, ExtensionKey}
import bi.fris.common.ErrorMessage
import play.api.libs.json.Json
import scala.async.Async.async
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import org.scribe.builder._
import org.scribe.builder.api._
import org.scribe.model._
import org.scribe.oauth._


object TwitterClient extends ExtensionKey[TwitterClient]

class TwitterClient(protected val system: ExtendedActorSystem) extends Extension {

  import TwitterProtocol._

  val twitterSetting = Settings(system).twitter
  val status_update = twitterSetting.status_update
  val verify_credentials = twitterSetting.verify_credentials

  def requestToken(callback:String)(implicit ec: ExecutionContext): Future[(Token, String)] = Future {
    oauthService(Some(callback)) { service =>
      val requestToken = service.getRequestToken
      (requestToken, service.getAuthorizationUrl(requestToken))
    }
  }

  def accessToken(token: String, secret: String, verifier: String)(implicit ec: ExecutionContext): Future[Token] = Future {
    oauthService() { service =>
      service.getAccessToken(new Token(token, secret), new Verifier(verifier))
    }
  }

  def verifyCredentials(token: String, secret: String, verifier: String)(implicit ec: ExecutionContext): Future[Either[ErrorMessage, TwitterUser]] = Future {
    oauthService() { service =>
      val at = service.getAccessToken(new Token(token, secret), new Verifier(verifier))
      val request = new OAuthRequest(Verb.GET, verify_credentials)
      service.signRequest(at, request)
      val response = request.send()
      response.getCode match {
        case 200 => Right(Json.fromJson[TwitterUser](Json.parse(response.getBody)).map(_.copy(token = Some(at.getToken), secret = Some(at.getSecret))) get)
        case x => {
          Left(UnableVerifyTwitterCredentials())
        }
      }
    }
  }

  def postStatus(token: String, secret: String, status: String)(implicit ec: ExecutionContext): Future[Either[Error, Int]] = Future {
    oauthService() { service =>
      val request = new OAuthRequest(Verb.POST, status_update)
      request.addBodyParameter("status", status)
      service.signRequest(new Token(token, secret), request)

      val response = request.send()
      response.getCode match {
        case 200 => Right(200)
        case x => {
          Left(new Error(response.getMessage))
        }
      }

    }
  }

  private def oauthService[T](callback: Option[String] = None)(f: OAuthService => T): T = {
    val b = new ServiceBuilder()
      .provider(classOf[TwitterApi])
      .apiKey(twitterSetting.customerId)
      .apiSecret(twitterSetting.customerSecret)
    for {
      cb <- callback
    } yield b.callback(cb)
    f(b.build())
  }

}