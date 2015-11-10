package bi.fris

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.event.{ Logging, LoggingAdapter }
import scala.collection.breakOut
import scala.concurrent.Await
import scala.concurrent.duration.Duration
object BaseApp {

  private val opt = """-D(\S+)=(\S+)""".r

  def applySystemProperties(args: Array[String]): Unit = {
    def argsToProps(args: Array[String]) =
      args.collect { case opt(key, value) => key -> value }(breakOut)
    for ((key, value) <- argsToProps(args))
      System.setProperty(key, value)
  }
}

abstract class BaseApp[A] {

  import BaseApp._

  def main(args: Array[String]) {
    applySystemProperties(args)
    val system = ActorSystem("frisbi")
    val log = Logging.apply(system, getClass)

    log.debug("Waiting to become a cluster member ...")
    Cluster(system).registerOnMemberUp {
      run(system, log)
      log.info("App up and running")
    }

    Await.ready(system.whenTerminated, Duration.Inf)

  }

  def run(system: ActorSystem, log: LoggingAdapter): Unit
}