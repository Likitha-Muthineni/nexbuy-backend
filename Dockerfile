# Use Java 21 slim image
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy the Maven wrapper and project files
COPY . .

# Build the JAR
RUN ./mvnw clean package -DskipTests

# Expose port 8080
EXPOSE 8080

# Run the app
CMD ["java", "-jar", "target/nexbuy-backend-1.0.0.jar"]
