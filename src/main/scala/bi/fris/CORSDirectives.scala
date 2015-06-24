package bi.fris


import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server._
import akka.http.scaladsl.model.StatusCodes.OK
import Directives._
trait CORSDirectives {
  val corsHeaders = List(
    `Access-Control-Allow-Origin`(HttpOrigin("http://localhost:3000")),
    //`Access-Control-Allow-Origin`(SomeOrigins(Seq(HttpOrigin("https://ydls.co")))),
    `Access-Control-Allow-Credentials`(allow = true),
    `Access-Control-Allow-Methods`(POST, GET, OPTIONS, DELETE, PATCH),
    `Access-Control-Allow-Headers`("Content-Type", "Accept", "X-Session-Header", "Authorization")
  )
  val respondWithCors: Directive0 = respondWithHeaders(corsHeaders)
  val optionsForCors = options(complete(OK))
}
