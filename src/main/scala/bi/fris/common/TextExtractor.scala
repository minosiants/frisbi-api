package bi.fris
package common

import com.twitter.Extractor
import scala.collection.JavaConverters._

trait TextExtractor {
  import TextExtractor._
  def extractHashtags(text: String) = extractor.extractHashtags(text).asScala.toList
}

object TextExtractor {
  val extractor = new Extractor()
}