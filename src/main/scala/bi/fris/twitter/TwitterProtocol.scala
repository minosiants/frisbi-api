package bi.fris
package twitter

import bi.fris.account.AccountProtocol._
import bi.fris.common.ErrorMessage
import de.heikoseeberger.akkasse.ServerSentEvent
import play.api.libs.json._

object TwitterProtocol {
  case class UnableVerifyTwitterCredentials(id:String = "unable_verify_twitter_credentials", message:String="Unable to verify twitter credentials.") extends ErrorMessage

  case class GetAuthUrl(user: Option[Profile])
  case class AuthCallback(token: String, verifier: String)


  case class TwitterUser(id_str:String, screen_name:String, profile_image_url_https:String, token:Option[String], secret:Option[String])
  implicit val twitterUserFormat = Json.format[TwitterUser]

  sealed trait TwitterAuthEvent
  case class AuthUrlObtained(url:String) extends TwitterAuthEvent
  case class CredentialsSaved(token:Token) extends TwitterAuthEvent
  case class AccountCreated(token:Token) extends TwitterAuthEvent
  case class UsernameIsTaken(msg:String) extends TwitterAuthEvent
  case class EventError(msg:String) extends TwitterAuthEvent

  implicit val twitterAuthUrlObtainedFormat = Json.format[AuthUrlObtained]
  implicit val accountCreatedFormat = Json.format[AccountCreated]
  implicit val credentialsSavedFormat = Json.format[CredentialsSaved]
  implicit val usernameIsTakenFormat = Json.format[UsernameIsTaken]
  implicit val eventErrorFormat = Json.format[EventError]

  implicit def flowEventToServerSentEvent(event: TwitterAuthEvent): ServerSentEvent =
    event match {
      case url:AuthUrlObtained =>
        ServerSentEvent(Json.toJson(url).toString(), "twitter-auth-url")
      case error:UsernameIsTaken =>
        ServerSentEvent(Json.toJson(error).toString(), "twitter-auth-username-taken")
      case created:AccountCreated =>
        ServerSentEvent(Json.toJson(created).toString(), "twitter-auth-account-created")
      case saved:CredentialsSaved =>
        ServerSentEvent(Json.toJson(saved).toString(), "twitter-auth-credentials-saved")
      case error:EventError =>
        ServerSentEvent(Json.toJson(error).toString(), "twitter-auth-error")
      case _ => ServerSentEvent("unsupported event", "error")
    }

}
