package bi.fris
package account

import java.util.UUID

import com.github.nscala_time.time.Imports.DateTime
import bi.fris.JSONWebToken
import play.api.libs.json.Json

object AccountProtocol {

  sealed trait AccountCommand {
    def id: String
  }
  case class CreateAccount(
    id: String = UUID.randomUUID.toString,
    username: String,
    password: String,
    email: String,
    created_at: DateTime = DateTime.now,
    updated_at: DateTime = DateTime.now) extends AccountCommand

  case class CreateAccountFromTwitter(id:String , username:String, twitterId:String, twitterName:String, image:String, signedToken:String, created_at: DateTime = DateTime.now, updated_at: DateTime = DateTime.now) extends AccountCommand
  case class CreateAccountFromFacebook(id:String , username:String, fbId:String, fbName:String, image:String, signedToken:String, email:String, created_at: DateTime = DateTime.now, updated_at: DateTime = DateTime.now) extends AccountCommand
  case class DeleteAccount(id: String, username: String, password: String, updated_at: DateTime = DateTime.now) extends AccountCommand
  case class ConfirmAccount(id: String, username: String, updated_at: DateTime = DateTime.now) extends AccountCommand
  case class AddTwitterCredentials(id: String, id_str:String, screen_name: String, image:String, signedToken:String, updated_at: DateTime = DateTime.now) extends AccountCommand
  case class AddFacebookCredentials(id: String, id_str:String, screen_name: String, image:String, signedToken:String, email:String, updated_at: DateTime = DateTime.now) extends AccountCommand
  case class UpdateAvatar(id: String, username: String, uri:String, updated_at: DateTime = DateTime.now) extends AccountCommand
  case class UpdatePassword(id: String, username: String, password:String, updated_at: DateTime = DateTime.now) extends AccountCommand

  val AccountEventKey = "account-event"
  sealed trait AccountEvent 
  case class AccountCreated(id: String, username: String, password: String, email: String, created_at: DateTime, updated_at: DateTime) extends AccountEvent
  case class AccountFromTwitterCreated(id:String , username:String, twitterId:String, twitterName:String, image:String, signedToken:String, created_at: DateTime, updated_at: DateTime) extends AccountEvent
  case class AccountFromFacebookCreated(id:String , username:String, fbId:String, fbName:String, image:String, signedToken:String, email:String, created_at: DateTime, updated_at: DateTime) extends AccountEvent
  case class AccountDeleted(id: String, username: String, updated_at: DateTime) extends AccountEvent
  case class AccountConfirmed(username: String, updated_at: DateTime) extends AccountEvent
  case class TwitterCredentialsAdded(id: String, id_str:String, screen_name: String, image:String, signedToken:String, updated_at: DateTime) extends AccountEvent
  case class FacebookCredentialsAdded(id: String, id_str:String, screen_name: String, image:String, signedToken:String, email:String, updated_at: DateTime) extends AccountEvent
  case class AvatarUpdated(id: String, username: String, uri:String, updated_at: DateTime) extends AccountEvent
  case class PasswordUpdated(id: String, username: String, password:String, updated_at: DateTime) extends AccountEvent

  sealed trait AccountQuery
  case class RequestToken(username: String, password: String) extends AccountQuery
  case class GetProfile(username: String) extends AccountQuery  

  sealed trait AccountQueryResult
  case class Token(username: String, token: String) extends AccountQueryResult

  implicit val requestTokenFormat = Json.format[RequestToken]
  implicit val tokenFormat = Json.format[Token]

  case class Profile(
    id: String,
    username: String,
    email: Option[String],
    confirmed: Boolean,
    created_at: DateTime,
    updated_at: DateTime,
    avatar: Option[String] = None,
    twitter:Option[String] = None,
    facebook:Option[String] = None,
    topics_count: Long = 0)

  implicit val userProfileFormat = Json.format[Profile]

  case class User(username: String, email: Option[String], created_at: DateTime)
  implicit val userFormat = Json.format[User]
}


