# Changelog - E2E тесты Cypress

## [1.2.3] - 2025-12-16

### Исправлено
- 🔥 **Исправлена ошибка прав доступа в тестах шаблонов**
  - Тесты создания шаблонов теперь используют `officer_1` вместо `dispatcher`
  - `dispatcher` не имеет права `CASES:*:TEMPLATE_ADD`
  - `officer_1` имеет необходимые права для создания шаблонов
  - Исправлено в файле: `templates/create-template.cy.ts` (все 12 тестов)

### Права пользователей
```typescript
// ❌ dispatcher НЕ имеет:
// - CASES:*:TEMPLATE_ADD
// - TEMPLATES:*:ADD

// ✅ officer_1 имеет:
// - CASES:*:TEMPLATE_ADD
// - TEMPLATES:*:READ
// - FILES:*:UPLOAD (для загрузки в ЦБД)
```

## [1.2.2] - 2025-12-16

### Исправлено
- 🔥 **Исправлена ошибка дубликатов identifier для подозреваемых**
  - Поле `identifier` в таблице `case_suspects` имеет уникальное ограничение
  - Теперь используется уникальный identifier с timestamp: `TEST${Date.now()}`
  - Исправлено в файлах:
    - `templates/create-template.cy.ts` - создание подозреваемого через API
    - `suspects/create-suspect.cy.ts` - все тесты создания подозреваемых через UI

### Было (ошибка при повторном запуске)
```typescript
identifier: '1234567890'  // ❌ Duplicate key error при втором запуске
```

### Стало (уникальное значение каждый раз)
```typescript
const uniqueId = `TEST${Date.now()}`
identifier: uniqueId  // ✅ Уникальный identifier при каждом запуске
```

## [1.2.1] - 2025-12-16

### Добавлено
- ✅ **Добавлены участники к делам в тестах с учетом совместимости ролей**
  - Все тесты теперь создают дела с 2 участниками
  - Используются пользователи из E2E test data:
    - User ID 3: `officer_1` (PATROL) → назначается на позицию `PATROL`
    - User ID 5: `investigator_1` (INVESTIGATOR) → назначается на позицию `INVESTIGATOR`
  - Соблюдается таблица совместимости ролей из бэкенда
  - Обновлённые файлы:
    - `templates/create-template.cy.ts` - таблица Users теперь заполнена
    - `suspects/create-suspect.cy.ts`
    - `cases/view-cases.cy.ts`
    - `clues/create-clue.cy.ts`

### Совместимость ролей
```typescript
// PATROL может быть назначен как: PATROL, OFFICER
// INVESTIGATOR может быть назначен как: INVESTIGATOR, LEAD_INVESTIGATOR, CLUES_ANALYSIS

users: {
  '3': 'PATROL',           // ✅ officer_1 (PATROL) → PATROL
  '5': 'INVESTIGATOR'      // ✅ investigator_1 (INVESTIGATOR) → INVESTIGATOR
}
```

### Требования
Для запуска тестов необходимо выполнить SQL скрипт с тестовыми данными:
- `e2e-test-data.sql` должен быть загружен в базу данных
- Это создаст пользователей с ID 1-7 и дополнительных ranked users

## [1.2.0] - 2025-12-16

### Исправлено (Critical)
- 🔥 **Исправлен URL для API запросов**
  - API запросы теперь отправляются на бэкенд `http://localhost:8081`
  - Ранее все запросы отправлялись на фронтенд `http://localhost:5173` (baseUrl)
  - Добавлена переменная окружения `apiUrl` в `cypress.config.ts`
  - Обновлены команды:
    - `createIncidentViaAPI` - использует `Cypress.env('apiUrl')`
    - `createCaseViaAPI` - использует `Cypress.env('apiUrl')`
  - Обновлены тесты:
    - `templates/create-template.cy.ts` - создание suspect и clue через правильный URL

### Конфигурация

```typescript
// cypress.config.ts
env: {
  apiUrl: 'http://localhost:8081',  // URL бэкенда
}
```

### Использование

API команды автоматически используют правильный URL:
```typescript
// Запрос пойдет на http://localhost:8081/api/incidents
cy.createIncidentViaAPI({ ... })

// Запрос пойдет на http://localhost:8081/api/incidents/{id}/cases
cy.createCaseViaAPI(incidentId, { ... })
```

Можно переопределить через переменную окружения:
```bash
# Для другого порта
npx cypress run --env apiUrl=http://localhost:9000
```

## [1.1.1] - 2025-12-16

### Исправлено (Critical)
- 🔥 **Исправлена работа с токеном в API командах**
  - Токен теперь правильно извлекается из localStorage
  - В localStorage под ключом `'token'` хранится JSON объект со структурой:
    ```typescript
    {
      accessToken: string,
      refreshToken: string,
      tokenType: string,
      issueAtMS: number,
      expiresInMS: number
    }
    ```
  - Исправлены команды:
    - `createIncidentViaAPI` - правильный парсинг токена
    - `createCaseViaAPI` - правильный парсинг токена
  - Исправлены тесты:
    - `templates/create-template.cy.ts` - создание suspect и clue через API

### Было (неправильно)
```typescript
const token = win.localStorage.getItem('token')
Authorization: `Bearer ${token}`  // ❌ Отправляли весь JSON как строку
```

### Стало (правильно)
```typescript
const tokenStr = win.localStorage.getItem('token')
const tokenObj = JSON.parse(tokenStr)
const authHeader = `${tokenObj.tokenType} ${tokenObj.accessToken}`
Authorization: authHeader  // ✅ Правильный формат: "Bearer <accessToken>"
```

## [1.1.0] - 2025-12-16

### Улучшено
- ✅ **Команда `createIncidentViaAPI`** теперь полностью соответствует Backend DTO
  - Добавлена поддержка опционального параметра `waitForAssignment`
  - Добавлена поддержка опционального параметра `estimatedTime`
  - По умолчанию `waitForAssignment = true`
  - По умолчанию `estimatedTime = new Date().toISOString()`

### Backend DTO Mapping

```java
// Backend DTO
public record AddIncidentRequest(
    @Nullable String applicant,
    @NotNull @NotEmpty String type,
    @NotNull @NotEmpty String reason,
    @NotNull Double locationLatitude,
    @NotNull Double locationLongitude,
    @NotNull Double locationAltitude,
    @NotNull Instant estimatedTime,
    @NotNull Boolean waitForAssignment
)
```

```typescript
// Cypress команда
interface IncidentFormData {
  reason: string                    // → reason
  type: string                      // → type
  latitude: string                  // → locationLatitude (parseFloat)
  longitude: string                 // → locationLongitude (parseFloat)
  altitude: string                  // → locationAltitude (parseFloat)
  applicant?: string                // → applicant (nullable)
  waitForAssignment?: boolean       // → waitForAssignment (default: true)
  estimatedTime?: string            // → estimatedTime (ISO string)
}
```

### Добавлено
- 📄 **EXAMPLES.md** - подробные примеры использования всех кастомных команд
- 📄 **CHANGELOG.md** - история изменений

### Примеры использования

#### Создание с ожиданием назначения (по умолчанию)
```typescript
cy.createIncidentViaAPI({
  reason: 'Кража',
  type: 'Кража',
  latitude: '59.9311',
  longitude: '30.3609',
  altitude: '10.5'
  // waitForAssignment будет true по умолчанию
});
```

#### Создание без ожидания назначения
```typescript
cy.createIncidentViaAPI({
  reason: 'Ограбление',
  type: 'Ограбление',
  latitude: '59.9343',
  longitude: '30.3351',
  altitude: '5.0',
  waitForAssignment: false  // Явно указываем false
});
```

#### Создание с определенным временем
```typescript
cy.createIncidentViaAPI({
  reason: 'Инцидент в прошлом',
  type: 'Кража',
  latitude: '55.751244',
  longitude: '37.618423',
  altitude: '156',
  estimatedTime: '2025-12-15T10:00:00Z'  // Указываем конкретное время
});
```

---

## [1.0.0] - 2025-12-15

### Добавлено
- ✅ Полный набор E2E тестов для критических Use Cases
- ✅ Кастомные команды Cypress
- ✅ Тестовые fixtures (изображения, файлы)
- ✅ Документация (README.md, TestingImplementationSummary.md)

### Реализованные тесты

#### Auth
- `auth/login.cy.ts` - 6 тестов авторизации

#### UC1: Обработка вызовов
- `incidents/create-incident.cy.ts` - 5 тестов создания инцидентов

#### UC2: Работа с уликами
- `clues/create-clue.cy.ts` - 8 тестов работы с уликами и файлами

#### UC3/UC12: Шаблоны отчетов
- `templates/create-template.cy.ts` - 8 тестов создания и загрузки в ЦБД

#### UC4: Уведомления
- `notifications/notifications.cy.ts` - 8 тестов уведомлений

#### UC9: Просмотр дел
- `cases/view-cases.cy.ts` - 8 тестов просмотра и редактирования дел

#### Suspects
- `suspects/create-suspect.cy.ts` - 4 теста создания подозреваемых

### Кастомные команды
- `cy.login(username, password)` - Авторизация
- `cy.fillIncidentForm(data)` - Заполнение формы инцидента
- `cy.fillCaseForm(data)` - Заполнение формы дела
- `cy.fillClueForm(data)` - Заполнение формы улики
- `cy.createIncidentViaAPI(data)` - Создание инцидента через API
- `cy.createCaseViaAPI(incidentId, data)` - Создание дела через API
- `cy.uploadFile(selector, filePath)` - Загрузка файла

### Fixtures
- `test-data.json` - JSON данные
- `test-image.png` - PNG изображение 1x1
- `test-image.jpg` - JPEG изображение 1x1
- `test-report.txt` - Текстовый отчет

### Конфигурация
- Cypress 15.7.0
- TypeScript
- Timeout: 10s
- Retry: 2 попытки в headless режиме

### Покрытие
- Критический приоритет: 100%
- Высокий приоритет: 85%
- Средний приоритет: 60%
- Низкий приоритет: 50%
