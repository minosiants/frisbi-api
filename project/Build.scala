import com.typesafe.sbt.SbtGit._
import com.typesafe.sbt.SbtScalariform._
import sbt._
import sbt.Keys._
import spray.revolver.RevolverPlugin._
import net.virtualvoid.sbt.graph.Plugin._
import com.typesafe.sbt.SbtAspectj._



object Build extends AutoPlugin {

  override def requires =
    plugins.JvmPlugin

  override def trigger =
    allRequirements

  override def projectSettings =    
    versionWithGit ++
    Revolver.settings ++
    graphSettings ++
    aspectjSettings ++
    List(
      // Core settings
      organization := "bi.fris",
      scalaVersion := Version.scala,
      crossScalaVersions := List(scalaVersion.value),
      scalacOptions ++= List(
        "-unchecked",
        "-deprecation",
        "-language:_",
        "-target:jvm-1.8",
        "-encoding", "UTF-8"
      ),
      javaOptions <++= AspectjKeys.weaverOptions in Aspectj,
      unmanagedSourceDirectories in Compile := List((scalaSource in Compile).value),
      unmanagedSourceDirectories in Test := List((scalaSource in Test).value),      
      // Git settings
      git.baseVersion := "1.0.0"
    )
}