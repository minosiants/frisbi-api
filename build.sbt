lazy val frisbi = project.in(file("."))

name := "frisbi"

version := "1.0"

libraryDependencies ++= List(
  Library.akkaContrib,
  //Library.akkaParsing,
  Library.akkaHttp,
  Library.akkaHttpSacla,
  Library.akkaStream,
  Library.akkaPersistence,
  Library.akkaPersistenceMongo,
  Library.akkaCluster,
  Library.akkaSse,
  Library.httpJson,
 // Library.playJson,
  Library.akkaSlf4j,
  Library.commonsCodec,
  Library.auth0JavaJwt,
  Library.anormcypher,
  Library.commonsCodec,
  Library.nscalaTime,
  Library.twitterText,
  Library.scalaAsync,  
  Library.courier,
  Library.scribe, 
  Library.aws,
  Library.kamonCore,
  Library.kamonDatadog,
//  Library.kamonSpray,
  Library.kamonAkkaRemote,
  Library.kamonLogReporter,
  Library.akkaTestkit % "test",
  Library.scalaTest   % "test"   
)

resolvers ++= List(
  Resolver.sonatypeRel,
  Resolver.sonatypeSnap,
  Resolver.atlassian,
  Resolver.anormcypher,
  Resolver.akkaSnapshot,
  Resolver.hseeberger,
  Resolver.kamon
)
	  	  
//EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

initialCommands := """|import com.frisbi._""".stripMargin

addCommandAlias("rf1", "reStart -Dakka.remote.netty.tcp.port=2551 -Dakka.cluster.roles.0=yodals -Dyodals.http-service.port=8080")
addCommandAlias("rf2", "run     -Dakka.remote.netty.tcp.port=2552 -Dakka.cluster.roles.0=yodals -Dyodals.http-service.port=8081")
addCommandAlias("rf3", "run     -Dakka.remote.netty.tcp.port=2553 -Dakka.cluster.roles.0=yodals -Dyodals.http-service.port=8082")
initialCommands := """|import com.frisbi._""".stripMargin

