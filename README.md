Quizzy
=======

Description
-----------
A quiz application built using [Leaky Cauldron](https://github.com/josephlbarnett/leakycauldron)

Built primarily to serve the needs of an email based Rules Exchange.

Getting Started
---------------
#### Install jdk and maven
* Install openjdk 17 or newer
* Install maven 3.9.2 or newer (or use included ./mvnw script instead of mvn)
#### Build and run tests
* Building with maven will run tests
```
mvn clean install
```
* View test coverage reports for each module at ${module}/target/site/jacoco/index.html
#### Run
* Run the built fat jar
```
java -jar server/target/server-1.0-SNAPSHOT-shaded.jar
```
* Run via maven
```
cd server
mvn exec:java -Dexec.mainClass=com.trib3.server.TribeApplicationKt
```

Project Layout
--------------
Note that most modules make use of [Guice](https://github.com/google/guice) for 
dependency injection of configured instances.

* /api

This contains the API layer definition (model classes, etc).

* /client

This contains the [Vue](https://vuejs.org) client application.

* /persistence

This contains the DB layer implementation.

* /server

This contains the API layer implementation.
