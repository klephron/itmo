FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
ARG MODULE
RUN ./gradlew :${MODULE}:bootJar -x test

FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y ffmpeg && rm -rf /var/lib/apt/lists/*
ARG MODULE
WORKDIR /app
COPY --from=build /app/${MODULE}/build/libs/*.jar /app/app.jar
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
