FROM amazoncorretto:17-alpine-jdk

WORKDIR /app
COPY admin-server/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
