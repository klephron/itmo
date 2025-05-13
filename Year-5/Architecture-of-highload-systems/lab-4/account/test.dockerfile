FROM maven:3.9.4-eclipse-temurin-21

WORKDIR /app

COPY pom.xml .

RUN mvn -T 1C dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

CMD ["mvn", "test"]
