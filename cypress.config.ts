import { defineConfig } from 'cypress';

export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:5173',
    supportFile: 'cypress/support/e2e.ts',
    specPattern: 'cypress/e2e/**/*.cy.{js,jsx,ts,tsx}',
    viewportWidth: 1280,
    viewportHeight: 720,
    video: false,
    screenshotOnRunFailure: true,

    // URL бэкенда для API запросов
    env: {
      apiUrl: 'http://localhost:8081',
    },

    // Увеличенные таймауты для SSR приложений
    defaultCommandTimeout: 10000,  // 10 секунд (было 4000)
    requestTimeout: 15000,          // 15 секунд для API запросов
    responseTimeout: 15000,         // 15 секунд для ответов
    pageLoadTimeout: 30000,         // 30 секунд для загрузки страницы

    // Retry стратегия для нестабильных тестов
    retries: {
      runMode: 2,    // 2 попытки в headless режиме
      openMode: 0,   // 0 попыток в интерактивном режиме (для отладки)
    },

    // Экспериментальные фичи
    experimentalRunAllSpecs: true,  // Позволяет запускать все спеки сразу

    setupNodeEvents(on, config) {
      // Логирование для отладки
      on('task', {
        log(message) {
          console.log(message)
          return null
        },
      })

      return config
    },
  },
  component: {
    devServer: {
      framework: 'react',
      bundler: 'vite',
    },
    specPattern: 'cypress/component/**/*.cy.{js,jsx,ts,tsx}',
    supportFile: 'cypress/support/component.ts',
  },
});
