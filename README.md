# tykhe-host

Fingerprint encoding for host machines.

### Packaging
Packages are built using [sbt-native-packager](https://www.scala-sbt.org/sbt-native-packager/index.html).
```sbtshell
universal:stage                 // for staging
universal:packageBin            // for ZIP package
universal:packageZipTarball     // for TGZ package
```

### Modules

#### host-security

Fingerprint verification for applications.
```sbt
libraryDependencies += "com.tykhe.host" %% "host-security" % "0.0.2"
```