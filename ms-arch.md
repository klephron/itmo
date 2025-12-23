```plantuml-svg
@startuml
!includeurl https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml

LAYOUT_WITH_LEGEND()

title BrainStop – C4 Container

Person(user, "Пользователь", "Смотрит ленту, загружает видео, ставит лайки, пишет комментарии")

System_Boundary(tiktok, "BrainStop") {

  Container(react, "BrainStop Web App", "React SPA", "UI ленты, загрузки, студии, аккаунта. Работает в браузере и ходит в API Gateway")

  Container(gateway, "API Gateway & Auth", "Spring Boot", "Единая точка входа: маршрутизация к микросервисам, обработка /api/auth/*, выдача JWT, валидация токенов")

  Container(streamSvc, "Streaming & Video Service", "Spring Boot", "HTTP Range-стриминг сконвертированных MP4 из MinIO, Загрузка видео, метаданные, постановка задач конвертации")
  Container(transcodeSvc, "Transcode Worker", "Spring Boot worker", "Консьюмер задач конвертации, запуск ffmpeg, обновление статусов job’ов")
  Container(commentSvc, "Comment Service", "Spring Boot", "Комментарии к видео, список комментов")
  Container(likeSvc, "Metrics Service", "Spring Boot", "Лайки, просмотры, счётчики, события лайков")
  Container(profileSvc, "User Profile Service", "Spring Boot", "Профиль пользователя, аватар, статистика, подписки, подписчики")
  Container(adminSvc, "Admin / Moderation Service", "Spring Boot", "Админка, модерация контента и пользователей")

  Container(cache, "Redis Cache", "Redis", "Базовый кэш частых запросов")
}

' Внешние системы
ContainerDb_Ext(db, "PostgreSQL", "RDBMS", "Общая база: пользователи, видео, лайки, комментарии, профили, права, статусы job'ов и т.п")
Container_Ext(minio, "MinIO / S3", "Object Storage", "Хранение raw и processed видеофайлов")
Container_Ext(queue, "Kafka", "Kafka Broker", "Очередь задач конвертации и событий лайков")
Container_Ext(prom, "Prometheus", "Monitoring", "Сбор метрик с /actuator/prometheus у gateway и сервисов")
System_Ext(ffmpeg, "FFmpeg", "CLI tool", "Конвертация видео в H.264 MP4 и создание превью")

' Пользователь ↔ фронт ↔ платформа
Rel(user, react, "Использует через браузер", "HTTPS")
Rel(react, gateway, "REST / JSON, HTML5 видео, WebSocket (опц.)", "HTTPS")

' Gateway ↔ сервисы
Rel(gateway, streamSvc, "Маршрутизирует /api/videos/, /streaming/video/list|upload", "REST/JSON, multipart")
Rel(gateway, streamSvc, "Маршрутизирует /streaming/video/{uuid}", "REST/HTTP Range")
Rel(gateway, commentSvc, "Маршрутизирует /api/comments/", "REST/JSON")
Rel(gateway, likeSvc, "Маршрутизирует /api/videos/{uuid}/like, /api/videos/{uuid}/view", "REST/JSON")
Rel(gateway, profileSvc, "Маршрутизирует /api/users/", "REST/JSON")
Rel(gateway, adminSvc, "Маршрутизирует /api/admin/, /api/moderation/", "REST/JSON")
Rel(gateway, gateway, "Обрабатывает /api/auth/* (регистрация, логин, refresh, проверка токена)", "REST/JSON") 

' Gateway ↔ кэш
Rel(gateway, cache, "Кэш JWT-сессий, профилей, популярных видео, горячих счётчиков лайков/просмотров", "Redis")

' Сервисы ↔ общая БД (вынесена за границы System_Boundary)
Rel(gateway, db, "Читает/пишет пользователей, хеши паролей, роли, базовые справочники", "JPA / SQL")
Rel(streamSvc, db, "Читает/пишет FileMetadata, статусы job’ов", "JPA / SQL")
Rel(streamSvc, db, "Читает метаданные видео", "JPA / SQL")
Rel(commentSvc, db, "Читает/пишет комментарии", "JPA / SQL")
Rel(likeSvc, db, "Читает/пишет лайки, просмотры и агрегированные счётчики", "JPA / SQL")
Rel(profileSvc, db, "Читает/пишет профили, аватары (метаданные)", "JPA / SQL")
Rel(adminSvc, db, "Админские операции по всем сущностям", "JPA / SQL")

' Видео и конвертация
Rel(streamSvc, minio, "Кладёт raw-видео после загрузки", "S3 PUT (raw/)")
Rel(streamSvc, queue, "Публикует задачи конвертации (jobId, videoId, rawPath)", "Kafka (transcoding.command)")

Rel(transcodeSvc, queue, "Подписывается на задачи конвертации", "Kafka consumer")
Rel(transcodeSvc, minio, "Читает raw, пишет processed-видео", "S3 GET/PUT")
Rel(transcodeSvc, ffmpeg, "Вызывает ffmpeg, парсит прогресс", "CLI / stdout")
Rel(transcodeSvc, db, "Обновляет статусы job’ов и FileMetadata", "JPA / SQL")

Rel(streamSvc, minio, "Стримит processed-видео по Range-запросам", "S3 GET (processed/*)")

' Лайки и события
Rel(likeSvc, queue, "Публикует события лайков (VideoLikeEvent)", "Kafka topic (video.likes)")
Rel(queue, adminSvc, "Опционально: подписывается для аналитики / модерации", "Kafka consumer")

' Метрики
Rel(prom, gateway, "Scrapes /actuator/prometheus", "HTTP pull")
Rel(prom, streamSvc, "Scrapes /actuator/prometheus", "HTTP pull")
Rel(prom, commentSvc, "Scrapes /actuator/prometheus", "HTTP pull")
Rel(prom, likeSvc, "Scrapes /actuator/prometheus", "HTTP pull")
Rel(prom, profileSvc, "Scrapes /actuator/prometheus", "HTTP pull")
Rel(prom, adminSvc, "Scrapes /actuator/prometheus", "HTTP pull")
Rel(prom, transcodeSvc, "Scrapes /actuator/prometheus", "HTTP pull")

@enduml
```
