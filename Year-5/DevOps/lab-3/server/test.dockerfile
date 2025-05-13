FROM maven:3.9.4-eclipse-temurin-17 AS installer

WORKDIR /app

COPY mvnw .
COPY .mvn ./.mvn

COPY pom.xml .
RUN ./mvnw -T 1C dependency:go-offline

COPY src ./src

CMD ["/app/mvnw", "test"]
