package bi.fris.aws

import java.io.{BufferedInputStream, InputStream, File}

import akka.actor.{ExtendedActorSystem, Extension, ExtensionKey}
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream
import bi.fris.Settings

import scala.concurrent.{ExecutionContext, Future}


object S3Client extends ExtensionKey[S3Client]

class S3Client (protected val system: ExtendedActorSystem) extends Extension{
  val appAws = Settings(system).aws
  lazy val s3 = new AmazonS3Client(new BasicAWSCredentials(appAws.key, appAws.secret));


  def upload(key:String, file:File)(implicit ec: ExecutionContext):Future[String] = Future {
      s3.putObject(new PutObjectRequest(appAws.bucket, key, file))
      s"${appAws.bucket}/${key}"
  }
  def upload(key:String, bytes:Array[Byte])(implicit ec: ExecutionContext):Future[String] = Future {
      s3.putObject(new PutObjectRequest(appAws.bucket, key, new ByteInputStream(bytes, bytes.length), new ObjectMetadata()))
      s"${appAws.bucket}/${key}"
  }

}

