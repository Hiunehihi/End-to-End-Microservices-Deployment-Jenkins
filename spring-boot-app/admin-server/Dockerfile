# Build stage
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml pom.xml
COPY admin-server/pom.xml admin-server/pom.xml
COPY admin-server/src admin-server/src

RUN mvn -f admin-server/pom.xml clean package spring-boot:repackage -DskipTests

# Run stage
FROM amazoncorretto:17-alpine-jdk

WORKDIR /app
COPY --from=build /workspace/admin-server/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
