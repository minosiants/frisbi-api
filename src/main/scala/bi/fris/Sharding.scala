package bi.fris
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import akka.actor.ActorIdentity
import akka.actor.ActorPath
import akka.actor.ActorSystem
import akka.actor.Identify
import akka.actor.Props
import akka.contrib.pattern.ClusterSharding
import akka.pattern.ask
import akka.persistence.journal.leveldb.SharedLeveldbJournal
import akka.persistence.journal.leveldb.SharedLeveldbStore
import akka.util.Timeout

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.contrib.pattern.{ ClusterSharding, ShardRegion }
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Future

trait Sharding {

  def start(): Unit = {
    Logging(system, getClass).info("Starting shard region for type [{}]", typeName)
    ClusterSharding(system).start(typeName, Some(props), idExtractor, shardResolver)
  }

  def tellEntry(name: String, message: Any)(implicit sender: ActorRef): Unit =
    shardRegion ! (name, message)

  def askEntry(name: String, message: Any)(implicit timeout: Timeout, sender: ActorRef): Future[Any] =
    shardRegion ? (name, message)

  protected def props: Props

  protected def typeName: String

  protected def system: ActorSystem

  private def idExtractor: ShardRegion.IdExtractor = {
    case (name: String, payload) => (name, payload)
  }

  private def shardResolver: ShardRegion.ShardResolver = {
    case (name: String, _) => (math.abs(name.hashCode) % 100).toString
  }

  private def shardRegion = ClusterSharding(system).shardRegion(typeName)
}