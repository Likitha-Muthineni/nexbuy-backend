FROM openjdk:21-jdk-slim
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests
EXPOSE 8080
CMD ["java", "-jar", "target/nexbuy-backend-1.0.0.jar"]
