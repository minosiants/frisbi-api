package bi.fris
package account

import java.util.UUID

import akka.actor.Actor
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import bi.fris.aws.S3Client
import bi.fris.common._
import bi.fris.email.EmailSender
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import org.apache.commons.codec.binary.Base64
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

object AccountRoute {

  case class CreateAccountReq(username: String, password: String, email: String) extends Validation {
    require(validText(username), "username invalid")
    require(validText(password), "password invalid")
    require(validText(email) && validEmail(email), "email invalid")

  }

  case class UploadAvatarReq(data: String) {
    def decode() = {
      Base64.decodeBase64(data.substring(data.indexOf(',') + 1))
      //Files.copy(in, Paths.get("/home/kaspar/Desktop/ttt.png"), StandardCopyOption.REPLACE_EXISTING);
    }
  }

  case class ResetPasswordReq(username: String, password: String) extends Validation {
    require(validText(password), "password invalid")
  }

  implicit val createAccountReqFormat = Json.format[CreateAccountReq]
  implicit val uploadAvatarReqFormat = Json.format[UploadAvatarReq]
  implicit val resetPasswordReqFormat = Json.format[ResetPasswordReq]
}

import TokenHttpAuthenticationDirectives.TokenAuth._

trait AccountRoute extends CORSDirectives with ConfirmationToken with Hashing {
  this: Actor =>

  import AccountProtocol._
  import AccountRoute._
  import ErrorMessage._

  def normolize(txt:String)=txt.trim.toLowerCase

  def accountRoute(implicit ec: ExecutionContext, mat: ActorMaterializer, askTimeout: Timeout) = {
    respondWithCors {
      pathPrefix("account") {
        path("verify_credentials") {
          optionsForCors ~
            get {
              tokenAuth(ec) { user =>
                onSuccess(AccountView(context.system).profile(normolize(user.username))) {
                  case Some(profile) => complete(profile)
                  case None => complete(BadRequest)
                }
              }
            }
        } ~
          path("confirmation") {
            optionsForCors ~
              get {
                parameters('token.as[String]) { token =>
                  onSuccess {
                    (for {
                      email <- FutureO(Future(decodeToken(token)))
                      profile <- FutureO(AccountView(context.system).profile(normolize(email)))
                    } yield Accounts(context.system).confirmAccount(profile.id, profile.username)).future
                  } {
                    case Some(a) => complete(OK)
                    case None => complete(BadRequest -> Some(InvalidToken()))
                  }
                }
              }
          } ~
          path("create") {
            optionsForCors ~
              post {
                entity(as[CreateAccountReq]) { req =>
                  onSuccess(AccountView(context.system).profile(normolize(req.username))) {
                    case Some(profile) => complete(Account_Exists_Already)
                    case None => Accounts(context.system).createAccount(normolize(req.username), req.password, normolize(req.email))
                      EmailSender(context.system).thnakYou(req.email)
                      complete(Created)
                  }
                }
              }

          } ~
          path("resetPassword") {
            optionsForCors ~
              post {
                entity(as[ResetPasswordReq]) { req =>
                  onSuccess(AccountView(context.system).profile(normolize(req.username))) {
                    case Some(profile) if profile.email.isEmpty => complete(Unable_Find_Email)
                    case Some(profile) => EmailSender(context.system).ressetPassword(profile.email.get, md5hash(req.password))
                      complete(OK)
                    case None =>
                      complete(Unable_Find_User)
                  }
                }
              } ~
              get {
                parameters('token.as[String]) { token =>
                  onSuccess((for {
                    a <- FutureO(Future(decodeResetPassword(decodeToken(token))))
                    profile <- FutureO(AccountView(context.system).profile(a._1))
                  } yield Accounts(context.system).updatePassword(profile.id, profile.username, a._2)).future) {
                    case Some(a) => complete(OK)
                    case None => complete(Invalid_Token)
                  }
                }
              }
          } ~
          path("token") {
            optionsForCors ~
              post {
                entity(as[RequestToken]) { cmd =>
                  onSuccess(AccountView(context.system).createToken(cmd.username, cmd.password)) {
                    case Some(token) => complete(OK -> token)
                    case None => complete(Invalid_Token)
                  }
                }
              }
          } ~
          path("avatar") {
            optionsForCors ~
              post {
                tokenAuth(ec) { user =>
                  entity(as[UploadAvatarReq]) { data =>
                    onSuccess(
                      for {
                        uri <- S3Client(context.system).upload(UUID.randomUUID().toString, data.decode())
                        avatarUpdated <- Accounts(context.system).updateAvatar(user.id, user.username, uri)
                      } yield avatarUpdated) {
                      case AvatarUpdated(_, _, uri, _) => complete(OK -> uri)
                      case _ => complete(Unable_Save_Avatar)
                    }
                  }
                }
              }
          }
      }
    }
  }

  def decodeResetPassword(str: Option[String]) = {
    str.map { t =>
      val parts = t.split("\\|")
      (parts(0), parts(1))
    }
  }
}

