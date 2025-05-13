FROM maven:3.9.4-eclipse-temurin-21 AS installer

WORKDIR /opt/build

COPY pom.xml .

RUN mvn dependency:go-offline


FROM installer AS builder
COPY src ./src

RUN mvn clean package -DskipTests


FROM eclipse-temurin:21 AS bundler

WORKDIR /opt/build
COPY --from=builder /opt/build/target/*.jar ./application.jar

RUN java -Djarmode=layertools -jar application.jar extract
RUN $JAVA_HOME/bin/jlink \
         --add-modules `jdeps --ignore-missing-deps -q -recursive --multi-release 17 --print-module-deps -cp 'dependencies/BOOT-INF/lib/*' application.jar` \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output jdk


FROM eclipse-temurin:21-jre-ubi9-minimal

ARG BUNDLE_PATH=/opt/build
ENV PATH="${JAVA_HOME}/bin:${PATH}"

RUN groupadd --gid 1000 spring-app \
  && useradd --uid 1000 --gid spring-app --shell /bin/bash --create-home spring-app

COPY entrypoint.sh /home/spring-app/entrypoint.sh

RUN chmod 777 /home/spring-app/entrypoint.sh

USER root

RUN microdnf install -y hostname && microdnf clean all

USER spring-app:spring-app
WORKDIR /home/spring-app

COPY --from=bundler $BUNDLE_PATH/spring-boot-loader/ ./
COPY --from=bundler $BUNDLE_PATH/dependencies/ ./
COPY --from=bundler $BUNDLE_PATH/application/ ./

ENTRYPOINT ["/home/spring-app/entrypoint.sh"]

#ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
