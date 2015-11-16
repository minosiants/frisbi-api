import sbt._

object Version {
  val akka                 = "2.4.0"
  val akkaHttp             = "2.0-M1"
  val akkaParsing          = "1.0-M2"
  val akkaPersistence      = "2.4.0"
  val persistenceCassandra = "0.3.5"
  val akkaPersistenceMongo = "1.0.9"
  val akkaSse              = "1.2.0"
  val play                 = "2.4.3"
  val logback              = "1.1.2"
  val scala                = "2.11.6"
  val scalaTest            = "2.2.2"
  val commonsCodec         = "1.8"
  val javaJwt              = "1.0.0"
  val anormcypher          = "0.7.1"
  val nscalaTime           = "1.6.0"
  val twitterText          = "1.6.1"
  val scalaAsync           = "0.9.2"
  val courier              = "0.1.3" 
  val twitter4j            = "4.0.3"
  val scribe               = "1.3.7"
  val aws                  = "1.9.31"
  val kamon                = "0.3.5"
  val httpJson             = "1.2.0"

}

object Library {
  val akkaActor            = "com.typesafe.akka"        %% "akka-actor"                    % Version.akka               withSources ()
  val akkaCluster          = "com.typesafe.akka"        %% "akka-cluster-sharding"         % Version.akka               withSources ()
  val akkaHttp             = "com.typesafe.akka"        %% "akka-http-core-experimental"   % Version.akkaHttp           withSources ()
  val akkaHttpSacla        = "com.typesafe.akka"        %% "akka-http-experimental"        % Version.akkaHttp           withSources ()
  val akkaStream           = "com.typesafe.akka"        %% "akka-stream-experimental"      % Version.akkaHttp           withSources ()
  val akkaPersistence      = "com.typesafe.akka"        %% "akka-persistence"              % Version.akkaPersistence
  val akkaDData            = "com.typesafe.akka"        %% "akka-distributed-data-experimental" % Version.akka
  val akkaLog4j            = "de.heikoseeberger"        %% "akka-log4j"                     % "1.0.2"
  val macroLogging        =  "de.heikoseeberger"        %% "akka-macro-logging"            % "0.1.0"
  //val akkaPersistenceMongo = "com.github.scullxbones"   %% "akka-persistence-mongo-casbah" % Version.akkaPersistenceMongo
  val akkaPersistenceMongo = "com.github.scullxbones"   %% "akka-persistence-mongo-rxmongo" % Version.akkaPersistenceMongo
  val reactivemongo        = "org.reactivemongo"        %% "reactivemongo"                 % "0.11.6"
  val akkaTestkit          = "com.typesafe.akka"        %% "akka-testkit"                  % Version.akka
  val log4j2               = "org.apache.logging.log4j" %  "log4j-core"                    % "2.3"
  val logbackClassic       = "ch.qos.logback"           %  "logback-classic"               % Version.logback
  val scalaTest            = "org.scalatest"            %% "scalatest"                     % Version.scalaTest
  val commonsCodec         = "commons-codec"            %  "commons-codec"                 % Version.commonsCodec             
  val auth0JavaJwt         = "com.auth0"                % "java-jwt"                       % Version.javaJwt
  val anormcypher          = "org.anormcypher"         %% "anormcypher"                    % Version.anormcypher        withSources ()
  val playWs               = "com.typesafe.play"       %% "play-ws"                        % Version.play
  val nscalaTime           = "com.github.nscala-time"  %% "nscala-time"                    % Version.nscalaTime
  val twitterText          = "com.twitter"              % "twitter-text"                   % Version.twitterText
  val scalaAsync           = "org.scala-lang.modules"  %% "scala-async"                    % Version.scalaAsync
  val courier              = "me.lessis"               %% "courier"                        % Version.courier
  val scribe               = "org.scribe"               % "scribe"                         % Version.scribe
  val aws                  = "com.amazonaws"            % "aws-java-sdk"                   % Version.aws
  val kamonCore            = "io.kamon"                %% "kamon-core"                     % Version.kamon
  val kamonDatadog         = "io.kamon"                %% "kamon-datadog"                  % Version.kamon
  val kamonSystemMetrics   = "io.kamon"                %% "kamon-system-metrics"           % Version.kamon
  val kamonAkkaRemote      = "io.kamon"                %% "kamon-akka-remote"              % Version.kamon
  val kamonLogReporter     = "io.kamon"                %% "kamon-log-reporter"             % Version.kamon
  val httpJson             = "de.heikoseeberger"       %% "akka-http-play-json"            %  Version.httpJson
 // val playJson             = "com.typesafe.play"       %% "play-json"                      % Version.play
  val akkaSse              = "de.heikoseeberger"       %% "akka-sse"                       % Version.akkaSse            withSources ()



}

object Resolver {
  val hseeberger     = "hseeberger at bintray"    at "http://dl.bintray.com/hseeberger/maven"
  val patriknw       = "patriknw at bintray"      at "http://dl.bintray.com/patriknw/maven"
  val atlassian      = "atlassian public"         at "https://maven.atlassian.com/repository/public"
  val anormcypher    = "anormcypher"              at "http://repo.anormcypher.org/"
  val akkaSnapshot   = "akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
  val sonatypeSnap   = "sonatype OSS Snapshots"   at "https://oss.sonatype.org/content/repositories/snapshots"
  val sonatypeRel    = "Sonatype OSS Releses"     at "https://oss.sonatype.org/content/repositories/releases"
  val softprops      = "softprops-maven"          at "http://dl.bintray.com/content/softprops/maven"
  val kamon          = "Kamon Releases"           at "http://snapshots.kamon.io/"
  val me             = "me at bintray"            at "http://dl.bintray.com/minosiants/maven"
 
}
