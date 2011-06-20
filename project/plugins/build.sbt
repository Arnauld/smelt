// remove this once plugins are working
retrieveManaged := true // and the dependencies will be copied to lib_managed as a build-local cache

resolvers += "protostuff-repo" at "http://protostuff.googlecode.com/svn/repos/maven2"

libraryDependencies ++= Seq(
   "com.dyuproject.protostuff" % "protostuff-compiler" % "1.0.0"
)