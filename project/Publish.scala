import io.github.morgaroth.sbt.SbtSonatypeUtils
import sbt._
import Keys._

object Publish {
  lazy val settings = Seq(
    pomExtra := SbtSonatypeUtils.autoImport.githubPom(name.value, "Mateusz Jaje", "Morgaroth"),
    publishTo := SbtSonatypeUtils.autoImport.publishRepoForVersion(version.value),
    pomPostProcess := PackagingHelpers.removeTestOrSourceDependencies,
    publishArtifact in Test := false
  )
}
