FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# The jar file name might vary, using a wildcard is safer
COPY target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
