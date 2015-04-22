import com.typesafe.sbt.pgp.PgpKeys._
import sbt.Keys._
import sbt.{State, TaskKey, _}
import scala.xml.transform.{RewriteRule, RuleTransformer}

object PackagingHelpers {
  private def testIfRemove(dep: xml.Node) =
    ((dep \ "scope").text == "test") ||
      ((dep \ "classifier").text == "sources")

  val removeTestOrSourceDependencies: (xml.Node) => xml.Node = { (node: xml.Node) =>
    val rewriteRule = new RewriteRule {
      override def transform(node: xml.Node) = node.label match {
        case "dependency" if testIfRemove(node) => xml.NodeSeq.Empty
        case _ => node
      }
    }
    val transformer = new RuleTransformer(rewriteRule)
    transformer.transform(node).headOption.getOrElse(node)
  }
}
