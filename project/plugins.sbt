addSbtPlugin("edu.gemini"     % "sbt-lucuma"        % "0.4.0")
addSbtPlugin("org.scala-js"   % "sbt-scalajs"       % "1.7.0")
addSbtPlugin("com.github.sbt" % "sbt-ci-release"    % "1.5.9")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"      % "2.4.3")
addSbtPlugin(("ch.epfl.scala" % "sbt-scalafix"      % "0.9.31").cross(CrossVersion.for3Use2_13))
addSbtPlugin("com.eed3si9n"   % "sbt-projectmatrix" % "0.8.0")
