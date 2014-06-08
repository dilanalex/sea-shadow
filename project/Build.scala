import sbt._
import Keys._
import PlayProject._
import com.typesafe.sbteclipse.core.EclipsePlugin.EclipseKeys

object ApplicationBuild extends Build {

    val appName         = "obc"
    val appVersion      = "0.1-afternoon-sunrise"

    override def settings = super.settings ++ Seq(
      EclipseKeys.skipParents in ThisBuild := false
    )
      
    val appDependencies = Seq(
      // Add your project dependencies here,
      "com.github.twitter" %  "bootstrap"  % "2.0.3",
      "se.radley" %% "play-plugins-salat" % "1.1",
      "com.github.cleverage" % "elasticsearch_2.9.1" % "0.3",
      "org.imgscalr" % "imgscalr-lib" % "4.2",
      "commons-io" % "commons-io" % "2.0.1",
      "be.objectify" % "deadbolt-2_2.9.1" % "1.1.3-SNAPSHOT",
       "net.sf.opencsv" % "opencsv" % "2.1"
    )
    
    val ssDependencies = Seq(
      // Add your project dependencies here,
      "com.typesafe" %% "play-plugins-util" % "2.0.4",
      "org.mindrot" % "jbcrypt" % "0.3m"
    ) 
    
    val secureSocial = PlayProject(
        appName + "secureSocial", appVersion, ssDependencies, mainLang = SCALA, path = file("modules/securesocial")		
    ).settings(
      resolvers ++= Seq(
        "jBCrypt Repository" at "http://mvnrepository.com/",
        "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
      )	  
    )
    
    // Only compile the bootstrap bootstrap.less file and any other *.less file in the stylesheets directory
    /*def customLessEntryPoints(base: File): PathFinder = (
      (base / "app" / "assets" / "stylesheets" / "bootstrap" * "bootstrap.less") +++
      (base / "app" / "assets" / "stylesheets" ** "*.less")
    )*/

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here
      //lessEntryPoints <<= baseDirectory(customLessEntryPoints),
     
      resolvers += "webjars" at "http://webjars.github.com/m2",
      routesImport += "se.radley.plugin.salat.Binders._",
      templatesImport += "org.bson.types.ObjectId",
      resolvers += Resolver.url("GitHub Play2-elasticsearch Repository", url("http://cleverage.github.com/play2-elasticsearch/releases/"))(Resolver.ivyStylePatterns),
      resolvers += Resolver.url("Objectify Play Repository", url("http://schaloner.github.com/releases/"))(Resolver.ivyStylePatterns),
      resolvers += Resolver.url("Objectify Play Repository", url("http://schaloner.github.com/snapshots/"))(Resolver.ivyStylePatterns)    
    ).dependsOn(secureSocial).aggregate(secureSocial)

}
