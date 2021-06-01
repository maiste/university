name := "Trac"
version := "0.1"

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.2"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.2" % "test"
libraryDependencies += "org.scalatest" %% "scalatest-funsuite" % "3.2.2" % "test"

scalacOptions ++= Seq("-feature")

/* In case of StackOverflow during runs... */
// fork in run := true
// javaOptions += "-Xss100m"
