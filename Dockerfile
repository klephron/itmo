ARG MODULE=common


FROM eclipse-temurin:17-jdk AS base

WORKDIR /opt/brainstop

COPY gradlew ./
COPY gradle/wrapper ./gradle/wrapper

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --version


FROM base AS deps

ARG MODULE

WORKDIR /opt/brainstop

COPY settings.gradle build.gradle ./

COPY ${MODULE}/build.gradle ./${MODULE}/.

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :${MODULE}:dependencies


FROM deps AS build

ARG MODULE

WORKDIR /opt/brainstop

COPY common ./common
COPY ${MODULE} ./${MODULE}

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :${MODULE}:bootJar -x test


FROM eclipse-temurin:17-jre

ARG MODULE

RUN apt-get update && apt-get install -y \
  ffmpeg \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/brainstop/${MODULE}

COPY --from=build /opt/brainstop/${MODULE}/build/libs/${MODULE}-*.jar ${MODULE}.jar

ENV MODULE=${MODULE}
ENV JAVA_OPTS=

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /opt/brainstop/${MODULE}/${MODULE}.jar"]
