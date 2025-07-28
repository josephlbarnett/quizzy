#FROM eclipse-temurin:21-jre-jammy
FROM ibm-semeru-runtimes:open-21-jre-focal

ARG CURRENT_VERSION

ADD server/target/server-${CURRENT_VERSION}-shaded.jar /app/server.jar

ENV PORT 8080
EXPOSE 8080

CMD JDBC_DATABASE_URL=`echo $DATABASE_URL | sed 's/^postgres:\/\/\([^:]*\):\([^@]*\)@\(.*\)$/jdbc:postgresql:\/\/\3?user=\1\&password=\2/'` java -XX:MaxRAM=70m -jar /app/server.jar
