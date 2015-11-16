lazy val frisbi = project.in(file("."))

organization := "bi.fris"
name := "frisbi"
version := "1.0"


scalaVersion   := "2.11.7"
scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-target:jvm-1.8",
  "-encoding", "UTF-8"
)

crossScalaVersions := List(scalaVersion.value)
//javaOptions <++= AspectjKeys.weaverOptions in Aspectj

unmanagedSourceDirectories in Compile := List((scalaSource in Compile).value)
unmanagedSourceDirectories in Test := List((scalaSource in Test).value)

val akkaVersion = "2.4.0"
val akkaHttpVersion = "2.0-M1"
libraryDependencies ++= List(
  "com.typesafe.akka"       %%   "akka-distributed-data-experimental" % akkaVersion,
  "com.typesafe.akka"       %%   "akka-cluster-sharding"              % akkaVersion,
  "com.typesafe.akka"       %%   "akka-http-experimental"             % akkaHttpVersion,
  "com.typesafe.akka"       %%   "akka-persistence"                   % akkaVersion,
  "com.typesafe.akka"       %%   "akka-distributed-data-experimental" % akkaVersion,
  "com.typesafe.play"       %%   "play-ws"                            % "2.4.3",
  "de.heikoseeberger"       %%   "akka-sse"                           % "1.2.0",
  "de.heikoseeberger"       %%   "akka-http-play-json"                %  "1.2.0",
  "org.reactivemongo"       %%   "reactivemongo"                      % "0.11.6",
  "com.github.scullxbones"  %%   "akka-persistence-mongo-rxmongo"     % "1.0.9",
  "org.anormcypher"         %%   "anormcypher"                        % "0.8.0",
  "commons-codec"           %    "commons-codec"                      %   "1.8",
  "com.auth0"               %    "java-jwt"                           % "1.0.0",
  "org.slf4j"               %    "slf4j-api"                          % "1.7.7",
  "com.typesafe.akka"       %%   "akka-slf4j"                         % "2.3.4",
  "ch.qos.logback"          %    "logback-classic"                    % "1.1.2",
  "com.twitter"             %    "twitter-text"                       % "1.6.1",
  "me.lessis"               %%   "courier"                            % "0.1.3",
  "com.amazonaws"           %    "aws-java-sdk"                       % "1.9.31",
  "org.scribe"              %    "scribe"                             % "1.3.7",
  "com.github.nscala-time"  %%    "nscala-time"                       % "1.6.0",
  //"de.heikoseeberger"         %% "akka-log4j"                         % "1.0.1",
  "com.typesafe.akka"       %%   "akka-testkit"                       % akkaVersion     % "test",
  "org.scalatest"           %%   "scalatest"                          % "2.2.5"         % "test"
)


resolvers ++= List(
  "sonatype OSS Snapshots"   at "https://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype OSS Releses"     at "https://oss.sonatype.org/content/repositories/releases",
  "akka Snapshot Repository" at "http://repo.akka.io/snapshots/",
  "hseeberger at bintray"    at "http://dl.bintray.com/hseeberger/maven",
  "patriknw at bintray"      at "http://dl.bintray.com/patriknw/maven",
  "atlassian public"         at "https://maven.atlassian.com/repository/public",
  "anormcypher"              at "http://repo.anormcypher.org/",
  "softprops-maven"          at "http://dl.bintray.com/content/softprops/maven"
  //"Kamon Releases"           at "http://snapshots.kamon.io/"
)
	  	  
//EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

initialCommands := """|import com.frisbi._""".stripMargin

addCommandAlias("rf1", "reStart -Dakka.remote.netty.tcp.port=2551 -Dakka.cluster.roles.0=frisbi -Dyodals.http-service.port=8080")
addCommandAlias("rf2", "run     -Dakka.remote.netty.tcp.port=2552 -Dakka.cluster.roles.0=frisbi -Dyodals.http-service.port=8081")
addCommandAlias("rf3", "run     -Dakka.remote.netty.tcp.port=2553 -Dakka.cluster.roles.0=frisbi -Dyodals.http-service.port=8082")
initialCommands := """|import com.frisbi._""".stripMargin

