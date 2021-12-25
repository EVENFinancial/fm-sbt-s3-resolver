package fm.sbt

import _root_.sbt._

trait S3ResolverPluginCompat {
  protected def compatProjectSettings: Seq[Setting[_]] = Nil
}