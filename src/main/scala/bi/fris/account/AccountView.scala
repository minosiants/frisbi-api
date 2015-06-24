package bi.fris
package account

import scala.concurrent.{ExecutionContext, Future}

import bi.fris.{JSONWebToken, SettingsActor, Sharding}
import bi.fris.account.AccountProtocol._
import akka.actor.{Actor, ActorLogging, ActorRef, ExtendedActorSystem, Extension, ExtensionKey, Props}
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Source
import common.Hashing


object AccountsConsumer extends ExtensionKey[AccountsConsumer]

class AccountsConsumer(protected val system: ExtendedActorSystem) extends Extension with Sharding {

  implicit val materializer = ActorFlowMaterializer()(system)
  def consume(source: Source[AccountEvent, Unit])(implicit sender: ActorRef) {
    source.runForeach { x =>
      tellEntry("Accounts", x)
    }
  }

  override protected def typeName: String = "AccountsConsumer"
  override protected def props: Props = AccountConsumer.props
}

class AccountConsumer extends AccountRepository with Actor with ActorLogging with SettingsActor {
  import context.dispatcher

  def receive: Receive = {
    case AccountCreated(id, username, password, email, created_at, updated_at) =>
      createAccount(id, username, password, email, created_at, updated_at)
    case AccountConfirmed(username, updated_at) => 
      makeConfirmed(username, updated_at)
    case TwitterCredentialsAdded(id, id_str, screen_name, image, signedToken, updated_at) =>
      addTwitter(id, id_str, screen_name, image, signedToken, updated_at)
    case FacebookCredentialsAdded(id, id_str, screen_name, image, signedToken, email, updated_at) =>
      addFacebook(id, id_str, screen_name, image, signedToken, email, updated_at)
    case AvatarUpdated(id, username, uri, updated_at) =>
      updateAvatar(username, uri, updated_at)
    case PasswordUpdated(id, username, password, updated_at) =>
      updatePassword(username, password, updated_at)
    case AccountFromTwitterCreated(id, username, twitterId, twitterName, image, signedToken,created_at, updated_at) =>
      createAccountFromTwitter(id, username, twitterId, twitterName, image, signedToken, created_at, updated_at)
    case AccountFromFacebookCreated(id, username, fbId, fbName, image, signedToken, email, created_at, updated_at) =>
      createAccountFromFacebook(id, username, fbId, fbName, image, email, signedToken, created_at, updated_at)
    case _ => println(">>>>>>>>>>>>>>> unknown")
  }

}

object AccountConsumer {
  def props = Props(new AccountConsumer())
}

object AccountView extends ExtensionKey[AccountView]

class AccountView(protected val system: ExtendedActorSystem) extends Extension with JSONWebToken with AccountRepository with Hashing {
  import AccountProtocol._
  def createToken(username: String, password: String)(implicit ec: ExecutionContext): Future[Option[Token]] = {
    findUser(username, Some(md5hash(password))) map {
      case Some(user) => Some(Token(username, token(username)))
      case None => None
    }
  }
  def createToken(username: String):Token = {
    Token(username, token(username))
  }
  def profile(username: String)(implicit ec: ExecutionContext): Future[Option[Profile]] = {
    findProfile(username)
  }
  def profileByTwitter(twitterId:String)(implicit ec: ExecutionContext): Future[Option[Profile]] = {
    findProfileByTwitter(twitterId)
  }
  def profileByFacebook(fbId:String)(implicit ec: ExecutionContext): Future[Option[Profile]] = {
    findProfileByFacebook(fbId)
  }
  
}