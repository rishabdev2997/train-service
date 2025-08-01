# Use lightweight OpenJDK 17 runtime as base image
FROM eclipse-temurin:17-jre-alpine

# Set working directory inside the container
WORKDIR /app

# Copy the built jar from the target folder into the container
COPY target/train-service-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your service listens on
EXPOSE 8083

# Run the Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
