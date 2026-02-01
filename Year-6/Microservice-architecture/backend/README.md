# BrainStop microservices

Монорепозиторий микросервисов BrainStop:

- discovery - Eureka (реестр сервисов) - [discovery/](discovery/)
- gateway - brainstop-gateway (API gateway, маршрутизация, auth, Swagger UI) - [gateway/](gateway/)
- video - video-service (стриминг, загрузка, лента, метаданные) - [video/](video/)
- profile - profile-service (профили, подписки, аватары) - [profile/](profile/)
- storage - storage-service (фасад MinIO для видео, превью и аватаров) - [storage/](storage/)
- interactions - interactions-service (лайки, просмотры, комментарии, жалобы) - [interactions/](interactions/)
- admin - admin-service (админка и модерация) - [admin/](admin/)
- transcoding - transcoding-service (FFmpeg, транскодирование, обработка видео) - [transcoding/](transcoding/)
- search - search-service (поиск и эмбеддинги) - [search/](search/)
- common - общая библиотека DTO/утилит - [common/](common/)
- stress-tests - нагрузочные тесты k6 - [stress-tests/](stress-tests/)
- djl-cache - локальный кэш моделей (LLM/эмбеддинги)

## Документация
- Архитектура: [ms-arch.md](ms-arch.md)
- Диаграмма (PlantUML): [ms-arch-abstract-plantuml.md](ms-arch-abstract-plantuml.md)

## Требования
- JDK 17
- Gradle 8
- Docker + Docker Compose
- k6 (опционально, для нагрузочных тестов)

## Быстрый старт (Docker)
```bash
# собрать образы (если нужно)
docker compose build
# поднять стек
docker compose up -d
# посмотреть логи конкретного сервиса
docker compose logs -f video
```
Compose-файл: [docker-compose.yml](docker-compose.yml)

Порты:
- gateway: 8080
- discovery: 8761 (UI http://localhost:8761)
- остальные сервисы работают в сети compose

## Инфраструктура (docker compose)
- PostgreSQL (с pgvector; init-скрипты в [docker/db](docker/db))
- Kafka
- Redis
- MinIO
- Ollama (локальная LLM) + `ollama-pull` для загрузки модели

## База данных и миграции
- Init-скрипты лежат в [docker/db](docker/db) и выполняются Postgres при первом старте контейнера (если volume пустой).
- Скрипты выполняются в порядке имени файла (например, расширения, роли, индексы, настройки ленты).
- Если volume уже создан, скрипты из `docker/db` нужно применять вручную через `psql`.

## Локальный запуск отдельного сервиса
```bash
# пример: video-service
./gradlew :video:bootRun --args='--spring.profiles.active=dev'
```
Перед локальным запуском подними зависимости (PostgreSQL, MinIO, Kafka, Eureka).

### Demo-данные (dev-seed)
Есть профиль **dev-seed**, который заполняет тестовые данные (только video-service):
- Конфиг: [video/src/main/java/ru/itmo/zavar/brainstop/video/config/DevSeedConfig.java](video/src/main/java/ru/itmo/zavar/brainstop/video/config/DevSeedConfig.java)
- Запуск: `./gradlew :video:bootRun --args='--spring.profiles.active=dev,dev-seed'`
- Требование: в PostgreSQL должен быть установлен pgvector (`CREATE EXTENSION IF NOT EXISTS vector;`).

## Переменные окружения
Spring relaxed binding: параметры можно задавать через .env, env vars или application.yml.

Общие (для всех сервисов):
- **SPRING_PROFILES_ACTIVE** - например `dev` или `dev,dev-seed`.
- **EUREKA_CLIENT_SERVICEURL_DEFAULTZONE** - Eureka (docker: `http://discovery:8761/eureka/`, локально: `http://localhost:8761/eureka/`).
- **SPRING_DATASOURCE_URL** / **SPRING_DATASOURCE_USERNAME** / **SPRING_DATASOURCE_PASSWORD** - PostgreSQL (docker: host `postgres`, port `5432`).
- **SPRING_KAFKA_BOOTSTRAP_SERVERS** - Kafka (docker: `kafka:9092`, локально: `localhost:9092`).
- **SPRING_DATA_REDIS_HOST** / **SPRING_DATA_REDIS_PORT** - Redis (docker: host `redis`, port `6379`).
- **MINIO_URL** - MinIO (docker: `http://minio:9000`), **MINIO_ACCESS_KEY**, **MINIO_SECRET_KEY**, **MINIO_BUCKET**.

Auth:
- **APP_JWT_SECRET**, **APP_JWT_EXPIRATION_MS** - HMAC секрет и TTL токенов (gateway).
- Для resource-server: **SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI** или **SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI**, или **SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_PUBLIC_KEY_LOCATION**.

Видео / транскодирование:
- **TRANSCODER_FFMPEG_PATH** - путь к ffmpeg (в контейнере: `/usr/bin/ffmpeg`).
- **TRANSCODER_FFPROBE_PATH** - путь к ffprobe (в контейнере: `/usr/bin/ffprobe`, используется video-service).

LLM (video/search):
- **LLM_OPENROUTER_ENABLED**, **LLM_VIDEO_ENABLED**, **LLM_SEARCH_ENABLED**
- **LLM_OPENROUTER_BASE_URL** (для Ollama: `http://ollama:11434/v1`)
- **LLM_OPENROUTER_MODELS**, **OPENROUTER_API_KEY**, **OPENROUTER_TITLE**, **OPENROUTER_REFERER**

Дополнительно:
- pgvector нужен для поиска/эмбеддингов.
- common подключается как зависимость во все сервисы.

## OpenAPI и Swagger
- Swagger UI через gateway: http://localhost:8080/swagger-ui.html
- OpenAPI для каждого сервиса доступен на `/v3/api-docs`.
- В gateway настроены агрегированные ссылки `/v3/api-docs/{service}` (video/profile/storage/admin/search/interactions).

## Быстрое появление сервисов после старта
Для уменьшения окна с 503 после старта сервисов:
- В gateway включен короткий TTL кэша load balancer (см. `gateway/src/main/resources/application.yml`).
- В discovery отключен read-only response cache и уменьшен интервал обновления (см. `discovery/src/main/resources/application.yml`).
Если сервис все равно не виден сразу, проверь `/actuator/health` и время готовности сервиса.

## Сборка
```bash
./gradlew clean build
```
Артефакты лежат в `*/build/libs`. Docker-образы собираются через `docker compose build`.

## Скрипты
- `run-local.ps1` - старт всех сервисов из jar (опции: `-Build`, `-NoDocker`, `-Profiles`, `-XmsMb`, `-XmxMb`).
- `run-tests.sh` - прогон тестов и jacoco для `admin` и `discovery`, плюс сводный отчёт.

## Нагрузочные тесты
Тесты лежат в [stress-tests/](stress-tests/), запуск через k6:
```bash
BASE_URL=http://localhost:8080 AUTH_TOKEN=... k6 run stress-tests/stress-test.js
```
Другие сценарии: `feed-load-test.js`, `upload-load-test.js`, `interactions-load-test.js` и др.

## Полезные ссылки
- Eureka UI: http://localhost:8761
- Swagger UI через gateway: http://localhost:8080/swagger-ui.html
- MinIO UI: http://localhost:9000

## FFmpeg/ffprobe
В контейнере можно установить: `apt-get install ffmpeg`.
Для корректной работы транскодирования укажи:
```bash
TRANSCODER_FFMPEG_PATH=/usr/bin/ffmpeg
TRANSCODER_FFPROBE_PATH=/usr/bin/ffprobe
```

## Типовой сценарий разработки
1. Поднять зависимости: `docker compose up -d discovery postgres minio kafka`
2. Запустить сервис локально: `./gradlew :<module>:bootRun --args='--spring.profiles.active=dev'`
3. Проверить регистрацию в Eureka и доступ через gateway.
