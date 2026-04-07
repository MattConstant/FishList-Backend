# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# Heap uses MaxRAMPercentage of cgroup memory. Metaspace is separate: Spring + Hibernate compile many
# query/model classes — do NOT cap Metaspace too low (96m caused OOM: Metaspace on deploy).
# On 512 MB instances only, set JAVA_OPTS in Render to lower MaxRAMPercentage and/or use -XX:MaxMetaspaceSize=256m.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=60.0 -XX:InitialRAMPercentage=12.0 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:MaxMetaspaceSize=384m"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
