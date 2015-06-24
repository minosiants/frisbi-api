package bi.fris
package facebook

import akka.actor.{ExtendedActorSystem, Extension, ExtensionKey}
import bi.fris.common.ErrorMessage
import org.scribe.builder.ServiceBuilder
import org.scribe.builder.api.FacebookApi
import org.scribe.model.{OAuthRequest, Token, Verb, Verifier}
import org.scribe.oauth.OAuthService
import play.api.libs.json.{Json, Reads}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


object FacebookClient extends ExtensionKey[FacebookClient]

class FacebookClient (protected val system: ExtendedActorSystem) extends Extension {

  private val facebookSetting = Settings(system).facebook


  import FacebookProtocol._

  def authorizationUrl(callback:String):String = {
    oauthService(Some(callback)){ service =>
      service.getAuthorizationUrl(null)
    }
  }

  def verifyCredentials(callback:String, verifier: String)(implicit ec: ExecutionContext): Future[Either[ErrorMessage, FacebookUser]] = Future {
    oauthService(Some(callback)) { service =>
      val graphMe = requestFb[FacebookUser](service)(Verb.GET)(facebookSetting.graph_me)_
      for {
        token <- accessToken(service)(verifier).right
        user <- graphMe(token).right
      } yield user.copy(picture = Some(s"${facebookSetting.graph}/${user.id}/picture"), token=Some(token.getToken), secret = Some(token.getSecret))
    }
  }
  private def accessToken(service:OAuthService)(verifier: String):Either[ErrorMessage, Token]={
    Try(service.getAccessToken(null, new Verifier(verifier))) match {
      case Success(v) => Right(v)
      case Failure(e) => Left(UnableVerifyAccessToken(exception = Some(e)))
    }
  }
  private def requestFb[A](service:OAuthService)(verb:Verb)(url:String)(at:Token)(implicit reads: Reads[A]):Either[ErrorMessage, A] ={
    val request = new OAuthRequest(verb, url)
    service.signRequest(at, request)
    val response = request.send()
    response.getCode match {
      case 200 =>
        println(response.getBody)
        reads.reads(Json.parse(response.getBody)).asOpt match {
          case Some(a) => Right(a)
          case None =>  Left(FacebookRequestError(message = "Unable parece json"))
        }
      case x => {
        Left(FacebookRequestError(message = response.getBody))
      }
    }
  }

  private def oauthService[T](callback: Option[String] = None)(f: OAuthService => T): T = {
    val b = new ServiceBuilder()
      .provider(classOf[FacebookApi])
      .apiKey(facebookSetting.customerId)
      .apiSecret(facebookSetting.customerSecret)
      .scope("email")
    for {
      cb <- callback
    } yield b.callback(cb)
    f(b.build())
  }
}
