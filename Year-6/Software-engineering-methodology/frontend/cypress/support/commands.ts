/**
 * Кастомные команды Cypress для E2E тестирования
 * Упрощают написание тестов и повышают их читаемость
 */

/// <reference types="cypress" />

// ==============================================
// Типы для TypeScript
// ==============================================

declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * Авторизация пользователя
       * @param username - имя пользователя
       * @param password - пароль
       * @example cy.login('dispatcher', 'test123')
       */
      login(username: string, password: string): Chainable<void>

      /**
       * Заполнение формы создания инцидента
       * @param data - данные инцидента
       * @example cy.fillIncidentForm({ reason: 'Кража', type: 'Кража', ... })
       */
      fillIncidentForm(data: IncidentFormData): Chainable<void>

      /**
       * Заполнение формы создания дела
       * @param data - данные дела
       * @example cy.fillCaseForm({ title: 'Дело', description: 'Описание' })
       */
      fillCaseForm(data: CaseFormData): Chainable<void>

      /**
       * Заполнение формы создания улики
       * @param data - данные улики
       * @example cy.fillClueForm({ title: 'Улика', description: 'Описание' })
       */
      fillClueForm(data: ClueFormData): Chainable<void>

      /**
       * Создание инцидента через API
       * @param data - данные инцидента
       * @param data.waitForAssignment - ожидать назначения (по умолчанию true)
       * @param data.estimatedTime - время инцидента в ISO формате (по умолчанию текущее время)
       * @returns ID созданного инцидента
       * @example cy.createIncidentViaAPI({ reason: '...', type: '...', ... }).then(id => { ... })
       */
      createIncidentViaAPI(data: IncidentFormData): Chainable<number>

      /**
       * Создание дела через API
       * @param incidentId - ID инцидента
       * @param data - данные дела
       * @returns ID созданного дела
       * @example cy.createCaseViaAPI(1, { ... }).then(id => { ... })
       */
      createCaseViaAPI(incidentId: number, data: CaseAPIData): Chainable<number>

      /**
       * Загрузка файла
       * @param selector - селектор input[type="file"]
       * @param filePath - путь к файлу в cypress/fixtures/
       * @example cy.uploadFile('input[type="file"]', 'test-image.jpg')
       */
      uploadFile(selector: string, filePath: string): Chainable<void>
    }
  }
}

// Типы данных
interface IncidentFormData {
  reason: string
  type: string
  latitude: string
  longitude: string
  altitude: string
  applicant?: string
  waitForAssignment?: boolean
  estimatedTime?: string
}

interface CaseFormData {
  title: string
  description: string
}

interface ClueFormData {
  title: string
  description: string
}

interface CaseAPIData {
  title: string
  description: string
  users: Record<string, string>
}

// ==============================================
// КОМАНДЫ
// ==============================================

/**
 * Авторизация пользователя
 */
Cypress.Commands.add('login', (username: string, password: string) => {
  cy.visit('/login')
  cy.get('input[type="text"]').clear().type(username)
  cy.get('input[type="password"]').clear().type(password)
  cy.contains('button', 'Login').click()

  // Ждем редиректа (успешная авторизация)
  cy.url().should('not.include', '/login', { timeout: 10000 })

  // Проверяем наличие токена
  cy.window().its('localStorage').invoke('getItem', 'token').should('exist')


})

/**
 * Заполнение формы создания инцидента
 */
Cypress.Commands.add('fillIncidentForm', (data: IncidentFormData) => {
  cy.get('textarea').first().clear().type(data.reason)
  cy.get('input[type="input"]').eq(0).clear().type(data.type)
  cy.get('input[type="input"]').eq(1).clear().type(data.latitude)
  cy.get('input[type="input"]').eq(2).clear().type(data.longitude)
  cy.get('input[type="input"]').eq(3).clear().type(data.altitude)

  if (data.applicant) {
    cy.get('input[type="input"]').eq(4).clear().type(data.applicant)
  }


})

/**
 * Заполнение формы создания дела
 */
Cypress.Commands.add('fillCaseForm', (data: CaseFormData) => {
  // Находим поля через placeholder или другие атрибуты
  cy.get('input[type="input"]').first().clear().type(data.title)
  cy.get('textarea').first().clear().type(data.description)


})

/**
 * Заполнение формы создания улики
 */
Cypress.Commands.add('fillClueForm', (data: ClueFormData) => {
  cy.get('input[type="input"]').first().clear().type(data.title)
  cy.get('textarea').first().clear().type(data.description)


})

/**
 * Создание инцидента через API (для подготовки тестовых данных)
 */
Cypress.Commands.add('createIncidentViaAPI', (data: IncidentFormData) => {
  return cy.window().then((win) => {
    const tokenStr = win.localStorage.getItem('token')
    if (!tokenStr) {
      throw new Error('No token found in localStorage')
    }

    const tokenObj = JSON.parse(tokenStr)
    const authHeader = `${tokenObj.tokenType} ${tokenObj.accessToken}`

    const apiUrl = Cypress.env('apiUrl') || 'http://localhost:8081'

    return cy.request({
      method: 'POST',
      url: `${apiUrl}/api/incidents`,
      headers: {
        Authorization: authHeader,
        'Content-Type': 'application/json'
      },
      body: {
        reason: data.reason,
        type: data.type,
        locationLatitude: parseFloat(data.latitude),
        locationLongitude: parseFloat(data.longitude),
        locationAltitude: parseFloat(data.altitude),
        applicant: data.applicant || null,
        estimatedTime: data.estimatedTime || new Date().toISOString(),
        waitForAssignment: data.waitForAssignment !== undefined ? data.waitForAssignment : true
      }
    }).then((response) => {
      expect(response.status).to.eq(200)
      const incidentId = response.body.id


      return cy.wrap(incidentId)
    })
  })
})

/**
 * Создание дела через API
 */
Cypress.Commands.add('createCaseViaAPI', (incidentId: number, data: CaseAPIData) => {
  return cy.window().then((win) => {
    const tokenStr = win.localStorage.getItem('token')
    if (!tokenStr) {
      throw new Error('No token found in localStorage')
    }

    const tokenObj = JSON.parse(tokenStr)
    const authHeader = `${tokenObj.tokenType} ${tokenObj.accessToken}`

    const apiUrl = Cypress.env('apiUrl') || 'http://localhost:8081'

    return cy.request({
      method: 'POST',
      url: `${apiUrl}/api/incidents/${incidentId}/case`,
      headers: {
        Authorization: authHeader,
        'Content-Type': 'application/json'
      },
      body: {
        title: data.title,
        description: data.description,
        users: data.users
      }
    }).then((response) => {
      expect(response.status).to.eq(200)
      const caseId = response.body.id


      return cy.wrap(caseId)
    })
  })
})

/**
 * Загрузка файла
 */
Cypress.Commands.add('uploadFile', (selector: string, filePath: string) => {
  cy.get(selector).selectFile(`cypress/fixtures/${filePath}`, { force: true })


})

// ==============================================
// ДОПОЛНИТЕЛЬНЫЕ УТИЛИТЫ
// ==============================================

/**
 * Перехват сетевых запросов для более надежных тестов
 * ПРИМЕЧАНИЕ: Эти intercepts автоматически применяются ко всем тестам
 */
beforeEach(() => {
  // Перехватываем API запросы для мониторинга и отладки
  cy.intercept('POST', '/api/incidents').as('createIncident')
  cy.intercept('POST', '/api/incidents/*/cases').as('createCase')
  cy.intercept('POST', '/api/cases/*/clues').as('createClue')
  cy.intercept('POST', '/api/files').as('uploadFile')
  cy.intercept('GET', '/api/users/ranked*').as('getRankedUsers')
  cy.intercept('GET', '/api/incidents/*').as('getIncident')
  cy.intercept('GET', '/api/cases/*').as('getCase')
  cy.intercept('POST', '/api/auth/login').as('login')
  cy.intercept('GET', '/api/notifications*').as('getNotifications')
  cy.intercept('PATCH', '/api/notifications/*').as('updateNotification')

  // Логируем все неперехваченные запросы (для отладки)
  cy.intercept('**', (req) => {
    if (!req.alias) {

    }
  })
})

// Export для TypeScript
export {}
