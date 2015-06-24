package bi.fris
package account

import scala.async.Async.async
import scala.concurrent.Future
import com.github.nscala_time.time.Imports.DateTime
import bi.fris.Logging1
import bi.fris.common.Neo4jClient
import scala.concurrent.ExecutionContext
import org.anormcypher.CypherParser._
import org.anormcypher._

import AccountProtocol._

trait AccountRepository extends Neo4jClient with Logging1 {
  def createAccount(id: String,
                    username: String,
                    password: String,
                    email: String,
                    created_at: DateTime,
                    updated_at: DateTime)(implicit ec: ExecutionContext): Future[Unit] = async {
    val q = s"""
           MATCH(users:Group{name:"users"})
           CREATE ( 
               user:User{
                          username:"$username",
                          password:"$password",
                          email:"$email",
                          created_at:${created_at.getMillis},
                          updated_at:${updated_at.getMillis}
           })          
          CREATE (user)-[r:PART_OF_GROUP]->(users)
          CREATE (account:Account{
                          id:"$id",
                          created_at:${created_at.getMillis},
                          updated_at:${updated_at.getMillis}
          }) 
          CREATE (user)-[b:BELONGS]->(account)                      
          """
    logger.debug(q)
    query(q).execute()
  }

  def deleteAccount(id: String, username: String, updated_at: DateTime)(implicit ec: ExecutionContext): Future[Unit] = async {
    val q = s"""
          MATCH (user:User{username="$username"}),(account:Account{id="$id"})
          SET user.updated_at=${updated_at.getMillis}
          SET account.updated_at=${updated_at.getMillis}
          MERGE (user)-[p:PART_OF_GROUP]->(deleted_users)
          MERGE (account)-[pp:PART_OF_GROUP]->(deleted_accounts)
      """
    logger.debug(q)
    query(q).execute()
  }

  def findProfile(username: String)(implicit ec: ExecutionContext): Future[Option[Profile]] = findProfile(Some(username))

  def findProfile(username: Option[String] = None, twitterId: Option[String] = None, fbId:Option[String]=None)(implicit ec: ExecutionContext): Future[Option[Profile]] = Future {
    val byUsername = username.map { un => s"WHERE u.username = '$un' OR u.email = '$un'" }.getOrElse("")
    val withTwitter = twitterId.map { twId => s" MATCH (u)-[:HAS_TWITTER]->(t:Twitter {id:'$twId'}) " }.getOrElse(" OPTIONAL MATCH (u)-[:HAS_TWITTER]->(t:Twitter) ")
    val withFb = fbId.map { id => s" MATCH (u)-[:HAS_FACEBOOK]->(fb:Facebook {id:'$id'}) " }.getOrElse(" OPTIONAL MATCH (u)-[:HAS_FACEBOOK]->(fb:Facebook) ")

    val social = twitterId match {
      case Some(id) => withTwitter + withFb
      case None => withFb + withTwitter
    }
    val q = s"""
          MATCH (u:User)-[:BELONGS]->(a:Account)
          $byUsername
          WITH u, a
          $social
          OPTIONAL MATCH (u)-[:HAS_AVATAR]->(av:Avatar)
          OPTIONAL MATCH (u)-[:IS]->(confirmed:Confirmed)
          RETURN a.id, u.username, u.email, u.created_at, u.updated_at, confirmed IS NOT NULL as confirmed , t.signedToken, av.uri, fb.signedToken
        """
    logger.debug(q)
    query(q).as(
      (str("a.id") ~ str("u.username") ~ str("u.email").? ~ long("u.created_at") ~ long("u.updated_at") ~ bool("confirmed") ~ str("t.signedToken").? ~ str("av.uri").? ~ str("fb.signedToken").?)
        .map { case id ~ username ~ email ~ created_at ~ updated_at ~ confirmed ~ twitterToken ~ avatar ~ fbToken=>
        Profile(id = id,
          username = username,
          email = email,
          confirmed = confirmed,
          created_at = new DateTime(created_at),
          updated_at = new DateTime(updated_at),
          twitter = twitterToken,
          facebook = fbToken,
          avatar = avatar)
      }
        .singleOpt
    )
  }

  def findProfileByTwitter(twitterId: String)(implicit ec: ExecutionContext): Future[Option[Profile]] = findProfile(None, Some(twitterId))
  def findProfileByFacebook(fbId: String)(implicit ec: ExecutionContext): Future[Option[Profile]] = findProfile(None, None, Some(fbId))


  def findUser(username: String, passwrod: Option[String])(implicit ec: ExecutionContext): Future[Option[User]] = async {
    val _password = passwrod.map { p => s""", password:"$p"""" }
    val q = s"""
          MATCH (u:User{username:"$username" ${_password.getOrElse("")}})
          RETURN u.username, u.email, u.created_at   
        """
    logger.debug(q)
    query(q).as(
      (str("u.username") ~ str("u.email").? ~ long("u.created_at"))
        .map { case username ~ email ~ created_at => User(username, email, new DateTime(created_at)) }
        .singleOpt
    )
  }

  def makeConfirmed(username: String, updated_at: DateTime)(implicit ec: ExecutionContext): Future[Unit] = async {
    val q = s"""
          MATCH (u:User{username:"$username"})
          MERGE (u)-[:IS]->(c:Confirmed)
          SET c.created_at = ${updated_at.getMillis}
          SET u.updated_at = ${updated_at.getMillis}
          RETURN u.username  
        """
    logger.debug(q)
    query(q).execute()
  }

  def addTwitter(id: String, id_str:String, screen_name: String, image:String, signedToken:String, updated_at: DateTime )(implicit ec: ExecutionContext): Future[Unit] = async {
    val q = s"""
          MATCH (u:User{id:"$id"})
          MERGE (u)-[:HAS_TWITTER]->(t:Twitter)
          SET t.signedToken = "$signedToken"
          SET t.id_str = "$id_str"
          SET t.screen_name = "$screen_name"
          SET t.image = "$image"
          SET t.updated_at = ${updated_at.getMillis}
          RETURN u.username  
        """
    logger.debug(q)
    query(q).execute()
  }
  def addFacebook(id: String, id_str:String, screen_name: String, image:String, signedToken:String, email:String, updated_at: DateTime)(implicit ec: ExecutionContext): Future[Unit] = async {
    val q = s"""
          MATCH (u:User{id:"$id"})
          MERGE (u)-[:HAS_FACEBOOK]->(f:Facebook)
          SET t.id_str = "$id_str"
          SET t.screen_name = "$screen_name"
          SET t.image = "$image"
          SET t.email = "$email"
          SET t.signedToken = "$signedToken"
          SET t.updated_at = ${updated_at.getMillis}
          RETURN u.username
        """
    logger.debug(q)
    query(q).execute()
  }

  def updateAvatar(username: String, uri: String, updated_at: DateTime)(implicit ec: ExecutionContext): Future[Unit] = Future {
    val q = s"""
          MATCH (u:User{username:"$username"})
          MERGE (u)-[:HAS_AVATAR]->(av:Avatar)
          SET u.updated_at = ${updated_at.getMillis}
          SET av.uri = "$uri"
          RETURN u.username
        """
    logger.debug(q)
    query(q).execute()
  }

  def updatePassword(username: String, password: String, updated_at: DateTime)(implicit ec: ExecutionContext): Future[Unit] = Future {
    val q = s"""
          MATCH (u:User{username:"$username"})
          MERGE (u)-[:IS]->(c:Confirmed)
          SET u.updated_at = ${updated_at.getMillis}, u.password = "$password"
          RETURN u.username
        """
    logger.debug(q)
    query(q).execute()
  }

  def createAccountFromTwitter(id: String,
                               username: String,
                               twitterId: String,
                               twitterName: String,
                               image: String,
                               signedToken: String,
                               created_at: DateTime,
                               updated_at: DateTime)
                              (implicit ec: ExecutionContext): Future[Unit] = Future {

    val q = s"""
           MATCH(users:Group{name:"users"})
           CREATE (
               user:User{
                          username:"$username",
                          created_at:${created_at.getMillis},
                          updated_at:${updated_at.getMillis}
           })
          CREATE (user)-[r:PART_OF_GROUP]->(users)
          CREATE (account:Account{
                          id:"$id",
                          created_at:${created_at.getMillis},
                          updated_at:${updated_at.getMillis}
          })
          CREATE (user)-[b:BELONGS]->(account)
          CREATE (user)-[:HAS_AVATAR]->(av:Avatar {uri:'$image'})
          CREATE (user)-[:HAS_TWITTER]->(t:Twitter {signedToken:'$signedToken',
                                                    image:'$image',
                                                    screenName:'$twitterName',
                                                    id:'$twitterId',
                                                    updated_at:'$updated_at',
                                                    created_at:'$created_at'})
          """
    logger.debug(q)
    query(q).execute()
  }
  def createAccountFromFacebook(id: String,
                               username: String,
                               fbId: String,
                               fbName: String,
                               image: String,
                               email:String,
                               signedToken: String,
                               created_at: DateTime,
                               updated_at: DateTime)
                              (implicit ec: ExecutionContext): Future[Unit] = Future {

    val q = s"""
           MATCH(users:Group{name:"users"})
           CREATE (
               user:User{
                          username:'$username',
                          email:'$email',
                          created_at:${created_at.getMillis},
                          updated_at:${updated_at.getMillis}
           })
          CREATE (user)-[r:PART_OF_GROUP]->(users)
          CREATE (account:Account{
                          id:'$id',
                          created_at:${created_at.getMillis},
                          updated_at:${updated_at.getMillis}
          })
          CREATE (user)-[b:BELONGS]->(account)
          CREATE (user)-[:HAS_AVATAR]->(av:Avatar {uri:'$image'})
          CREATE (user)-[:HAS_FACEBOOK]->(f:Facebook {signedToken:'$signedToken',
                                                    image:'$image',
                                                    screenName:'$fbName',
                                                    email:'$email',
                                                    id:'$fbId',
                                                    updated_at:'$updated_at',
                                                    created_at:'$created_at'})
          """
    logger.debug(q)
    query(q).execute()
  }
}