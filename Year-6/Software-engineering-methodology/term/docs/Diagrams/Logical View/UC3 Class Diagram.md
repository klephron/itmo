```plantuml-svg
@startuml
title UC3 — Формирование шаблона отчёта (Logical View)
left to right direction
skinparam linetype ortho
skinparam nodesep 60
skinparam ranksep 60

package "controller" {
  class CasesController {
    +getCaseTemplates(...)
    +addCaseTemplate(...)
  }
  class TemplatesController {
    +getTemplate(...)
    +updateTemplate(...)
    +deleteTemplate(...)
    +addTemplateToCDB(...)
  }
}

package "application" {
  class TemplateService {
    +getTemplate(...)
    +getTemplatesByCaseId(...)
    +addTemplate(...)
    +updateTemplate(...)
    +deleteTemplate(...)
  }
  class CaseService {
    +getCaseById(...)
  }
  class ClueService {
    +getAllClueByCase(...)
  }
  class UserService {
    +getUser(...)
  }
}

package "infrastructure" {
  interface TemplateRepository
  interface CaseRepository
  interface ClueRepository
  interface UserRepository
}

package "domain" {
  class TemplateEntity
  class CaseEntity
  class ClueEntity
  class UserEntity
  class IncidentEntity
}

package "dto" {
  class TemplateDto
  class TemplateMainInfoDto
}

package "dto.request" as request {
  class AddTemplateRequest
  class UpdateTemplateRequest
}

package "mapper" {
  class TemplateMapper
  class TemplateMainInfoMapper
}

CasesController ..> TemplateService
CasesController ..> TemplateMapper
CasesController ..> TemplateMainInfoMapper
CasesController ..> AddTemplateRequest

TemplatesController ..> TemplateService
TemplatesController ..> TemplateMapper
TemplatesController ..> UpdateTemplateRequest

TemplateService ..> TemplateRepository
TemplateService ..> CaseService
TemplateService ..> ClueService
TemplateService ..> UserService

CaseService ..> CaseRepository
ClueService ..> ClueRepository
UserService ..> UserRepository

TemplateMapper ..> TemplateDto
TemplateMapper ..> TemplateEntity
TemplateMainInfoMapper ..> TemplateMainInfoDto
TemplateMainInfoMapper ..> TemplateEntity

TemplateDto ..> TemplateEntity
TemplateMainInfoDto ..> TemplateEntity
AddTemplateRequest ..> TemplateEntity
UpdateTemplateRequest ..> TemplateEntity

CaseEntity "1" o-- "0..*" TemplateEntity
TemplateEntity "0..*" --> "0..1" UserEntity : {creator, updator}
CaseEntity "1" -- "0..1" IncidentEntity
CaseEntity "0..*" --> "0..1" UserEntity : {creator, updator}
CaseEntity "1" o-- "0..*" ClueEntity
@enduml

```