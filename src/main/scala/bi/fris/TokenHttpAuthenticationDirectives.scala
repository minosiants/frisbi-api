package bi.fris

import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.directives.{SecurityDirectives, AuthenticationResult, Credentials, AuthenticationDirective}
import akka.http.scaladsl.server.directives.BasicDirectives._
import akka.http.scaladsl.util.FastFuture
import akka.http.scaladsl.util.FastFuture._
import bi.fris.account.AccountProtocol.Profile
import bi.fris.account.AccountRepository

import scala.concurrent.{ExecutionContext, Future}

object TokenHttpAuthenticationDirectives {

  type TokenHttpAuthenticator[T] = Credentials => Future[Option[T]]

  object TokenHttpAuthentication extends JSONWebToken with SecurityDirectives {

    override def challengeFor(realm: String) = HttpChallenge(scheme = "jwt", realm = realm, params = Map.empty)

    def apply[T](realm: String)(authenticator: TokenHttpAuthenticator[T]): AuthenticationDirective[T] =
      extractExecutionContext.flatMap { implicit ctx =>
        authenticateOrRejectWithChallenge[OAuth2BearerToken, T] { basic =>
          authenticator(authDataFor(basic)).fast.map {
            case Some(t) => AuthenticationResult.success(t)
            case None => AuthenticationResult.failWithChallenge(challengeFor(realm))
          }
        }
      }

    private def authDataFor(cred: Option[OAuth2BearerToken]): Credentials =
      cred match {
        case Some(OAuth2BearerToken(token)) =>
          decode(token).map { username =>
            new Credentials.Provided(username) {
              def verify(secret: String): Boolean = true
            }
          }.getOrElse(Credentials.Missing)
        case Some(a) =>
          Credentials.Missing
        case None =>
          Credentials.Missing
      }
  }

  object TokenHttpAuthenticator {
    implicit def apply[T](f: Credentials => Future[Option[T]]): TokenHttpAuthenticator[T] =
      new TokenHttpAuthenticator[T] {
        def apply(credentials: Credentials): Future[Option[T]] = f(credentials)
      }

    def fromPF[T](pf: PartialFunction[Credentials, Future[Option[T]]])(implicit ec: ExecutionContext): TokenHttpAuthenticator[T] =
      new TokenHttpAuthenticator[T] {
        def apply(credentials: Credentials): Future[Option[T]] =
          if (pf.isDefinedAt(credentials)) pf(credentials)
          else FastFuture.successful(None)
      }

    def checkAndProvide[T](check: Credentials.Provided => Boolean)(provide: String => Option[T])(implicit ec: ExecutionContext): TokenHttpAuthenticator[T] =
      TokenHttpAuthenticator.fromPF {
        case p@Credentials.Provided(name) if check(p) => FastFuture.successful(provide(name))
      }

    /* def provideUserName(check: UserCredentials.Provided => Boolean)(implicit ec: ExecutionContext): TokenHttpAuthenticator[String] =
       checkAndProvide(check)(identity)*/
  }


  object TokenAuth extends AccountRepository {
    def tokenAuth(implicit ec: ExecutionContext) = TokenHttpAuthentication("talkopedia") {
      TokenHttpAuthenticator.fromPF[Profile] {
        case p@Credentials.Provided(username) => findProfile(username)
      }
    }
    def tokenAuthO(implicit ec: ExecutionContext) = TokenHttpAuthentication("talkopedia") {
      TokenHttpAuthenticator.fromPF[Profile] {
        case p@Credentials.Provided(username) => findProfile(username)
      }
    }.optional
  }
}

