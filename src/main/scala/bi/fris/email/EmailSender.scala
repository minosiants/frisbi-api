package bi.fris
package email

import bi.fris.{ConfirmationToken, Settings}
import akka.actor.{ExtendedActorSystem, Extension, ExtensionKey}
import courier.{Envelope, Mailer, Multipart}
import courier.Defaults.executionContext
import javax.mail.internet.InternetAddress
import bi.fris.account.AccountProtocol.Profile

import scala.concurrent.Future

object EmailSender extends ExtensionKey[EmailSender]

class EmailSender(protected val system: ExtendedActorSystem) extends Extension with ConfirmationToken {
  val appSmtp = Settings(system).appSmtp
  lazy val mailer = Mailer(appSmtp.host, appSmtp.port).auth(true).as(appSmtp.username, appSmtp.password).startTtls(true)()

  def thnakYou(to: String) = {
    val settings = Settings(system).regTemplate
    val url = settings.url + confirmationToken(to)
    val body = settings.template.replace("|confirmation-url|", url)
    mailer(envelope(settings.subject, body)(settings.from)(to))
  }

  def ressetPassword(to: String, password: String) = {
    val settings = Settings(system).passwordRessetTemplate
    val url = settings.url + confirmationToken(to + "|" + password)
    val body = settings.template.replace("|password-reset-url|", url)
    mailer(envelope(settings.subject, body)(settings.from)(to))
  }

  def shareTopic(user: Profile, to: List[String], topicUrl: String, text: String) = user.email match {
    case Some(email) =>
      val settings = Settings(system).shareTemplate
      val body = settings.template.replace("|username|", user.username).replace("|topic-url|", topicUrl).replace("|text|", text)
      Future.sequence(to.map(envelope(settings.subject, body)(email)(_)).map(mailer(_)))
    case None => Future.successful(Unit)
  }

  private def envelope(subject: String, body: String)(from: String)(to: String) = {
    Envelope
      .from(new InternetAddress(from))
      .to(new InternetAddress(to))
      .subject(subject)
      .content(
        Multipart().html(body))
  }

  private def render(email: String, file: String, url: String): String = {
    val text = scala.io.Source.fromFile(uri(file), "utf-8").getLines.mkString
    val result = text.replace("|confirmation-url|", url)
    return result
  }

  private def uri(fileName: String) = {
    getClass().getClassLoader().getResource(fileName).toURI()
  }
} 


