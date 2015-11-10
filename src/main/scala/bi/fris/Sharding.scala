package bi.fris

import akka.cluster.sharding.{ClusterShardingSettings, ClusterSharding, ShardRegion}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Future

trait Sharding {

  def start(): Unit = {
    Logging(system, getClass).info("Starting shard region for type [{}]", typeName)
    ClusterSharding(system).start(
      typeName,
      props,
      ClusterShardingSettings(system),
      idExtractor,
      shardResolver)
  }

  def tellEntry(name: String, message: Any)(implicit sender: ActorRef): Unit =
    shardRegion ! (name, message)

  def askEntry(name: String, message: Any)(implicit timeout: Timeout, sender: ActorRef): Future[Any] =
    shardRegion ? (name, message)

  protected def props: Props

  protected def typeName: String

  protected def system: ActorSystem

  private def idExtractor: ShardRegion.ExtractEntityId = {
    case (name: String, payload) => (name, payload)
  }

  private def shardResolver: ShardRegion.ExtractShardId = {
    case (name: String, _) => (math.abs(name.hashCode) % 100).toString
  }

  private def shardRegion = ClusterSharding(system).shardRegion(typeName)
}