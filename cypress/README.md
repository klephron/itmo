# E2E тесты для PAI Frontend

Этот документ описывает структуру E2E тестов, реализованных с использованием Cypress 15.7.0.

## 📋 Структура тестов

```
cypress/
├── e2e/
│   ├── auth/              # Тесты авторизации
│   │   └── login.cy.ts
│   ├── incidents/         # UC1: Создание инцидентов
│   │   └── create-incident.cy.ts
│   ├── clues/             # UC2: Работа с уликами
│   │   └── create-clue.cy.ts
│   ├── cases/             # UC9: Просмотр дел
│   │   └── view-cases.cy.ts
│   ├── notifications/     # UC4: Уведомления
│   │   └── notifications.cy.ts
│   ├── suspects/          # Работа с подозреваемыми
│   │   └── create-suspect.cy.ts
│   └── templates/         # UC3/UC12: Шаблоны отчетов
│       └── create-template.cy.ts
├── fixtures/              # Тестовые данные и файлы
│   ├── test-data.json
│   ├── test-image.png
│   ├── test-image.jpg
│   └── test-report.txt
├── support/               # Кастомные команды и конфигурация
│   ├── commands.ts
│   ├── e2e.ts
│   └── component.ts
└── downloads/             # Скачанные файлы (генерируется автоматически)
```

## 🎯 Покрытие тестами

### Реализованные Use Cases

| UC | Описание | Файл | Приоритет |
|----|----------|------|-----------|
| UC1 | Обработка вызовов (создание инцидентов) | `incidents/create-incident.cy.ts` | Критический |
| UC2 | Работа с уликами и анализами | `clues/create-clue.cy.ts` | Критический |
| UC3 | Формирование шаблона отчета | `templates/create-template.cy.ts` | Критический |
| UC4 | Уведомление участников дела | `notifications/notifications.cy.ts` | Высокий |
| UC9 | Чтение данных по делу | `cases/view-cases.cy.ts` | Высокий |
| UC12 | Сохранение отчетов в ЦБД | `templates/create-template.cy.ts` | Критический |
| AUTH | Авторизация и выход | `auth/login.cy.ts` | Критический |
| SUSPECT | Создание подозреваемых | `suspects/create-suspect.cy.ts` | Высокий |

## 🚀 Запуск тестов

### Предварительные требования

1. **Backend** должен быть запущен на `http://localhost:8081` (или настройте `apiUrl` в `cypress.config.ts`)
2. **Frontend** должен быть запущен на `http://localhost:5173` (или настройте `baseUrl` в `cypress.config.ts`)
3. База данных должна быть доступна и содержать тестовых пользователей

### Настройка портов

По умолчанию Cypress использует:
- `baseUrl: http://localhost:5173` - для открытия страниц фронтенда
- `apiUrl: http://localhost:8081` - для прямых API запросов к бэкенду

Если ваш бэкенд запущен на другом порту, измените в `cypress.config.ts`:
```typescript
env: {
  apiUrl: 'http://localhost:9000',  // Ваш порт бэкенда
}
```

Или передайте через командную строку:
```bash
npx cypress run --env apiUrl=http://localhost:9000
```

### Тестовые пользователи

**ВАЖНО**: Перед запуском тестов выполните SQL скрипт `e2e-test-data.sql` в базе данных!

Скрипт создаст следующих тестовых пользователей:

| ID | Username | Password | Primary Position | Используется в тестах |
|----|----------|----------|------------------|------------------------|
| 2 | dispatcher | test123 | DISPATCHER | Создание инцидентов и дел |
| 3 | officer_1 | test123 | PATROL | ✅ Участник дел, **создание шаблонов** |
| 4 | officer_2 | test123 | SENIOR_PATROL | Дополнительный участник |
| 5 | investigator_1 | test123 | INVESTIGATOR | ✅ Участник дел |
| 6 | forensics_1 | test123 | FORENSICS | Работа с уликами |
| 7 | admin | test123 | OPERATOR | Все права (включая LOGS:*:READ) |
| 8 | viewer | test123 | ANALYST | READ-only (негативные тесты) |

**Важно**:
- `dispatcher` **НЕ** имеет права создавать шаблоны (`CASES:*:TEMPLATE_ADD`)
- `officer_1` имеет права: `CASES:*:TEMPLATE_ADD`, `TEMPLATES:*:READ`, `FILES:*:UPLOAD`
- Тесты шаблонов используют `officer_1`, а не `dispatcher`

**Совместимость ролей**: При создании дел через API учитывайте таблицу совместимости:
- **PATROL** может быть назначен как: `PATROL`, `OFFICER`
- **INVESTIGATOR** может быть назначен как: `INVESTIGATOR`, `LEAD_INVESTIGATOR`, `CLUES_ANALYSIS`
- **FORENSICS** может быть назначен как: `FORENSICS`, `CLUES_ANALYSIS`

См. `cypress/support/EXAMPLES.md` для подробной информации о совместимости ролей.

### Команды запуска

#### Интерактивный режим (для разработки)
```bash
npm run test:e2e:open
```
Открывает Cypress UI, где можно выбрать и запустить отдельные тесты.

#### Headless режим (для CI/CD)
```bash
npm run test:e2e
```
Запускает все тесты в headless режиме.

#### Запуск конкретного теста
```bash
npx cypress run --spec "cypress/e2e/incidents/create-incident.cy.ts"
```

#### Запуск тестов с определенным браузером
```bash
npx cypress run --browser chrome
npx cypress run --browser firefox
npx cypress run --browser edge
```

## 🔧 Кастомные команды

Для упрощения написания тестов созданы следующие кастомные команды:

### `cy.login(username, password)`
Выполняет авторизацию пользователя.

```typescript
cy.login('dispatcher', 'test123');
```

### `cy.fillIncidentForm(data)`
Заполняет форму создания инцидента.

```typescript
cy.fillIncidentForm({
  reason: 'Кража',
  type: 'Кража',
  latitude: '59.9311',
  longitude: '30.3609',
  altitude: '10.5',
  applicant: 'Иванов И.И.'
});
```

### `cy.createIncidentViaAPI(data)`
Создает инцидент через API (для подготовки тестовых данных).

```typescript
cy.createIncidentViaAPI({...}).then((incidentId) => {
  // Используем созданный инцидент
});
```

### `cy.createCaseViaAPI(incidentId, data)`
Создает дело через API.

```typescript
cy.createCaseViaAPI(incidentId, {
  title: 'Дело',
  description: 'Описание',
  users: {}
}).then((caseId) => {
  // Используем созданное дело
});
```

### `cy.uploadFile(selector, filePath)`
Загружает файл из cypress/fixtures/.

```typescript
cy.uploadFile('input[type="file"]', 'test-image.jpg');
```

## 📊 Конфигурация

### Таймауты

```javascript
defaultCommandTimeout: 10000,  // 10 секунд
requestTimeout: 15000,          // 15 секунд для API
responseTimeout: 15000,         // 15 секунд для ответов
pageLoadTimeout: 30000,         // 30 секунд для загрузки страницы
```

### Retry стратегия

```javascript
retries: {
  runMode: 2,    // 2 попытки в headless режиме
  openMode: 0,   // 0 попыток в интерактивном режиме
}
```

## 🐛 Отладка

### Скриншоты
При падении теста автоматически создается скриншот в `cypress/screenshots/`.

### Видеозапись
Видеозапись отключена по умолчанию (`video: false`). Для включения измените в `cypress.config.ts`:
```javascript
video: true
```

### Console logs
Все console.log из приложения видны в консоли Cypress.

### Просмотр в браузере
В интерактивном режиме можно:
- Пошагово выполнять команды
- Смотреть состояние DOM после каждой команды
- Использовать time-travel для отладки

## 📝 Лучшие практики

1. **Используйте data-testid** для селекторов (когда возможно)
2. **Избегайте хрупких селекторов** (классы, nth-child)
3. **Используйте кастомные команды** для переиспользуемого кода
4. **Создавайте независимые тесты** (каждый тест должен работать сам по себе)
5. **Очищайте данные** в beforeEach (clearCookies, clearLocalStorage)
6. **Используйте fixtures** для тестовых данных
7. **Ждите ответов от API** с помощью cy.wait('@alias')

## 🔍 Проверка тестов

### Проверка качества
```bash
# Запуск всех тестов
npm run test:e2e

# Проверка только критических тестов
npx cypress run --spec "cypress/e2e/incidents/**,cypress/e2e/clues/**,cypress/e2e/templates/**"
```

### CI/CD Integration
Тесты готовы для запуска в CI/CD:
```yaml
# Пример для GitHub Actions
- name: Run E2E tests
  run: npm run test:e2e
```

## 📚 Дополнительные ресурсы

- [Примеры использования команд](support/EXAMPLES.md) - Подробные примеры всех кастомных команд
- [История изменений](CHANGELOG.md) - Changelog с описанием обновлений
- [Cypress Documentation](https://docs.cypress.io/)
- [План тестирования](../docs/UITestingPlan.md)
- [Use Cases](../docs/UseCases.md)
- [Функциональные требования](../docs/funcRequirements.md)

## ✅ Статус реализации

- ✅ UC1: Обработка вызовов
- ✅ UC2: Работа с уликами
- ✅ UC3: Формирование шаблонов
- ✅ UC4: Уведомления
- ✅ UC9: Просмотр дел
- ✅ UC12: Сохранение в ЦБД
- ✅ Авторизация
- ✅ Создание подозреваемых
- ⏳ UC5: Автоназначение ресурсов (частично покрыто в UC1)
- ⏳ UC6-8, UC10-11: Требуют доработки

---

**Автор**: Claude Sonnet 4.5
**Дата**: 2025-12-15
**Версия**: 1.0.0
