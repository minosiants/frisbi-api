package bi.fris
package common

import play.api.libs.json._
import play.api.libs.functional.syntax._
import akka.http.scaladsl.model.StatusCodes._
import play.api.libs.json.Json.toJsFieldJsValueWrapper

trait Message
trait EmptyMessage extends Message

trait ErrorMessage extends Throwable {
  def id: String
  def message: String
  def url: String=""
}
case class InvalidToken(id:String = "invalid_token", message:String="Invalid username or password.") extends ErrorMessage
case class UnableCreateToken(id:String = "unable_create_token", message:String="Unable to create token.") extends ErrorMessage
case class AccountExistsAlready(id:String = "account_exists_already", message:String="Username exists already. Please try another one.", data:Option[String]=None) extends ErrorMessage
case class UnableSaveAvatar(id:String = "unable_save_avatar", message:String="Unable to save your profile picture. Please try again.") extends ErrorMessage
case class UnableSaveBackground(id:String = "unable_save_background", message:String="Unable to save topic background. Please try again.") extends ErrorMessage
case class UnableFindUser(id:String = "unable_find_user", message:String="Unable to find user.") extends ErrorMessage
case class UnableFindEmail(id:String = "unable_find_email", message:String="Unable to find email.") extends ErrorMessage
case class UnableFindTopic(id:String = "unable_find_topic", message:String="Unable to find topic.") extends ErrorMessage
case class UnableSendOffer(id:String = "unable_send_offer", message:String="Unable to to send offer.") extends ErrorMessage
case class UnableSendAnswer(id:String = "unable_send_answer", message:String="Unable to to send answer.") extends ErrorMessage
case class UnableSendCandidate(id:String = "unable_send_Candidate", message:String="Unable to to send candidate.") extends ErrorMessage
case class UnableJoin(id:String = "unable_join", message:String="Unable to to join topic.") extends ErrorMessage

object ErrorMessage {
  implicit val errorMessageFormat = new Writes[ErrorMessage] {
    def writes(error: ErrorMessage) = Json.obj(
      "id" -> error.id,
      "message" -> error.message,
      "url" -> error.url
      )
  }
  val Invalid_Token = BadRequest -> InvalidToken()
  val Unable_Create_Token = Conflict -> UnableCreateToken()
  val Account_Exists_Already = Conflict -> AccountExistsAlready()
  val Unable_Save_Avatar = BadRequest -> UnableSaveAvatar()
  val Unable_Save_Background = BadRequest -> UnableSaveBackground()
  val Unable_Find_User = NotFound -> UnableFindUser()
  val Unable_Find_Email = NotFound -> UnableFindEmail()
  val Unable_Find_Topic = NotFound -> UnableFindTopic()
  val Unable_Send_Offer = BadRequest -> UnableSendOffer()
  val Unable_Send_Answer = BadRequest -> UnableSendAnswer()
  val Unable_Send_Candidate = BadRequest -> UnableSendCandidate()
  val Unable_Join = BadRequest -> UnableJoin()



}