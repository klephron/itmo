# ---------- STAGE 1: Build with Maven ----------
FROM maven:3.9.4-eclipse-temurin-21 AS deps

# Создаём рабочую директорию для сборки
WORKDIR /app

# Скопируем POM и исходники
COPY pom.xml .

RUN mvn dependency:go-offline

FROM deps AS builder
COPY src ./src
# Собираем проект (скачиваем зависимости, компилируем, запускаем Shade)
RUN mvn clean package -DskipTests

# После сборки, нужный jar лежит в /app/target/*.jar

# ---------- STAGE 2: Run on a minimal JRE ----------
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Копируем из builderа (файл fat-jar, например, data-simulator-mqtt-1.0-SNAPSHOT-shaded.jar)
COPY --from=builder /app/target/iot-controller-1.0-SNAPSHOT.jar /app/iot-controller.jar

# Копируем entrypoint.sh в рабочую директорию контейнера
COPY entrypoint.sh /app/entrypoint.sh

# Делаем скрипт исполняемым
RUN chmod 777 /app/entrypoint.sh

# Устанавливаем скрипт как точку входа
ENTRYPOINT ["./entrypoint.sh"]
