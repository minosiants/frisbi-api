package bi.fris
package account

import akka.actor.{ActorLogging, ActorRef, ExtendedActorSystem, Extension, ExtensionKey, Props, actorRef2Scala}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}

import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess}
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import akka.util.Timeout
import com.github.nscala_time.time.Imports.DateTime
import bi.fris.account.AccountProtocol._
import bi.fris.common.Hashing

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object Accounts extends ExtensionKey[Accounts]

class Accounts(protected val system: ExtendedActorSystem) extends Extension with Sharding with Hashing with Signature {

  def createAccount(username: String, password: String, email: String)(implicit timeout: Timeout, sender: ActorRef): Future[AccountCreated] = {
    val cmd = CreateAccount(username = username, password = md5hash(password), email = email)
    askEntry(cmd.id, cmd).mapTo[AccountCreated]
  }

  def createAccountFromTwitter(username: String, twitterId: String, twitterName: String, image: String, token: String, secret: String)(implicit timeout: Timeout, sender: ActorRef): Future[AccountFromTwitterCreated] = {
    val cmd = CreateAccountFromTwitter(id = "twitter-" + twitterId, username = username, twitterId = twitterId, twitterName = twitterName, image = image, signedToken = sign(List(token, secret)))
    askEntry(cmd.id, cmd).mapTo[AccountFromTwitterCreated]
  }

  def createAccountFromFacebook(username: String, fbId: String, fbName: String, image: String, email:String, token: String, secret: String)(implicit timeout: Timeout, sender: ActorRef): Future[AccountFromFacebookCreated] = {
    val cmd = CreateAccountFromFacebook(id = "fb-" + fbId, username = username, fbId = fbId, fbName = fbName, image = image, signedToken = sign(List(token, secret)), email)
    askEntry(cmd.id, cmd).mapTo[AccountFromFacebookCreated]
  }

  def confirmAccount(id: String, username: String)(implicit timeout: Timeout, sender: ActorRef): Future[AccountConfirmed] = {
    val cmd = ConfirmAccount(id = id, username = username)
    askEntry(cmd.id, cmd).mapTo[AccountConfirmed]
  }

  def addTwitter(id: String, id_str: String, screen_name: String, image: String, token: String, secret: String)(implicit timeout: Timeout, sender: ActorRef): Future[TwitterCredentialsAdded] = {
    val cmd = AddTwitterCredentials(id = id, id_str = id_str, screen_name = screen_name, image = image, signedToken = sign(List(token, secret)))
    askEntry(cmd.id, cmd).mapTo[TwitterCredentialsAdded]
  }

  def addFacebook(id: String, id_str: String, screen_name: String, image: String, token: String, secret: String, email: String)(implicit timeout: Timeout, sender: ActorRef): Future[FacebookCredentialsAdded] = {
    val cmd = AddFacebookCredentials(id = id, id_str = id_str, screen_name = screen_name, image = image, signedToken = sign(List(token, secret)), email)
    askEntry(cmd.id, cmd).mapTo[FacebookCredentialsAdded]
  }

  def updateAvatar(id: String, username: String, uri: String)(implicit timeout: Timeout, sender: ActorRef): Future[AvatarUpdated] = {
    val cmd = UpdateAvatar(id = id, username = username, uri = uri)
    askEntry(cmd.id, cmd).mapTo[AvatarUpdated]
  }

  def updatePassword(id: String, username: String, password: String)(implicit timeout: Timeout, sender: ActorRef): Future[PasswordUpdated] = {
    val cmd = UpdatePassword(id = id, username = username, password = password)
    askEntry(cmd.id, cmd).mapTo[PasswordUpdated]
  }


  def deleteAccount(id: String, username: String, password: String)(text: String)(implicit timeout: Timeout, sender: ActorRef): Future[AccountDeleted] =
    askEntry(id, DeleteAccount(id = id, username = username, password = md5hash(password))).mapTo[AccountDeleted]

  def eventsSource() = {
    Source(ActorPublisher[AccountEvent](system.actorOf(AccountEventPublisher.props)))
  }

  override protected def typeName: String = "AccountProcessor"

  override protected def props: Props = AccountPersistent.props()
}

class AccountPersistent extends PersistentActor with ActorLogging {

  import AccountPersistent._

  var state = AccountSate(Account.empty, deleted = false)

  override def persistenceId: String = self.path.parent.name + "-" + self.path.name

  private val mediator = DistributedPubSub(context.system).mediator

  context.setReceiveTimeout(2.minutes)

  private val handler = (sender: ActorRef) => (event: AccountEvent) => {
    state.updated(event)
    mediator ! DistributedPubSubMediator.Publish(AccountEventKey, event)
    sender ! event
  }

  override def receiveCommand = {
    case CreateAccount(id, username, password, email, created_at, updated_at) => //validate      
      persist(AccountCreated(id, username, password, email, created_at, updated_at))(handler(sender()))

    case ConfirmAccount(id, username, updated_at) => //validate      
      persist(AccountConfirmed(username, updated_at))(handler(sender()))

    case DeleteAccount(id, username, password, updated_at) =>
      persist(AccountDeleted(id, username, updated_at))(handler(sender()))

    case AddTwitterCredentials(id, id_str, screen_name, image, signedToken, updated_at) =>
      persist(TwitterCredentialsAdded(id, id_str, screen_name, image, signedToken, updated_at))(handler(sender()))

    case AddFacebookCredentials(id, id_str, screen_name, image, signedToken, email, updated_at) =>
      persist(FacebookCredentialsAdded(id, id_str, screen_name, image, signedToken, email, updated_at))(handler(sender()))

    case UpdateAvatar(id, username, uri, updated_at) =>
      persist(AvatarUpdated(id, username, uri, updated_at))(handler(sender()))

    case UpdatePassword(id, username, password, updated_at) =>
      persist(PasswordUpdated(id, username, password, updated_at))(handler(sender()))

    case CreateAccountFromTwitter(id, username, twitterId, twitterName, image, signedToken, created_at, updated_at) =>
      persist(AccountFromTwitterCreated(id, username, twitterId, twitterName, image, signedToken, created_at, updated_at))(handler(sender()))

    case CreateAccountFromFacebook(id, username, fbId, fbName, image, signedToken, email, created_at, updated_at) =>
      persist(AccountFromFacebookCreated(id, username, fbId, fbName, image, signedToken, email, created_at, updated_at))(handler(sender()))


    case "snap" =>
      saveSnapshot(state)
    case SaveSnapshotSuccess(md) =>
      println(s"snapshot saved (metadata = $md)")
    case SaveSnapshotFailure(md, e) =>
      println(s"snapshot saving failed (metadata = $md, error = ${e.getMessage})")


  }
  override def receiveRecover = {
    case evt: AccountEvent => {
      state = state.updated(evt)
    }
  }

}

object AccountPersistent {

  case class Account(id: String,
                     username: String,
                     password: Option[String],
                     email: Option[String],
                     created_at: DateTime,
                     updated_at: DateTime,
                     confirmed: Boolean,
                     avatar: Option[String] = None,
                     twitter: Option[TwitterAcc] = None,
                     facebook: Option[FacebookAcc] = None)

  case class TwitterAcc(id_str: String, screen_name: String, image: String, signedToken: String)

  case class FacebookAcc(id: String, email: String, name: String, image: String, signedToken: String)

  object Account {
    def empty = Account("", "", "", "", null, null, confirmed = false)

    def apply(id: String,
              username: String,
              password: String,
              email: String,
              created_at: DateTime,
              updated_at: DateTime,
              confirmed: Boolean) = new Account(id = id,
      username = username,
      password = Some(password),
      email = Some(email),
      confirmed = confirmed,
      created_at = created_at,
      updated_at = updated_at,
      avatar = None,
      twitter = None,
      facebook = None)
  }

  case class AccountSate(account: Account, deleted: Boolean = false) {
    def updated(evt: AccountEvent): AccountSate = evt match {
      case AccountCreated(id, username, password, email, created_at, updated_at) =>
        copy(account = Account(id, username, password, email, created_at, updated_at, confirmed = false))
      case AccountConfirmed(username, updated_at) =>
        copy(account = account.copy(confirmed = true))
      case AccountDeleted(id, username, time) => copy(account = account.copy(updated_at = time), deleted = true)
      case AvatarUpdated(id, username, uri, updated_at) => copy(account = account.copy(avatar = Some(uri), updated_at = updated_at), deleted = true)
      case PasswordUpdated(id, username, password, updated_at) => copy(account = account.copy(password = Some(password), updated_at = updated_at, confirmed = true))
      case TwitterCredentialsAdded(id, id_str, screen_name, image, signedToken, updated_at) =>
        copy(account = account.copy(updated_at = updated_at, twitter = Some(TwitterAcc(id_str, screen_name, image, signedToken))))
      case FacebookCredentialsAdded(id, id_str, screen_name, image, signedToken, email, updated_at) =>
        copy(account = account.copy(updated_at = updated_at, facebook = Some(FacebookAcc(id_str, email, screen_name, image, signedToken))))
      case AccountFromTwitterCreated(id, username, twitterId, twitterName, image, signedToken, created_at, updated_at) =>
        copy(account = Account(
          id = id,
          username = username,
          password = None,
          email = None,
          created_at = created_at,
          updated_at = updated_at,
          avatar = Some(image),
          twitter = Some(TwitterAcc(twitterId, twitterName, image, signedToken)),
          confirmed = false)
        )
      case AccountFromFacebookCreated(id, username, fbId, fbName, image, signedToken, email, created_at, updated_at) =>
        copy(account = Account(
          id = id,
          username = username,
          password = None,
          email = Some(email),
          created_at = created_at,
          updated_at = updated_at,
          avatar = Some(image),
          twitter = None,
          facebook = Some(FacebookAcc(fbId, email, fbName, image, signedToken)),
          confirmed = true)
        )
      case _ => this
    }
  }

  def props() = Props(new AccountPersistent())

}