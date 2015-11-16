package bi.fris

import de.heikoseeberger.akkalog4j.Log4jLogger
import org.apache.logging.log4j.LogManager


trait Logging1 {
  lazy val logger = LogManager.getLogger(this.getClass.getName)
}