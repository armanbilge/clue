resolvers += Resolver.sonatypeRepo("snapshots")
addSbtPlugin("edu.gemini"     % "sbt-lucuma-lib"    % "0.6-3ce8fc2-SNAPSHOT")
addSbtPlugin("org.scala-js"   % "sbt-scalajs"       % "1.8.0")
addSbtPlugin(("ch.epfl.scala" % "sbt-scalafix"      % "0.9.33").cross(CrossVersion.for3Use2_13))
addSbtPlugin("com.eed3si9n"   % "sbt-projectmatrix" % "0.9.0")
