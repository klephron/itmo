FROM maven:3.9.4-eclipse-temurin-17 AS installer

WORKDIR /app

COPY pom.xml .
RUN mvn -T 1C dependency:go-offline

CMD ["mvn", "test"]
