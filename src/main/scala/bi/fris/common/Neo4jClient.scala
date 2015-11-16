package bi.fris
package common

import org.anormcypher.Neo4jREST
import org.anormcypher.Cypher
import play.api.libs.ws._
trait Neo4jClient {
  import Neo4jClient._
  implicit def connection = conn
  def query(q: String) = Cypher(q)

}

object Neo4jClient {
  import bi.fris.Settings1._
  val wsclient = ning.NingWSClient()
  val conn = Neo4jREST()(wsclient)
  def deleteAll() = {
    /*implicit val connection = conn
    val q = """
          MATCH (n)
          OPTIONAL MATCH (n)-[r]-()
          DELETE n,r
       """
    Cypher(q).execute()*/
  }
  def reset() = {
    /*implicit val connection = conn
    deleteAll()
    Cypher(neo4j.schemaNodes).execute()
    neo4j.schemaConstraints.foreach { c =>
      Cypher(c).execute()
    }*/
  }
}