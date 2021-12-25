package fm.sbt

import _root_.sbt._
import Keys._

import scala.reflect.runtime.universe
import scala.util.Try

trait S3ResolverPluginCompat {

  /** This provides a shim for pre-sbt 1.3.x+ tests */
  private lazy val sbtShimCsrResolvers: TaskKey[Seq[Resolver]] =    TaskKey[Seq[Resolver]]("sbtShimCsrResolvers", "Provide a sbt shim for pre-coursier versions")
  private lazy val sbtShimCsrSbtResolvers: TaskKey[Seq[Resolver]] = TaskKey[Seq[Resolver]]("sbtShimCsrSbtResolvers", "Provide a sbt shim for pre-coursier versions")
  private lazy val sbtShimSbtResolvers: SettingKey[Seq[Resolver]] = SettingKey[Seq[Resolver]]("sbtShimSbtResolvers", "Provide a sbt shim for pre-coursier versions")

  private lazy val csrResolvers: TaskKey[Seq[Resolver]] = loadIfExists(
    fullyQualifiedName = "sbt.Keys.csrResolvers",
    args = None,
    default = sbtShimCsrResolvers
  )

  private lazy val csrSbtResolvers: TaskKey[Seq[Resolver]] = loadIfExists(
    fullyQualifiedName = "sbt.Keys.csrSbtResolvers",
    args = None,
    default = sbtShimCsrSbtResolvers
  )

  private lazy val sbtResolvers: SettingKey[Seq[Resolver]] = loadIfExists(
    fullyQualifiedName = "sbt.Keys.sbtResolvers",
    args = None,
    default = sbtShimSbtResolvers
  )

  protected def compatProjectSettings: Seq[Setting[_]] = Seq(
    // s3 resolvers in `resolvers` task don't always get set in coursier keys in
    // newer sbt versions, so setup a cross-sbt 0.13.x-compatible shim and manually
    // put the s3 resolvers into the appropriate csrResolvers/csrSbtResolvers values

    // Initialize our shim and use found java-reflection value or shim value
    sbtShimSbtResolvers := resolvers.value,
    sbtResolvers := {
      if (sbtHasCoursier(sbtVersion.value)) sbtResolvers.value
      else sbtShimSbtResolvers.value
    },
    sbtShimCsrResolvers := Nil,
    csrResolvers := Def.taskDyn {
      val v = if (sbtHasCoursier(sbtVersion.value)) csrResolvers.value else sbtShimCsrResolvers.value
      Def.task {
        v
      }
    }.value,
    csrResolvers ++= resolvers.value,
    sbtShimCsrSbtResolvers := Nil,
    csrSbtResolvers := Def.taskDyn {
      val v = if (sbtHasCoursier(sbtVersion.value)) csrSbtResolvers.value else sbtShimCsrSbtResolvers.value
      Def.task {
        v
      }
    }.value,
    csrSbtResolvers ++= sbtResolvers.value,
  )

  private def sbtHasCoursier(sbtVersion: String): Boolean = CrossVersion.partialVersion(sbtVersion) match {
    case Some((1, minor)) if minor >= 3 => true
    case Some((1, minor)) if minor < 3  => false
    case Some((0, 13))                  => false
    case _                              => sys.error(s"Unsupported sbtVersion: ${sbtVersion}")
  }

  /**
   * From: https://github.com/etspaceman/kinesis-mock/blob/a7d94e74d367b74479f565fa9c5b5692e4d1b8fd/project/BloopSettings.scala#L8
   * License: MIT
   *
   * MIT License
   *
   * Copyright (c) 2021 Eric Meisel
   *
   * Permission is hereby granted, free of charge, to any person obtaining a copy
   * of this software and associated documentation files (the "Software"), to deal
   * in the Software without restriction, including without limitation the rights
   * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
   * copies of the Software, and to permit persons to whom the Software is
   * furnished to do so, subject to the following conditions:
   *
   * The above copyright notice and this permission notice shall be included in all
   * copies or substantial portions of the Software.
   * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
   * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
   * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
   * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
   * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
   * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
   * SOFTWARE.
   *
   * Example Usage:
   * {{{
   * val default: Seq[Def.Setting[_]] = loadIfExists(
   *   fullyQualifiedName = "bloop.integrations.sbt.BloopDefaults.configSettings",
   *   args = Some(Nil),
   *   default = Seq.empty[Def.Setting[_]]
   * )
   * }}}
   *
   * @param fullyQualifiedName
   * @param args
   * @param default
   * @tparam T
   * @return
   */
  private def loadIfExists[T](
    fullyQualifiedName: String,
    args: Option[Seq[Any]],
    default: => T
  ): T = {
    val tokens     = fullyQualifiedName.split('.')
    val memberName = tokens.last
    val moduleName = tokens.take(tokens.length - 1).mkString(".")

    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    val value = Try(runtimeMirror.staticModule(moduleName)).map { module =>
      val obj            = runtimeMirror.reflectModule(module)
      val instance       = obj.instance
      val instanceMirror = runtimeMirror.reflect(instance)
      val member =
        instanceMirror.symbol.info.member(universe.TermName(memberName))
      args
        .fold(instanceMirror.reflectField(member.asTerm).get)(args => instanceMirror.reflectMethod(member.asMethod)(args: _*))
        .asInstanceOf[T]
    }
    value.getOrElse(default)
  }
}