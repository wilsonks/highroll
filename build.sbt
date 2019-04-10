lazy val highroll = project
  .in(file("."))
  .settings(sharedSettings)
  .settings(doNotPublishArtifact)
  .aggregate(`highroll-game`, `teenpatti-table`)

lazy val `highroll-game` = project
  .settings(crossSettings)
  .settings(libraryDependencies += "co.fs2" %% "fs2-core" % "1.0.4")
  .settings(libraryDependencies += "com.tykhe.deck" %% "deck-device" % "2.0.0")

lazy val `teenpatti-table` = project
  .settings(addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
  .settings(crossSettings)
  .dependsOn(`highroll-game`)
  .settings(libraryDependencies += "com.tykhe.fxml" %% "fxml-desktop" % "1.7.1")
  .settings(libraryDependencies += "com.tykhe.fxml" %% "fxml-driver" % "1.7.1")
  .settings(libraryDependencies += "com.tykhe.fxml" %% "fxml-extras" % "1.7.1")

lazy val universalSettings = Seq(
  // basic settings
  packageName in Universal    := name.value,
  maintainer                  := "tykhegaming.com",
  assemblyJarName in assembly := "fat.jar",
  // remove all jar mappings in universal and append the fat jar
  mappings in Universal := (mappings in Universal).value.filterNot(_._2.endsWith(".jar")),
  mappings in Universal += (assembly in Compile).map(f => f -> s"lib/${f.name}").value,
  // bash scripts classpath only needs the fat jar
  scriptClasspath := Seq((assemblyJarName in assembly).value)
)

lazy val doNotPublishArtifact = Seq(
  publishArtifact                          := false,
  publishArtifact in (Compile, packageDoc) := false,
  publishArtifact in (Compile, packageSrc) := false,
  publishArtifact in (Compile, packageBin) := false
)

lazy val warnUnusedImport = Seq(
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 10)) =>
        Seq()
      case Some((2, n)) if n >= 11 =>
        Seq("-Ywarn-unused-import", "-Ypartial-unification")
    }
  },
  scalacOptions in (Compile, console) ~= {
    _.filterNot("-Ywarn-unused-import" == _)
  },
  scalacOptions in (Test, console) ~= {
    _.filterNot("-Ywarn-unused-import" == _)
  }
)

lazy val sharedSettings = warnUnusedImport ++ Seq(
  organization       := "com.tykhe.host",
  scalaVersion       := "2.12.8",
  crossScalaVersions := Seq("2.12.8"),
  scalacOptions ++= Seq(
    // warnings
    "-unchecked", // able additional warnings where generated code depends on assumptions
    "-deprecation", // emit warning for usages of deprecated APIs
    "-feature", // emit warning usages of features that should be imported explicitly
    // Features enabled by default
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:experimental.macros",
    // possibly deprecated options
    "-Ywarn-inaccessible"
  ),
  // Force building with Java 8
  initialize := {
    val required = "1.8"
    val current = sys.props("java.specification.version")
    assert(current == required, s"Unsupported build JDK: java.specification.version $current != $required")
  },
  // Targeting Java 6, but only for Scala <= 2.11
  javacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, majorVersion)) if majorVersion <= 11 =>
      // generates code with the Java 6 class format
      Seq("-source", "1.6", "-target", "1.6")
    case _ =>
      // For 2.12 we are targeting the Java 8 class format
      Seq("-source", "1.8", "-target", "1.8")
  }),
  // Targeting Java 6, but only for Scala <= 2.11
  scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, majorVersion)) if majorVersion <= 11 =>
      // generates code with the Java 6 class format
      Seq("-target:jvm-1.6")
    case _ =>
      // For 2.12 we are targeting the Java 8 class format
      Seq.empty
  }),
  // Linter
  scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, majorVersion)) if majorVersion >= 11 =>
      Seq(
        // Turns all warnings into errors ;-)
        "-Xfatal-warnings",
        // Enables linter options
        "-Xlint:adapted-args", // warn if an argument list is modified to match the receiver
        "-Xlint:nullary-unit", // warn when nullary methods return Unit
        "-Xlint:inaccessible", // warn about inaccessible types in method signatures
        "-Xlint:nullary-override", // warn when non-nullary `def f()' overrides nullary `def f'
        "-Xlint:infer-any", // warn when a type argument is inferred to be `Any`
        "-Xlint:missing-interpolator", // a string literal appears to be missing an interpolator id
        "-Xlint:doc-detached", // a ScalaDoc comment appears to be detached from its element
        "-Xlint:private-shadow", // a private field (or class parameter) shadows a superclass field
        "-Xlint:type-parameter-shadow", // a local type parameter shadows a type already in scope
        "-Xlint:poly-implicit-overload", // parameterized overloaded implicit methods are not visible as view bounds
        "-Xlint:option-implicit", // Option.apply used implicit view
        "-Xlint:delayedinit-select", // Selecting member of DelayedInit
        "-Xlint:by-name-right-associative", // By-name parameter of right associative operator
        "-Xlint:package-object-classes", // Class or object defined in package object
        "-Xlint:unsound-match" // Pattern match may not be typesafe
      )
    case _ =>
      Seq.empty
  }),
  // Scala 2.11
  scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 11)) => Seq("-Xexperimental")
    case _             => Seq.empty
  }),
  // Turning off fatal warnings for ScalaDoc, otherwise we can't release.
  scalacOptions in (Compile, doc) ~= (_ filterNot (_ == "-Xfatal-warnings")),
  // ScalaDoc settings
  autoAPIMappings := true,
  scalacOptions in ThisBuild ++= Seq(
    // Note, this is used by the doc-source-url feature to determine the
    // relative path of a given source file. If it's not a prefix of a the
    // absolute path of the source file, the absolute path of that file
    // will be put into the FILE_SOURCE variable, which is
    // definitely not what we want.
    "-sourcepath",
    file(".").getAbsolutePath.replaceAll("[.]$", "")
  ),
  parallelExecution in Test             := false,
  parallelExecution in IntegrationTest  := false,
  testForkedParallel in Test            := false,
  testForkedParallel in IntegrationTest := false,
  concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
  // https://github.com/sbt/sbt/issues/2654
  incOptions        := incOptions.value.withLogRecompileOnMacro(false),
  publishMavenStyle := true,
  publishTo := Some(
    "Tykhe Artifactory" at "http://repo-desktop:8081/artifactory/" + (if (isSnapshot.value)
      s"tykhe-snapshots;build.timestamp=${new java.util.Date().getTime}"
    else "tykhe-releases")),
  isSnapshot              := version.value endsWith "SNAPSHOT",
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  }, // removes optional dependencies
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
  licenses  := Seq("TykheLicense" -> url("http://tykhegaming.github.io/LICENSE.txt")),
  startYear := Some(2018),
  headerLicense := Some(
    HeaderLicense.Custom(
      """|Copyright (c) 2018 by Tykhe Gaming Private Limited
         |
         |Licensed under the Software License Agreement (the "License") of Tykhe Gaming Private Limited.
         |You may not use this file except in compliance with the License.
         |You may obtain a copy of the License at
         |
         |    http://tykhegaming.github.io/LICENSE.txt.
         |
         |NOTICE
         |ALL INFORMATION CONTAINED HEREIN IS, AND REMAINS THE PROPERTY OF TYKHE GAMING PRIVATE LIMITED.
         |THE INTELLECTUAL AND TECHNICAL CONCEPTS CONTAINED HEREIN ARE PROPRIETARY TO TYKHE GAMING PRIVATE LIMITED AND
         |ARE PROTECTED BY TRADE SECRET OR COPYRIGHT LAW. DISSEMINATION OF THIS INFORMATION OR REPRODUCTION OF THIS
         |MATERIAL IS STRICTLY FORBIDDEN UNLESS PRIOR WRITTEN PERMISSION IS OBTAINED FROM TYKHE GAMING PRIVATE LIMITED.""".stripMargin)),
  homepage := Some(url("https://github.com/tykhegaming/tykhe-host")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/tykhegaming/tykhe-host"),
      "scm:git@github.com:tykhegaming/tykhe-host.git"
    )),
  // formatting
  scalafmtOnCompile := true,
  developers        := List()
)

lazy val crossSettings = sharedSettings ++ Seq(
  unmanagedSourceDirectories in Compile += {
    baseDirectory.value.getParentFile / "shared" / "src" / "main" / "scala"
  },
  unmanagedSourceDirectories in Test += {
    baseDirectory.value.getParentFile / "shared" / "src" / "test" / "scala"
  }
)
lazy val crossVersionSharedSources: Seq[Setting[_]] =
  Seq(Compile, Test).map { sc =>
    (unmanagedSourceDirectories in sc) ++= {
      (unmanagedSourceDirectories in sc).value.map { dir =>
        scalaPartV.value match {
          case Some((2, y)) if y == 11 => new File(dir.getPath + "_2.11")
          case Some((2, y)) if y >= 12 => new File(dir.getPath + "_2.12")
        }
      }
    }
  }
lazy val ReleaseTag = """^v(\d+\.\d+\.\d+(?:[-.]\w+)?)$""".r

def scalaPartV = Def setting (CrossVersion partialVersion scalaVersion.value)

enablePlugins(GitVersioning)

// baseVersion setting represents the in-development (upcoming) version, as an alternative to SNAPSHOTS
git.baseVersion := "0.0.2"

git.gitTagToVersionNumber := {
  case ReleaseTag(v) => Some(v)
  case _             => None
}

git.formattedShaVersion := {
  val suffix = git.makeUncommittedSignifierSuffix(git.gitUncommittedChanges.value, git.uncommittedSignifier.value)

  git.gitHeadCommit.value map {
    _.substring(0, 7)
  } map { sha =>
    git.baseVersion.value + "-" + sha + suffix
  }
}

