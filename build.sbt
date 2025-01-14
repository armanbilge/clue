lazy val V = _root_.scalafix.sbt.BuildInfo

lazy val scala2Version      = V.scala213
lazy val scala3Version      = "3.1.2"
lazy val rulesCrossVersions = Seq(V.scala213)
lazy val allVersions        = rulesCrossVersions :+ scala3Version

ThisBuild / tlBaseVersion              := "0.22"
ThisBuild / tlCiReleaseBranches        := Seq("master")
ThisBuild / tlJdkRelease               := Some(8)
ThisBuild / githubWorkflowJavaVersions := Seq("11", "17").map(JavaSpec.temurin(_))
ThisBuild / scalaVersion               := scala2Version
Global / onChangedBuildSource          := ReloadOnSourceChanges

lazy val mimaSettings = Seq(
  mimaPreviousArtifacts ~= { _.filterNot(_.revision == "0.20.1") } // botched
)

lazy val root = tlCrossRootProject
  .aggregate(
    model,
    core,
    scalaJS,
    http4s,
    genRules,
    genInput,
    genOutput,
    genTests
  )
  .settings(
    name := "clue"
  )

lazy val model =
  projectMatrix
    .in(file("model"))
    .settings(
      moduleName := "clue-model",
      mimaSettings,
      libraryDependencies ++=
        Settings.Libraries.Cats.value ++
          Settings.Libraries.CatsTestkit.value ++
          Settings.Libraries.Circe.value ++
          Settings.Libraries.DisciplineMUnit.value ++
          Settings.Libraries.MUnit.value
    )
    .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaPartialVersion(scala3Version))
    .jvmPlatform(allVersions)
    .jsPlatform(allVersions)

lazy val core =
  projectMatrix
    .in(file("core"))
    .settings(
      moduleName := "clue-core",
      mimaSettings,
      libraryDependencies ++=
        Settings.Libraries.Cats.value ++
          Settings.Libraries.CatsEffect.value ++
          Settings.Libraries.Fs2.value ++
          Settings.Libraries.Log4Cats.value ++
          Settings.Libraries.Http4sCore.value ++
          Settings.Libraries.DisciplineMUnit.value ++
          Settings.Libraries.MUnit.value,
      scalacOptions += "-language:implicitConversions"
    )
    .dependsOn(model)
    .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaPartialVersion(scala3Version))
    .jvmPlatform(allVersions)
    .jsPlatform(allVersions)

lazy val scalaJS = projectMatrix
  .in(file("scalajs"))
  .settings(
    moduleName      := "clue-scalajs",
    mimaSettings,
    coverageEnabled := false,
    libraryDependencies ++=
      Settings.Libraries.ScalaJSDom.value ++
        Settings.Libraries.Http4sDom.value ++
        Settings.Libraries.ScalaJSMacrotaskExecutor.value
  )
  .dependsOn(core)
  .defaultAxes(VirtualAxis.js, VirtualAxis.scalaPartialVersion(scala3Version))
  .jsPlatform(allVersions)

lazy val http4s = projectMatrix
  .in(file("http4s"))
  .settings(
    moduleName := "clue-http4s",
    mimaSettings,
    libraryDependencies ++=
      Settings.Libraries.Http4sCirce.value ++
        Settings.Libraries.Http4sClient.value
  )
  .dependsOn(core)
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaPartialVersion(scala3Version))
  .jvmPlatform(allVersions)

lazy val http4sJDKDemo = projectMatrix
  .in(file("http4s-jdk-demo"))
  .enablePlugins(NoPublishPlugin)
  .settings(
    moduleName   := "clue-http4s-jdk-client-demo",
    tlJdkRelease := Some(11),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "log4cats-slf4j" % Settings.LibraryVersions.log4Cats,
      "org.slf4j"      % "slf4j-simple"   % "1.6.4"
    ) ++ Settings.Libraries.Http4sJDKClient.value
  )
  .dependsOn(http4s)
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaPartialVersion(scala3Version))
  .jvmPlatform(allVersions)

lazy val genRules =
  projectMatrix
    .in(file("gen/rules"))
    .settings(
      moduleName := "clue-generator",
      mimaSettings,
      libraryDependencies ++=
        Settings.Libraries.Grackle.value ++
          Settings.Libraries.ScalaFix.value ++
          Settings.Libraries.DisciplineMUnit.value ++
          Settings.Libraries.MUnit.value,
      scalacOptions ~= (_.filterNot(Set("-Vtype-diffs")))
    )
    .dependsOn(core)
    .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaPartialVersion(rulesCrossVersions.head))
    .jvmPlatform(rulesCrossVersions)

// Only necessary to fix inputs in place. Sometimes it gives a clearer picture than a diff.
// ThisBuild / scalafixScalaBinaryVersion :=
//   CrossVersion.binaryScalaVersion(scalaVersion.value)

lazy val genInput =
  projectMatrix
    .in(file("gen/input"))
    .enablePlugins(NoPublishPlugin)
    .settings(
      libraryDependencies ++=
        Settings.Libraries.Monocle.value
    )
    .dependsOn(core)
    // .dependsOn(genRules % ScalafixConfig) // Only necessary to fix inputs in place.
    .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaPartialVersion(scala3Version))
    .jvmPlatform(allVersions)

lazy val genOutput = projectMatrix
  .in(file("gen/output"))
  .enablePlugins(NoPublishPlugin)
  .settings(
    scalacOptions ++= { if (tlIsScala3.value) Nil else List("-Wconf:cat=unused:info") },
    libraryDependencies ++= Settings.Libraries.Monocle.value,
    tlFatalWarnings := false
  )
  .dependsOn(core)
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaPartialVersion(scala3Version))
  .jvmPlatform(allVersions)

lazy val genTestsAggregate = Project("genTests", file("target/genTestsAggregate"))
  .aggregate(genTests.projectRefs: _*)

lazy val genTests = projectMatrix
  .in(file("gen/tests"))
  .enablePlugins(NoPublishPlugin)
  .settings(
    libraryDependencies ++= Settings.Libraries.ScalaFixTestkit.value,
    scalafixTestkitOutputSourceDirectories :=
      TargetAxis
        .resolve(genOutput, Compile / unmanagedSourceDirectories)
        .value,
    scalafixTestkitInputSourceDirectories  :=
      TargetAxis
        .resolve(genInput, Compile / unmanagedSourceDirectories)
        .value,
    scalafixTestkitInputClasspath          :=
      TargetAxis.resolve(genInput, Compile / fullClasspath).value,
    scalafixTestkitInputScalacOptions      :=
      TargetAxis.resolve(genInput, Compile / scalacOptions).value,
    scalafixTestkitInputScalaVersion       :=
      TargetAxis.resolve(genInput, Compile / scalaVersion).value
  )
  .dependsOn(genRules)
  .enablePlugins(ScalafixTestkitPlugin)
  .defaultAxes(
    rulesCrossVersions.map(VirtualAxis.scalaABIVersion) :+ VirtualAxis.jvm: _*
  )
  .customRow(
    scalaVersions = Seq(V.scala213),
    axisValues = Seq(TargetAxis(scala3Version), VirtualAxis.jvm),
    settings = Seq()
  )
  .customRow(
    scalaVersions = Seq(V.scala213),
    axisValues = Seq(TargetAxis(V.scala213), VirtualAxis.jvm),
    settings = Seq()
  )
