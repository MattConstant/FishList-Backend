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
# Tuned for small containers (e.g. Render 512 MB / shared CPU). Override in the dashboard if needed.
# MaxRAMPercentage respects cgroup memory limit; Serial GC is light on CPU; TieredStopAtLevel=1 favors faster startup over peak throughput.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=65.0 -XX:InitialRAMPercentage=12.0 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:MaxMetaspaceSize=96m"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
