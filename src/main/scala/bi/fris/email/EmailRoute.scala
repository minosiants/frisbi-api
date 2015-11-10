package bi.fris
package email

import akka.actor.Actor
import akka.stream.ActorMaterializer
import TokenHttpAuthenticationDirectives.TokenAuth._
import play.api.libs.json.Json
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._

import bi.fris.common.Validation

import scala.concurrent.ExecutionContext
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

object EmailRoute {

  case class ShareTopicReq(to: List[String], topicUrl: String, text: String) extends Validation {
    require(validText(text, 200), "text is invalid")
  }

  implicit val shareTopicReqFormat = Json.format[ShareTopicReq]
}

trait EmailRoute extends CORSDirectives {
  this: Actor =>

  import EmailRoute._
  import context.dispatcher

  def emailRoute(implicit ec: ExecutionContext, mat: ActorMaterializer) = {
    respondWithCors {
      path("email" / "share" / "topic") {
        optionsForCors ~
          post {
            tokenAuth(ec) { user =>
              entity(as[ShareTopicReq]) { req =>
                onSuccess(EmailSender(context.system).shareTopic(user, req.to, req.topicUrl, req.text)) {
                  case _ => complete(OK)
                }
              }
            }
          }
      }
    }
  }
}