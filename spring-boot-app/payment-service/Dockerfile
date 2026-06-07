FROM eclipse-temurin:17-jre
WORKDIR /app
COPY payment-service/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
