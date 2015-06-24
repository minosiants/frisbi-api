package bi.fris

import akka.event.slf4j.Logger

trait Logging1 {
  lazy val logger = Logger(this.getClass.getName)
}