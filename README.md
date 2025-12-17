# Brainstop microservices

Монорепозиторий с микросервисами:
- **discovery** – Eureka (регистрация сервисов) – [discovery/](discovery/)
- **gateway** – brainstop-gateway (API-вход) – [gateway/](gateway/)
- **video** – video-service (видео, стриминг, фид) – [video/](video/)
- **profile** – profile-service (профили, аватары) – [profile/](profile/)
- **storage** – storage-service (MinIO-прокси) – [storage/](storage/)
- **interactions** – interactions-service (лайки, комментарии, жалобы) – [interactions/](interactions/)
- **admin** – admin-service (админ/модерация) – [admin/](admin/)
- **transcoding** – transcoding-service (FFmpeg, перекодирование, превью) – [transcoding/](transcoding/)
- **search** – search-service (embeddings/поиск) – [search/](search/)
- **common** – общий модуль с DTO/утилитами – [common/](common/)

## Требования
- JDK 17
- Gradle 8 (скрипты в репо)
- Docker + Docker Compose

## Быстрый старт (Docker)
```bash
# собрать образы (если не собраны)
docker compose build
# поднять всё
docker compose up -d
# посмотреть логи отдельного сервиса
docker compose logs -f video
```
Compose-файл: [docker-compose.yml](docker-compose.yml)

Основные внешние порты по умолчанию:
- gateway: 8080
- discovery: 8761 (UI http://localhost:8761)
- video/profile/storage/transcoding/...: только внутри сети compose

## Локальный запуск отдельного сервиса
```bash
# пример: video-service
./gradlew :video:bootRun --args='--spring.profiles.active=dev'
```
Перед запуском подготовьте инфраструктуру (PostgreSQL, MinIO, RabbitMQ, Eureka). Удобно поднять их тем же docker compose и остановить ненужные сервисы.

## Ключевые переменные окружения (по сервисам)
Spring relaxed binding: можно задавать через .env, env vars или application.yml.

Общее (для всех, кто ходит в эти компоненты):
- **SPRING_PROFILES_ACTIVE** – типовой профиль dev.
- **EUREKA_CLIENT_SERVICEURL_DEFAULTZONE** – URL Eureka (docker: http://discovery:8761/eureka/, локально: http://localhost:8761/eureka/).
- **SPRING_DATASOURCE_URL** / **SPRING_DATASOURCE_USERNAME** / **SPRING_DATASOURCE_PASSWORD** – PostgreSQL (docker: host postgres, порт 5432).
- **SPRING_RABBITMQ_HOST** / **SPRING_RABBITMQ_PORT** / **SPRING_RABBITMQ_USERNAME** / **SPRING_RABBITMQ_PASSWORD** – RabbitMQ (docker: host rabbitmq, порт 5672).
- **MINIO_URL** – URL MinIO (docker: http://minio:9000), **MINIO_ACCESS_KEY**, **MINIO_SECRET_KEY**, **MINIO_BUCKET**.
- Auth:
  - **APP_JWT_SECRET** / **APP_JWT_EXPIRATION_MS** – HMAC-секрет и время жизни токенов (используется gateway для выдачи токенов; остальные сервисы валидируют).
  - Если включаете resource-server с публичным ключом: **SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI** (issuer), **SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI** или **SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_PUBLIC_KEY_LOCATION**. Значения можно задать через .env или application.yml (см. примеры в конфиге).

Видео/транскодер:
- **TRANSCODER_FFMPEG_PATH** – путь к ffmpeg (в контейнере /usr/bin/ffmpeg).
- **TRANSCODER_FFPROBE_PATH** – путь к ffprobe (в контейнере /usr/bin/ffprobe, нужен video-service).

## Сборка
```bash
./gradlew clean build
```
Артефакты лежат в */build/libs. Docker-образы собираются через `docker compose build`.

## Полезные эндпоинты
- Eureka UI: http://localhost:8761
- Swagger UI через gateway со списком сервисов: http://localhost:8080/swagger-ui.html
- MinIO UI: http://localhost:9000

## FFmpeg/ffprobe
В контейнерах ставится через `apt-get install ffmpeg`. Для корректной работы перекодирования и превью задайте:
```bash
TRANSCODER_FFMPEG_PATH=/usr/bin/ffmpeg
TRANSCODER_FFPROBE_PATH=/usr/bin/ffprobe   # нужен video-service
```

## Типовой сценарий разработки
1. Поднять базовые зависимости: `docker compose up -d discovery postgres minio rabbitmq`
2. Запустить нужный сервис локально через `./gradlew :<module>:bootRun --args='--spring.profiles.active=dev'`
3. Проверить регистрацию в Eureka и прокси через gateway.
