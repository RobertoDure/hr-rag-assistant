FROM openjdk:23-jdk-slim

# Set working directory
WORKDIR /app

# Copy the jar file
COPY target/assistant-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
