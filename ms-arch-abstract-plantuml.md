```plantuml-svg
@startuml
' ====== Global style ======
!option handwritten true
skinparam backgroundColor #FFFFFF
skinparam shadowing true
skinparam roundCorner 10
skinparam componentStyle rectangle
skinparam defaultTextAlignment center
skinparam ArrowColor #555555
skinparam ArrowThickness 1.3
skinparam ParticipantFontSize 13
skinparam DefaultFontSize 13
skinparam actorStyle awesome
skinparam linetype ortho
skinparam nodesep 80
skinparam ranksep 80
skinparam dpi 100
skinparam handwritten true
skinparam DefaultFontName "Comic Sans MS"

skinparam note {
  BackgroundColor #FFFDE7
  BorderColor #FBC02D
}

skinparam rectangle {
  FontSize 13
}

skinparam database {
  BackgroundColor #F3E5F5
  BorderColor #8E24AA
}

skinparam storage {
  BackgroundColor #FCE4EC
  BorderColor #D81B60
}

skinparam queue {
  BackgroundColor #FFF3E0
  BorderColor #FB8C00
}

skinparam component {
  BackgroundColor<<api>> #E3F2FD
  BorderColor<<api>> #1E88E5

  BackgroundColor<<worker>> #E8F5E9
  BorderColor<<worker>> #43A047

  BackgroundColor<<infra>> #F5F5F5
  BorderColor<<infra>> #757575
}

left to right direction


' ====== Clients ======
package "Clients" #Transparent {
  actor reader as "Reader"
  actor writer as "Writer"
  actor moderator as "Moderator"
  actor admin as "Admin"
}

note left of reader
  **Reader** – зритель:
  • просмотр ленты и стриминг
  • лайки, просмотры, комментарии
end note

note right of writer
  **Writer** – автор:
  • загрузка и управление видео
  • управление профилем
end note

note right of moderator
  **Moderator** – расширенный читатель:
  • видит флаги, жалобы, статусы решений
  • работает с модерацией в режиме read-mostly
end note

note right of admin
  **Admin** – полный контроль:
  • снятие/бан материалов
  • управление пользователями и ролями
end note


' ====== API Layer ======
package "API Layer" #Transparent {
  rectangle "Feed / Query API" as feed <<api>>
  rectangle "Streaming API\n(+ prefetch info)" as streaming <<api>>
  rectangle "Engagement API\n(likes/views/comments)" as engage <<api>>
  rectangle "Profile API" as profile <<api>>
  rectangle "Upload / Metadata API" as upload <<api>>
  rectangle "Admin / Moderation API" as adminapi <<api>>
}

' выравниваем колонку API простой цепочкой
feed -[hidden]-> streaming
streaming -[hidden]-> engage
engage -[hidden]-> profile
profile -[hidden]-> upload
upload -[hidden]-> adminapi

note bottom of "API Layer"
  **Tech stack:** Java • REST • JSON • OpenAPI
end note


' ====== Core Data & Infra ======
package "Core Data & Infra" #Transparent {
  database "PostgreSQL" as db <<infra>>
  storage "MinIO / S3" as s3 <<infra>>
  queue "RabbitMQ" as mq <<infra>>
  rectangle "Prometheus" as prom <<infra>>
}

package "Async Processing" #Transparent {
  rectangle "Transcode Worker\n+ FFmpeg" as worker <<worker>>
}

' чуть выровняем infra по вертикали
db -[hidden]-> s3
s3 -[hidden]-> mq
mq -[hidden]-> prom
prom -[hidden]-> worker


' =============================================================================
'  CLIENT → API (short labels)
' =============================================================================

' Reader — teal
reader -[#00897B]-> feed      : view feed
reader -[#00897B]-> streaming : watch
reader -[#00897B]-> engage    : read stats
reader -[#00897B]-> profile   : view user
reader -[#00897B]-> profile   : upload/transcode progress

' Writer — blue
writer -[#1E88E5]-> upload    : upload
writer -[#1E88E5]-> profile   : edit self
writer -[#1E88E5]-> adminapi  : self-moderate
writer -[#1E88E5]-> engage    : comment/write stats

' Moderator — purple-blue
moderator -[#5E35B1]-> adminapi : review
moderator -[#5E35B1]-> feed      : flagged
moderator -[#5E35B1]-> profile   : check user

' Admin — red
admin -[#C62828]-> adminapi : admin ops
admin -[#C62828]-> profile  : manage users
admin -[#C62828]-> feed     : manage videos


' =============================================================================
'  DB usage — purple
' =============================================================================
feed     -[#8E24AA]-> db : video meta
engage   -[#8E24AA]-> db : likes/views
profile  -[#8E24AA]-> db : users
adminapi -[#8E24AA]-> db : admin data
upload   -[#8E24AA]-> db : upload info


' =============================================================================
'  S3 usage — pink
' =============================================================================
upload    -[#D81B60]-> s3 : raw files
streaming <-[#D81B60]- s3 : read
profile   -[#D81B60]-> s3 : avatars


' =============================================================================
'  Queue / Events — orange
' =============================================================================
upload -[#FB8C00]-> mq : jobs
engage -[#FB8C00]-> mq : events
mq -[#FB8C00]-> worker  : jobs
mq -[#FB8C00]-> adminapi : signals


' =============================================================================
'  Worker flows — green
' =============================================================================
worker -[#43A047]-> s3 : write media
worker -[#43A047]-> db : update status


' =============================================================================
'  Metrics — gray
' =============================================================================
prom -[#757575]-> feed      : scrape
prom -[#757575]-> streaming : scrape
prom -[#757575]-> upload    : scrape
prom -[#757575]-> engage    : scrape
prom -[#757575]-> profile   : scrape
prom -[#757575]-> adminapi  : scrape
prom -[#757575]-> worker    : scrape


' ===== Notes =====
note right of engage
  **Events (AMQP):**
  • likes
  • views

  Может использоваться для:
  • realtime-метрик
  • модерации
end note

note bottom of mq
  **Upload job payload (JSON):**
    jobId, videoId, rawPath,
    presets, uploaderId, createdAt

  Status lifecycle:
    PUBLISHED → PROCESSING → DONE | ERROR
end note

note bottom of streaming
  **Предзагрузка:**
  • HEAD / Range-запросы
  • prefetch n+1 / n+2
end note

legend right
  == Legend ==
  <color:#00897B>Reader</color> — чтение контента
  <color:#1E88E5>Writer</color> — загрузка / правки
  <color:#5E35B1>Moderator</color> — модерация
  <color:#C62828>Admin</color> — полный контроль

  <color:#8E24AA>DB</color> — PostgreSQL
  <color:#D81B60>S3</color> — MinIO / S3
  <color:#FB8C00>AMQP</color> — RabbitMQ события
  <color:#43A047>Worker</color> — обработка / транскодинг
  <color:#757575>Prometheus</color> — метрики
end legend

@enduml
```
