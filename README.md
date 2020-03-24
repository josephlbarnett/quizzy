Example Leaky Cauldron Service
=======

Description
-----------
An example service using [Leaky Cauldron](https://github.com/trib3/leakycauldron)

Getting Started
---------------
#### Install jdk and maven
* Install openjdk 11 or newer
* Install maven 3.5 or newer
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

* /persistence

This contains the DB layer implementation.

* /server

This contains the API layer implementation.
