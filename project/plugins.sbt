// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
//resolvers += Classpaths.typesafeResolver
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % "2.0.4")

// Stage plugin to create local heroku stye environment
//addSbtPlugin("com.typesafe.startscript" % "xsbt-start-script-plugin" % "0.5.2")