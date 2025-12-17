# Примеры использования кастомных команд Cypress

## Конфигурация URL

### Разделение Frontend и Backend URL

Cypress использует два разных URL:
1. **`baseUrl`** (`http://localhost:5173`) - для открытия страниц фронтенда (`cy.visit()`)
2. **`apiUrl`** (`http://localhost:8081`) - для прямых API запросов к бэкенду (`cy.request()`)

Это настроено в `cypress.config.ts`:
```typescript
e2e: {
  baseUrl: 'http://localhost:5173',  // Frontend
  env: {
    apiUrl: 'http://localhost:8081',  // Backend API
  },
}
```

### Переопределение портов

Через командную строку:
```bash
# Изменить порт бэкенда
npx cypress run --env apiUrl=http://localhost:9000

# Изменить порт фронтенда
npx cypress run --config baseUrl=http://localhost:3000

# Изменить оба
npx cypress run --config baseUrl=http://localhost:3000 --env apiUrl=http://localhost:9000
```

Через конфиг файл:
```typescript
// cypress.config.ts
export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:3000',
    env: {
      apiUrl: 'http://production-backend.com',
    },
  },
});
```

## Создание инцидента через API

Команда автоматически использует `Cypress.env('apiUrl')` для отправки запросов на бэкенд.

### Базовое использование (с ожиданием назначения)
```typescript
cy.createIncidentViaAPI({
  reason: 'Кража личного имущества',
  type: 'Кража',
  latitude: '59.9311',
  longitude: '30.3609',
  altitude: '10.5',
  applicant: 'Иванов И.И.'
}).then((incidentId) => {
  cy.log(`Created incident with ID: ${incidentId}`);
});
```

### Создание с немедленным назначением (без ожидания)
```typescript
cy.createIncidentViaAPI({
  reason: 'Ограбление магазина',
  type: 'Ограбление',
  latitude: '59.9343',
  longitude: '30.3351',
  altitude: '5.0',
  applicant: 'Администратор магазина',
  waitForAssignment: false  // Не ожидать назначения
}).then((incidentId) => {
  // Теперь можно сразу создать дело
  cy.createCaseViaAPI(incidentId, {
    title: 'Дело по ограблению',
    description: 'Описание дела',
    users: { '1': 'INVESTIGATOR', '2': 'PATROL' }
  });
});
```

### Создание с определенным временем
```typescript
const specificTime = new Date('2025-12-15T10:00:00Z').toISOString();

cy.createIncidentViaAPI({
  reason: 'Инцидент в прошлом',
  type: 'Кража',
  latitude: '55.751244',
  longitude: '37.618423',
  altitude: '156',
  estimatedTime: specificTime  // Указываем конкретное время
}).then((incidentId) => {
  // ...
});
```

### Создание без заявителя
```typescript
cy.createIncidentViaAPI({
  reason: 'Анонимный инцидент',
  type: 'Вандализм',
  latitude: '59.9311',
  longitude: '30.3609',
  altitude: '10.5'
  // applicant не указан - будет null
});
```

## Создание дела через API

### Создание дела с участниками (с учетом совместимости ролей)

**ВАЖНО**: При назначении пользователей учитывайте таблицу совместимости ролей!

```typescript
cy.createIncidentViaAPI({ /* ... */ }).then((incidentId) => {
  cy.createCaseViaAPI(incidentId, {
    title: 'Дело о краже',
    description: 'Детальное описание дела',
    users: {
      '3': 'PATROL',        // officer_1 (PATROL) → PATROL ✓
      '5': 'INVESTIGATOR',  // investigator_1 (INVESTIGATOR) → INVESTIGATOR ✓
      '6': 'FORENSICS'      // forensics_1 (FORENSICS) → FORENSICS ✓
    }
  }).then((caseId) => {
    cy.log(`Created case with ID: ${caseId}`);
    // Теперь на /templates/create?case_id=${caseId} таблица Users покажет 3 участников
  });
});
```

### Совместимость ролей (из бэкенда)

| Primary Position | Может быть назначен как |
|-----------------|-------------------------|
| PATROL | PATROL, OFFICER |
| SENIOR_PATROL | PATROL, SENIOR_PATROL, COMMAND |
| INVESTIGATOR | INVESTIGATOR, LEAD_INVESTIGATOR, CLUES_ANALYSIS |
| SENIOR_INVESTIGATOR | INVESTIGATOR, SENIOR_INVESTIGATOR, CLUES_ANALYSIS |
| FORENSICS | FORENSICS, CLUES_ANALYSIS |
| DISPATCHER | DISPATCHER, OPERATOR |
| OPERATOR | OPERATOR |
| ANALYST | ANALYST |

### Тестовые пользователи (из e2e-test-data.sql)

```typescript
// ID 2: dispatcher (DISPATCHER)
// ID 3: officer_1 (PATROL)
// ID 4: officer_2 (SENIOR_PATROL)
// ID 5: investigator_1 (INVESTIGATOR)
// ID 6: forensics_1 (FORENSICS)
// ID 7: admin (OPERATOR)
// ID 8: viewer (ANALYST)
```

### Права тестовых пользователей

| Пользователь | Может создавать | Основное использование |
|--------------|----------------|------------------------|
| dispatcher | Инциденты, Дела | Создание инцидентов и дел |
| officer_1 | Улики, **Шаблоны** | ✅ Тесты шаблонов, загрузка файлов |
| officer_2 | Улики | Дополнительный участник |
| investigator_1 | Улики, Подозреваемых | Сбор улик, работа с подозреваемыми |
| forensics_1 | Улики | Криминалистическая экспертиза |
| admin | Всё | Полные права, логи |
| viewer | - | READ-only (негативные тесты) |

**Важно для тестов**:
- Тесты создания **шаблонов** должны использовать `officer_1` (имеет `CASES:*:TEMPLATE_ADD`)
- Тесты создания **инцидентов/дел** используют `dispatcher` (имеет `INCIDENTS:*:CASE_ADD`)
- Тесты создания **улик** могут использовать `officer_1`, `investigator_1`, или `forensics_1`

### Примеры правильных комбинаций

```typescript
// ✅ Правильно - совместимые роли
users: {
  '3': 'PATROL',           // officer_1 (PATROL) → PATROL
  '5': 'INVESTIGATOR'      // investigator_1 (INVESTIGATOR) → INVESTIGATOR
}

// ✅ Правильно - альтернативные роли
users: {
  '3': 'OFFICER',          // officer_1 (PATROL) → OFFICER
  '4': 'COMMAND',          // officer_2 (SENIOR_PATROL) → COMMAND
  '6': 'CLUES_ANALYSIS'    // forensics_1 (FORENSICS) → CLUES_ANALYSIS
}

// ❌ Неправильно - несовместимые роли
users: {
  '3': 'INVESTIGATOR',     // officer_1 (PATROL) ✗ INVESTIGATOR (не совместимо!)
  '8': 'PATROL'            // viewer (ANALYST) ✗ PATROL (не совместимо!)
}
```

### Создание дела без участников
```typescript
cy.createCaseViaAPI(incidentId, {
  title: 'Дело без участников',
  description: 'Описание',
  users: {}  // Пустой объект - таблица Users будет пустая
});
```

## Авторизация

```typescript
// Простая авторизация
cy.login('dispatcher', 'test123');

// С дальнейшими действиями
cy.login('admin', 'test123');
cy.visit('/incidents/create');
```

## Заполнение форм

### Форма инцидента
```typescript
cy.fillIncidentForm({
  reason: 'Описание инцидента',
  type: 'Кража',
  latitude: '59.9311',
  longitude: '30.3609',
  altitude: '10.5',
  applicant: 'Петров П.П.'
});
```

### Форма дела
```typescript
cy.fillCaseForm({
  title: 'Название дела',
  description: 'Описание дела'
});
```

### Форма улики
```typescript
cy.fillClueForm({
  title: 'Отпечатки пальцев',
  description: 'Найдены на месте преступления'
});
```

## Загрузка файлов

### Загрузка изображения
```typescript
cy.uploadFile('input[type="file"]', 'test-image.jpg');
```

### Загрузка PDF отчета
```typescript
cy.uploadFile('input[type="file"]', 'test-report.pdf');
```

## Комплексный пример: Создание полного инцидента с делом и уликами

```typescript
describe('Полный workflow создания инцидента', () => {
  it('Создает инцидент, дело и улику', () => {
    let incidentId: number;
    let caseId: number;

    // 1. Авторизация
    cy.login('dispatcher', 'test123');

    // 2. Создание инцидента
    cy.createIncidentViaAPI({
      reason: 'Кража велосипеда',
      type: 'Кража',
      latitude: '59.9311',
      longitude: '30.3609',
      altitude: '10.5',
      applicant: 'Сидоров С.С.',
      waitForAssignment: false
    }).then((id) => {
      incidentId = id;

      // 3. Создание дела
      cy.createCaseViaAPI(incidentId, {
        title: 'Дело о краже велосипеда',
        description: 'Расследование кражи велосипеда марки...',
        users: { '1': 'INVESTIGATOR' }
      }).then((id) => {
        caseId = id;

        // 4. Переход к созданию улики
        cy.visit(`/clues/new?case_id=${caseId}`);

        // 5. Заполнение формы улики
        cy.fillClueForm({
          title: 'Фотография с камеры наблюдения',
          description: 'Запись с камеры на момент кражи'
        });

        // 6. Загрузка файла
        cy.uploadFile('input[type="file"]', 'test-image.jpg');

        // 7. Отправка формы
        cy.contains('button', 'Submit').click();

        // 8. Проверка редиректа
        cy.url().should('include', `/cases/${caseId}`);
      });
    });
  });
});
```

## Работа с токеном авторизации

### Структура токена в localStorage

В localStorage под ключом `'token'` хранится JSON объект:

```typescript
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "refresh_token_here",
  "tokenType": "Bearer",
  "issueAtMS": 1702905600000,
  "expiresInMS": 3600000
}
```

### Правильное извлечение токена для API запросов

```typescript
// ❌ НЕПРАВИЛЬНО
const token = win.localStorage.getItem('token')
Authorization: `Bearer ${token}`  // Отправит весь JSON как строку!

// ✅ ПРАВИЛЬНО
const tokenStr = win.localStorage.getItem('token')
if (!tokenStr) throw new Error('No token found')
const tokenObj = JSON.parse(tokenStr)
const authHeader = `${tokenObj.tokenType} ${tokenObj.accessToken}`
Authorization: authHeader  // Отправит: "Bearer eyJhbGc..."
```

### Пример использования в кастомной команде

```typescript
cy.window().then((win) => {
  const tokenStr = win.localStorage.getItem('token');
  if (!tokenStr) throw new Error('No token found');

  const tokenObj = JSON.parse(tokenStr);
  const authHeader = `${tokenObj.tokenType} ${tokenObj.accessToken}`;

  cy.request({
    method: 'POST',
    url: '/api/some-endpoint',
    headers: {
      Authorization: authHeader,
      'Content-Type': 'application/json'
    },
    body: { /* ... */ }
  });
});
```

## Соответствие Backend DTO

### AddIncidentRequest
```java
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

### Mapping в команде createIncidentViaAPI
```typescript
{
  applicant: data.applicant || null,           // Nullable String
  type: data.type,                             // @NotNull @NotEmpty String
  reason: data.reason,                         // @NotNull @NotEmpty String
  locationLatitude: parseFloat(data.latitude), // @NotNull Double
  locationLongitude: parseFloat(data.longitude), // @NotNull Double
  locationAltitude: parseFloat(data.altitude),  // @NotNull Double
  estimatedTime: data.estimatedTime || new Date().toISOString(), // @NotNull Instant
  waitForAssignment: data.waitForAssignment !== undefined
    ? data.waitForAssignment
    : true                                     // @NotNull Boolean
}
```

## Ожидания (waits)

### Ожидание API запроса
```typescript
cy.wait('@createIncident', { timeout: 15000 });
```

### Ожидание элемента
```typescript
cy.get('.loading-spinner', { timeout: 10000 })
  .should('not.exist');
```

### Множественные ожидания
```typescript
cy.wait(['@createIncident', '@getRankedUsers']);
```

## Работа с перехватами (intercepts)

Все intercepts настроены автоматически в `beforeEach` в `commands.ts`:
- `@createIncident` - POST /api/incidents
- `@createCase` - POST /api/incidents/*/cases
- `@createClue` - POST /api/cases/*/clues
- `@uploadFile` - POST /api/files
- `@getRankedUsers` - GET /api/users/ranked*
- И другие...

Их можно использовать в тестах:
```typescript
cy.contains('button', 'Submit').click();
cy.wait('@createIncident').then((interception) => {
  expect(interception.response.statusCode).to.eq(200);
  expect(interception.response.body).to.have.property('id');
});
```
