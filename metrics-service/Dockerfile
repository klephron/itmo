FROM maven:3.9.4-eclipse-temurin-21 AS deps

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

FROM deps AS builder
COPY src ./src

RUN mvn clean package -DskipTests



FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY --from=builder /app/target/metrics-service-1.0-SNAPSHOT.jar /app/metrics-service.jar

ENTRYPOINT ["java", "-jar", "/app/metrics-service.jar"]
