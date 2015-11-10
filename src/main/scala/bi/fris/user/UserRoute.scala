package bi.fris
package user

import akka.actor.Actor
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import bi.fris.TokenHttpAuthenticationDirectives.TokenAuth._
import bi.fris.account.AccountProtocol.Profile
import bi.fris.account.AccountView
import bi.fris.topic.TopicProtocol.Topic
import bi.fris.topic.TopicView
import bi.fris.user.UserRoute.UserData
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import play.api.libs.json.Json

import scala.async.Async._
import scala.concurrent.ExecutionContext

case class SignInInfo(username: String, password: String)
object SignInInfo {
  implicit val SignInInfoFormat = Json.format[SignInInfo]
}
object UserRoute {
  case class UserData(profile: Profile, topics: List[Topic])
  implicit val userDataFormat = Json.format[UserData]
}
// POST  curl 'http://localhost:8080/users/signup' -H 'Pragma: no-cache' -H 'Origin: chrome-extension://cokgbflfommojglbmbpenpphppikmonn' -H 'Accept-Encoding: gzip,deflate,sdch' -H 'Accept-Language: en-US,en;q=0.8,ru;q=0.6' -H 'User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2024.2 Safari/537.36' -H 'Content-Type: application/json' -H 'Accept: application/json' -H 'Cache-Control: no-cache' -H 'Connection: keep-alive' --data-binary '{"username":"jonathan2","password":"test123","email":"jonathan@kd@com"}' --compressed
//curl 'http://localhost:8080/users/' -X PUT -H 'Pragma: no-cache' -H 'Origin: chrome-extension://cokgbflfommojglbmbpenpphppikmonn' -H 'Accept-Encoding: gzip,deflate,sdch' -H 'Accept-Language: en-US,en;q=0.8,ru;q=0.6' -H 'User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2024.2 Safari/537.36' -H 'Content-Type: application/json' -H 'Accept: */*' -H 'Cache-Control: no-cache' -H 'Connection: keep-alive' --data-binary '{"username":"jonathan2","password":"test123","email":"jonathan@kd@com"}' --compressed

trait UserRoute extends CORSDirectives{
  this: Actor =>



  def userRoute(implicit ec: ExecutionContext, mat: ActorMaterializer, askTimeout: Timeout ) = {
    respondWithCors {
      pathPrefix("users") {
        path("show") {
          optionsForCors ~ get {
            parameters('username) { username =>
              tokenAuthO(ec) { user =>
                val secret = user.exists(_.username == username.toLowerCase)
                onSuccess(show(username.toLowerCase, secret)) {
                  case Some(userData) => complete(userData)
                  case None => complete(NotFound)
                }
              }
            }
            }
          }
        }
    }
  }

  private def show(username: String, secret:Boolean)(implicit ec: ExecutionContext) = async {
    val topics = await(TopicView(context.system).userTopics(username=username, secret=secret))
    for {
      profile <- await(AccountView(context.system).profile(username))
    } yield UserData(profile, topics)
  }
}