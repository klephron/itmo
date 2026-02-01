// Import commands.js using ES2015 syntax:
import './commands';

// Alternatively you can use CommonJS syntax:
// require('./commands')

// ==============================================
// ОБРАБОТКА ОШИБОК
// ==============================================

/**
 * Игнорируем React hydration ошибки в тестах
 * Эти ошибки возникают из-за SSR в React Router 7 и не влияют на функциональность
 */
Cypress.on('uncaught:exception', (err, runnable) => {
  // Игнорируем React hydration mismatch ошибки
  if (err.message.includes('Hydration failed') ||
      err.message.includes('hydration mismatch') ||
      err.message.includes('server rendered HTML')) {
    console.warn('⚠️Ignoring React hydration error:', err.message)
    return false // Не фейлить тест
  }

  // Игнорируем ResizeObserver ошибки (распространенная ошибка в тестах)
  if (err.message.includes('ResizeObserver loop')) {
    console.warn('⚠️ Ignoring ResizeObserver error')
    return false
  }

  // Все остальные ошибки должны фейлить тест
  return true
})

// ==============================================
// ГЛОБАЛЬНЫЕ НАСТРОЙКИ
// ==============================================

// Очистка перед каждым тестом
beforeEach(() => {
  // Очищаем localStorage и cookies перед каждым тестом
  // (если не переопределено в конкретном тесте)
  if (!Cypress.currentTest.titlePath.includes('preserves session')) {
    cy.clearLocalStorage()
    cy.clearCookies()
  }
})

// Prevent TypeScript errors on custom commands
declare global {
  namespace Cypress {
    interface Chainable {
      login(username: string, password: string): Chainable<void>;
    }
  }
}
