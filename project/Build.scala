import com.dyuproject.protostuff.compiler._
import sbt._
import Keys._

object SmeltBuild extends Build {

  import Resolvers._
  import Dependencies._
  import BuildSettings._


  lazy val smelt = Project(
    "smelt",
    file("."),
    settings = buildSettings ++ Seq(
      resolvers := allResolvers,
      libraryDependencies := allDeps,
      sourceGenerators in Test <+= sourceManaged in Test map {
        outDir: File =>
          generateProtostuff(new File("src/test/proto/foo.proto"), outDir)
      },
      sourceGenerators in Compile <+= sourceManaged in Compile map {
        outDir: File =>
          generateProtostuff(new File("src/main/proto/"), outDir)
      }
    )
  )

  def generateProtostuff(srcFile: File, outDir: File): Seq[File] = {
    val module = new ProtoModule(srcFile, "java_bean", "UTF8", outDir)
    module.setOption("generate_field_map", "")
    module.setOption("separate_schema", "")
    CompilerMain.compile(module)
    Seq()
  }

}

object BuildSettings {
  val buildVersion = "0.1.0-SNAPSHOT"
  val buildScalaVersion = "2.9.0-1"

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.technbolts",
    version := buildVersion,
    scalaVersion := buildScalaVersion,
    shellPrompt := ShellPrompt.buildShellPrompt,
    retrieveManaged := true // remove this once plugins are working or i understand their layout
  )
}

// Shell prompt which show the current project,
// git branch and build version
object ShellPrompt {

  object devnull extends ProcessLogger {
    def info(s: => String) {}

    def error(s: => String) {}

    def buffer[T](f: => T): T = f
  }

  val current = """\*\s+([\w-]+)""".r

  def gitBranches = ("git branch --no-color" lines_! devnull mkString)

  val buildShellPrompt = {
    (state: State) => {
      val currBranch =
        current findFirstMatchIn gitBranches map (_ group (1)) getOrElse "-"
      val currProject = Project.extract(state).currentProject.id
      "%s:%s:%s> ".format(
        currProject, currBranch, BuildSettings.buildVersion
      )
    }
  }
}

object Resolvers {
  val protostuffRepo = "protostuff-repo" at "http://protostuff.googlecode.com/svn/repos/maven2"
  val oracleRepo = "Oracle Maven2 Repo" at "http://download.oracle.com/maven"
  val jbossRepo = "repository.jboss.org" at "http://repository.jboss.org/nexus/content/groups/public/"

  val allResolvers = Seq(protostuffRepo, oracleRepo, jbossRepo)
}

object Dependencies {

  val netty = "org.jboss.netty" % "netty" % "3.2.4.Final"
  val rhinoJS = "rhino" % "js" % "1.7R2"
  val jacksonjson = "org.codehaus.jackson" % "jackson-core-lgpl" % "1.7.2"

  val slf4j = "org.slf4j" % "slf4j-api" % "1.6.0"
  val logback = "ch.qos.logback" % "logback-classic" % "0.9.25" % "runtime"

  val sleepycat = "com.sleepycat" % "je" % "4.0.92"

  val scalaSpecs = "org.scala-tools.testing" %% "specs" % "1.6.8" % "test"
  val mockito = "org.mockito" % "mockito-all" % "1.8.5" % "test"

  val protostuffVer = "1.0.0"
  val protostuffCore = "com.dyuproject.protostuff" % "protostuff-core" % protostuffVer
  val protostuffApi = "com.dyuproject.protostuff" % "protostuff-api" % protostuffVer

  val allDeps = Seq(netty, rhinoJS, jacksonjson, slf4j, logback, sleepycat, scalaSpecs, mockito, protostuffCore, protostuffApi)
}
