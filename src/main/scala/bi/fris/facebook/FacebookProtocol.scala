package bi.fris
package facebook

import bi.fris.account.AccountProtocol.{Token, Profile}
import bi.fris.common.ErrorMessage
import de.heikoseeberger.akkasse.ServerSentEvent
import play.api.libs.json.Json

object FacebookProtocol {

  case class GetAuthUrl(user: Option[Profile])
  case class AuthCallback(verifier: String)

  case class FacebookUser(id:String, email:String,  name:String, picture:Option[String], token:Option[String], secret:Option[String])

  case class FacebookRequestError(id:String = "fb_request_error", message:String="Facebook request error.") extends ErrorMessage
  case class UnableVerifyAccessToken(id:String = "fb_unable_verify_access_token", message:String="Facebook unable verify access token.", exception:Option[Throwable]=None) extends ErrorMessage
  sealed trait FacebookAuthEvent
  case class AuthUrlObtained(url:String) extends FacebookAuthEvent
  case class CredentialsSaved(token:Token) extends FacebookAuthEvent
  case class AccountCreated(token:Token) extends FacebookAuthEvent
  case class UsernameIsTaken(msg:String) extends FacebookAuthEvent
  case class EventError(msg:String) extends FacebookAuthEvent


  implicit val authUrlObtainedFormat = Json.format[AuthUrlObtained]
  implicit val accountCreatedFormat = Json.format[AccountCreated]
  implicit val credentialsSavedFormat = Json.format[CredentialsSaved]
  implicit val usernameIsTakenFormat = Json.format[UsernameIsTaken]
  implicit val eventErrorFormat = Json.format[EventError]
  implicit val facebookUserFormat = Json.format[FacebookUser]

  def flowEventToServerSentEvent(event: FacebookAuthEvent): ServerSentEvent =
    event match {
      case url:AuthUrlObtained =>
        ServerSentEvent(Json.toJson(url).toString(), "fb-auth-url")
      case error:UsernameIsTaken =>
        ServerSentEvent(Json.toJson(error).toString(), "fb-auth-username-taken")
      case created:AccountCreated =>
        ServerSentEvent(Json.toJson(created).toString(), "fb-auth-account-created")
      case saved:CredentialsSaved =>
        ServerSentEvent(Json.toJson(saved).toString(), "fb-auth-credentials-saved")
      case error:EventError =>
        ServerSentEvent(Json.toJson(error).toString(), "fb-auth-error")
      case _ => ServerSentEvent("unsupported event", "error")
    }
}

