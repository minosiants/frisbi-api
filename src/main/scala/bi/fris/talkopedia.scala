package bi.fris

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import collection.JavaConversions._

case object Start
case object Stop

object Settings1 {
  val config = ConfigFactory.load();
  val appConf = config.getConfig("talkopedia")
  val serverConf = appConf.getConfig("server")

  val appHost = serverConf.getString("host")
  val appPort = serverConf.getInt("port")

  val cors = serverConf.getConfig("cors")
  val origins = cors.getStringList("origins").toList
  val neo4j = new Neo4jConfig(appConf.getConfig("neo4j"))

  class Neo4jConfig(config: Config) {
    val host = config.getString("host")
    val port = config.getInt("port")
    val db = config.getString("db")
    val username = config.getString("username")
    val password = config.getString("password")
    val schemaNodes = config.getString("schema-nodes")
    val schemaConstraints = config.getStringList("schema-constraints").toList
  }
}

