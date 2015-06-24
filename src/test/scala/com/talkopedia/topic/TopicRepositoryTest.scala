package bi.fris.topic

import org.joda.time.DateTime
import org.scalatest.FunSuite


import org.scalatest._
import org.scalatest.matchers._
import org.anormcypher._
import org.anormcypher.Neo4jREST._
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Future, Await, ExecutionContext}


class TopicRepositoryTest extends FlatSpec with Matchers with BeforeAndAfterEach with TopicRepository{

  it should "be able to return map" in {
    implicit def dispatcher = ExecutionContext.Implicits.global

    val res = findTopics(20, DateTime.now.getMillis, "latest")
    Await.ready(res, 10 seconds)
  }
}
