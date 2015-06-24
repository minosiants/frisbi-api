package bi.fris
package common

import java.security.MessageDigest
import java.math.BigInteger
import scala.util.Try

trait Hashing {
  def md5hash(txt:String):String = 
    digest.digest(txt.getBytes).map(0xFF & _).map("%02x".format(_)).mkString
  
    def digest = MessageDigest.getInstance("MD5")  
}