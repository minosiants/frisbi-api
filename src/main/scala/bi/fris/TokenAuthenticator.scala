package bi.fris

import java.util.Date
import bi.fris.common.InvalidToken

import scala.collection.JavaConversions.mapAsJavaMap
import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.concurrent.{Future, ExecutionContext, Promise}
import scala.concurrent.duration.{ Duration, DurationInt }
import scala.util.Try
import org.apache.commons.codec.binary.Base64
import com.auth0.jwt.{ JWTSigner, JWTVerifier }

import scala.async.Async.{ await, async }

import play.api.libs.json._

trait JSONWebToken {
  val secret = "my secret"
  val maxAge = 30 days;

  def token(username: String) = {
    val expire = "" + (maxAge.toMillis + now)
    val claims = Map("username" -> username, "expire" -> expire)
    new JWTSigner(secret).sign(claims)
  }
  def decodeF(token: String): Future[Option[String]] = {
    Future.successful(decode(token))
  }

  def decode(token: String):Option[String] = {
    val claims = Try(new JWTVerifier(Base64.encodeBase64String(secret.getBytes("utf8"))).verify(token).asScala.toMap)
    for {
      c <- claims.toOption
      _ <- validate(c)
      username <- c.get("username")
    } yield username.toString()

  }

  def validate(claims: Map[String, Object]) = {
    def isNotExpired(date: Object) = !isExpired(date.toString.toLong)
    claims.get("expire").collect {
      case expiration if isNotExpired(expiration) => expiration
    }
  }
  def expiresIn(d: Duration = maxAge): Long = d.toMillis + now
  def isExpired(expirationDate: Long) = expirationDate > expiresIn()

  def now = new Date().getTime
}

trait ConfirmationToken extends JSONWebToken {
  val extra = "extra"
  def confirmationToken(txt: String) = {
    token(extra + txt)
  }
  def decodeToken(token: String) = {
    decode(token).map(_.replace(extra, ""))
  }
} 
trait Signature extends JSONWebToken {  
  def sign(parts: List[String]) = {    
    token(parts.reduceLeft((a,b)=> s"${a}:${b}"))
  }
  def signOne(data: String) = {
    sign(List(data))
  }
  def extract(token: String) = {
    for{
      str <- decode(token)  
    } yield str.split(":").toList
    
  }
  def extractJson[T](token: String)(implicit tjs: Format[T]) = {
    for{
      str <- decode(token)
      result <- Json.fromJson[T](Json.parse(str)).asOpt
    } yield result
  }
  def extractJsonF[T](token: String)(implicit tjs: Format[T]) = {
    extractJson(token) match {
      case Some(t) => Future.successful(t)
      case None => Future.failed(InvalidToken())
    }
  }
} 