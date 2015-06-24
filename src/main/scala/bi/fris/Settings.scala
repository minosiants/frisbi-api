package bi.fris

import akka.actor.{ Actor, ExtendedActorSystem, Extension, ExtensionKey }
import scala.concurrent.duration.{ FiniteDuration, MILLISECONDS }

object Settings extends ExtensionKey[Settings]

class Settings(system: ExtendedActorSystem) extends Extension {
  object httpService {

    val interface: String =
      frisbi.getString("http-service.interface")

    val port: Int =
      frisbi.getInt("http-service.port")

    val askTimeout: FiniteDuration =
      FiniteDuration(frisbi.getDuration("http-service.ask-timeout", MILLISECONDS), MILLISECONDS)
  }

  object appRepository {

    val readTimeout: FiniteDuration =
      FiniteDuration(frisbi.getDuration("repository.read-timeout", MILLISECONDS), MILLISECONDS)

    val writeTimeout: FiniteDuration =
      FiniteDuration(frisbi.getDuration("repository.write-timeout", MILLISECONDS), MILLISECONDS)
  }

  object appSharding {
    val shardCount: Int =
      frisbi.getInt("sharding.shard-count")
  }
  
  object appSmtp {
	  val host: String = frisbi.getString("smtp.host")
    val port: Int = frisbi.getInt("smtp.port")
    val username: String = frisbi.getString("smtp.username")
    val password: String = frisbi.getString("smtp.password")
  }
  
  
  object regTemplate {
    val template= fromFile(frisbi.getString("emails.registration.template"))
    val from= frisbi.getString("emails.registration.from")
    val subject= frisbi.getString("emails.registration.subject")
    val url = frisbi.getString("emails.registration.confirmation")
  }
  object passwordRessetTemplate {
    val template= fromFile(frisbi.getString("emails.ressetPassword.template"))
    val from= frisbi.getString("emails.ressetPassword.from")
    val subject= frisbi.getString("emails.ressetPassword.subject")
    val url = frisbi.getString("emails.ressetPassword.confirmation")
  }
  object shareTemplate {
    val template = fromFile(frisbi.getString("emails.share.template"))
    val subject= frisbi.getString("emails.share.subject")
  }
  object twitter {
    val customerId = frisbi.getString("twitter.customerId")
    val customerSecret = frisbi.getString("twitter.customerSecret")
    val auth_callback =   frisbi.getString("twitter.auth_callback")
    val verify_credentials = frisbi.getString("twitter.verify_credentials")
    val status_update  = frisbi.getString("twitter.status_update")
  }

  object facebook {
    val customerId = frisbi.getString("facebook.customerId")
    val customerSecret = frisbi.getString("facebook.customerSecret")
    val auth_callback =   frisbi.getString("facebook.auth_callback")
    val graph_me =   frisbi.getString("facebook.graph_me")
    val graph =   frisbi.getString("facebook.graph")

  }

  object aws {
    val key = frisbi.getString("aws.access-key-id")
    val secret = frisbi.getString("aws.secret-access-key")
    val bucket = frisbi.getString("aws.bucket")
    val baseUrl = frisbi.getString("aws.baseUrl")
  }

  private val frisbi = system.settings.config.getConfig("frisbi")
  private def fromFile(file:String)={
    scala.io.Source.fromFile(uri(file), "utf-8").getLines.mkString
  }
  private def uri(fileName: String) = {
    getClass().getClassLoader().getResource(fileName).toURI()
  }



}

trait SettingsActor {
  this: Actor =>

  val settings: Settings =
    Settings(context.system)
}
