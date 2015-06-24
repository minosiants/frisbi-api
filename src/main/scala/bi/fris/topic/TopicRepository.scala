package bi.fris
package topic


import scala.async.Async.async
import scala.concurrent.ExecutionContext
import org.anormcypher.CypherParser._
import org.anormcypher._
import com.github.nscala_time.time.Imports._
import bi.fris.common.Neo4jClient
import TopicProtocol._
import scala.concurrent.Future
import account.AccountProtocol.Profile

trait TopicRepository extends Neo4jClient with Logging1 {
  def createTopic(topic: Topic) = {
    val q = s"""
           MATCH(author:User{username:"${topic.author.username}"})
           CREATE (
               topic:Topic{
                    id:"${topic.id}",
                    title:"${topic.title}",
                    secret:${topic.secret},
                    author:"${topic.author.username}",
                    created_at:${topic.created_at.getMillis},
                    updated_at:${topic.created_at.getMillis}
           })
          CREATE (author)-[r:OF_TOPIC]->(topic)
          """
    logger.debug(q)
    query(q).execute()
  }

  def addTags(topicId: String, tags: Seq[String], author: String) = {
    val q = s"""MATCH(topic:Topic{id:"$topicId"})(user:User{username:"$author"})
                 ${
      tags.map { x =>
        s"""
                        MERGE ($x:Tag{name:"$x"})
                        MERGE ($x)-[d:DESCRIBES]->(topic)
                        MERGE ($x)-[b:BELONGS]->(user)
                      """
      }.mkString("")
    }
                 """

    logger.debug(q)
    query(q).execute()
  }

  def deleteTag(topicId: String, tag: String, author: String) = {
    val q = s"""
        MATCH(tag:Tag{name:"$tag"})-[d:DESCRIBES]->(topic:Topic{id:"$topicId"),
             (tag:Tag{name:"$tag"})-[b:BELONGS]->(user:User{username:"$author"})
       DELETE d, b
     """
    logger.debug(q)
    query(q).execute()
  }

  def deleteTopic(id: String, update_at: DateTime) = {
    val q = s"""
           MATCH(topic:Topic{id:"$id"}),(deleted_topics:Group{name:"deleted_topics"})
           SET topic.update_at=${update_at.getMillis}
           CREATE (topic)-[r:PART_OF_GROUP]->(deleted_topics)
          """
    logger.debug(q)
    query(q).execute()
  }

  def addParticipant(id: String, username: String, timestamp: DateTime) = {
    val q = s"""
          MATCH (topic:Topic {id:"$id"} ), (u:User {username:"$username"} )
          MERGE (topic)<-[p:IS_PARTICIPANT]-(u)
          ON CREATE SET p.activeTimes =[]
          SET p.activeTimes  = p.activeTimes +  ${timestamp.getMillis}
          SET p.active = true
          RETURN  p.activeTimes
    """
    logger.debug(q)
    query(q).execute()
  }

  def removeParticipant(id: String, username: String, timestamp: DateTime) = {
    val q = s"""
          OPTIONAL MATCH (topic:Topic {id:"$id"} )<-[p:IS_PARTICIPANT]-(u:User{username:"$username"})
          SET p.active = false
          RETURN  p.activeTimes
    """
    logger.debug(q)
    query(q).execute()
  }

  def findTopic(id: String, username: Option[String] = None)(implicit ec: ExecutionContext): Future[Option[Topic]] = async {
    val user = username.map { u => s" {username:'$u'} " }.getOrElse("")
    val q = s"""
            MATCH(topic:Topic{id:"$id"})<-[r:OF_TOPIC]-(u:User $user), (dt:Group {name:"deleted_topics"} )
            WHERE NOT (topic)-[:PART_OF_GROUP]->(dt)
            WITH topic, u
            OPTIONAL MATCH(topic)<-[isp:IS_PARTICIPANT]-(p:User)
            OPTIONAL MATCH (topic)-[:HAS_BACKGROUND]->(b:Background)
            WITH topic, u, collect({p:coalesce(p.username, ""), active:coalesce(isp.active, false)}) as participants, b
            RETURN  topic.id , topic.title, topic.secret,  topic.created_at, topic.updated_at, b.url,
                    u.username, u.email, u.created_at, u.updated_at, participants,
                    FILTER(ap IN participants WHERE ap.active = true) as activeParticipants,
                    LENGTH(participants) as pNumber,
                    LENGTH(filter(ap IN participants WHERE ap.active = true))  as apNumber
          """
    logger.debug(q)
    query(q) as topicParser.singleOpt
  }

  def findUserTopics(author: String, count: Int, since: Long, secret: Boolean = false)(implicit ec: ExecutionContext): Future[List[Topic]] = async {

    val withSecret = if (!secret) ",secret:false" else ""
    val q = s"""
            MATCH(topic:Topic{author:"$author" $withSecret }),(u:User{username:"$author"}), (dt:Group {name:"deleted_topics"} )
            WHERE topic.created_at < $since AND NOT (topic)-[:PART_OF_GROUP]->(dt)
            WITH topic, u
            OPTIONAL MATCH (topic)<-[isp:IS_PARTICIPANT]-(p:User)
            OPTIONAL MATCH (topic)-[:HAS_BACKGROUND]->(b:Background)
            WITH topic, u, collect({p:coalesce(p.username, ""), active:coalesce(isp.active, false)}) as participants, b
            OPTIONAL MATCH (u)-[:HAS_AVATAR]->(av:Avatar)
            RETURN  topic.id , topic.title, topic.secret, topic.created_at, topic.updated_at, b.url,
                    u.username, u.email, u.created_at, u.updated_at, participants, av.uri,
                    FILTER(ap IN participants WHERE ap.active = true) as activeParticipants,
                    LENGTH(participants) as pNumber,
                    LENGTH(filter(ap IN participants WHERE ap.active = true))  as apNumber
           ORDER BY topic.created_at DESC
           LIMIT $count
          """
    logger.debug(q)
    query(q) as topicParser.*
  }

  def findTopics(count: Int, since: Long, order: String)(implicit ec: ExecutionContext): Future[List[Topic]] = async {
    val orderBy = order match {
      case "latest" => " ORDER BY topic.created_at DESC"
      case "active" => " ORDER BY apNumber DESC"
      case "popular" => " ORDER BY pNumber DESC"
      case _ => ""
    }
    val activeOnly = order match {
      case "active" => Some("WHERE isp.active=true")
      case _ => None
    }
    val q = s"""
           MATCH(topic:Topic {secret:false})<-[r:OF_TOPIC]-(u:User), (dt:Group {name:"deleted_topics"} )
           WHERE topic.created_at < $since AND NOT (topic)-[:PART_OF_GROUP]->(dt)
           WITH topic , u
           ${activeOnly.map(s => "").getOrElse("OPTIONAL")} MATCH(topic)<-[isp:IS_PARTICIPANT]-(p:User)
           ${activeOnly.getOrElse("")}
           OPTIONAL MATCH (topic)-[:HAS_BACKGROUND]->(b:Background)
           WITH topic, u, collect({p:coalesce(p.username, ""), active:coalesce(isp.active, false)}) as participants, b
           OPTIONAL MATCH (u)-[:HAS_AVATAR]->(av:Avatar)
           RETURN  topic.id , topic.title, topic.secret,  topic.created_at, topic.updated_at, b.url,
                      u.username, u.email, u.created_at, u.updated_at,  participants, av.uri,
                      FILTER(ap IN participants WHERE ap.active = true) as activeParticipants,
                      LENGTH(participants) as pNumber,
                      LENGTH(filter(ap IN participants WHERE ap.active = true))  as apNumber
           $orderBy
           LIMIT $count
          """
    logger.debug(q)
    query(q) as topicParser.*
  }

  def updateBackground(id: String, url: String, timestamp: DateTime): Unit = {
    val q = s"""
               MATCH (topic:Topic{id:"$id"})
               MERGE (topic)-[:HAS_BACKGROUND]->(b:Background)
               SET b.url = "$url"
               return b
          """
    logger.debug(q)
    query(q).execute()
  }

  val topicParser =
    (str("topic.id") ~ str("topic.title") ~ bool("topic.secret") ~ long("topic.created_at") ~ long("topic.updated_at") ~ str("b.url").?
      ~ str("u.username") ~ str("u.email").? ~ str("av.uri").? ~ long("u.created_at") ~ long("u.updated_at") ~ int("apNumber") ~ get[Seq[Map[String, Any]]]("participants"))
      .map {
      case id ~ title ~ secret ~ created_at ~ updated_at ~ background
        ~ username ~ email ~ avatar ~ ucreated_at ~ uupdated_at ~ countActive ~ p =>
        Topic(id = id,
          title = title,
          background = background,
          secret = secret,
          author = Profile("", username, email, false, new DateTime(ucreated_at), new DateTime(uupdated_at), avatar),
          participants = p.map { m => Participant(username = m("p").asInstanceOf[String], active = m("active").asInstanceOf[Boolean]) }.toList,
          countActive = countActive,
          created_at = new DateTime(created_at),
          updated_at = new DateTime(updated_at))
    }

  val profileParser =
    (str("u.username") ~ str("u.email").? ~ str("u.created_at") ~ str("u.updated_at"))
      .map { case username ~ email ~ created_at ~ updated_at => Profile("", username, email, false, DateTime.parse(created_at), DateTime.parse(updated_at)) }
}