/**
 * E2E тесты для UC1: Обработка вызовов (создание инцидентов)
 *
 * Тестируемые сценарии:
 * - UC1-001: Создание инцидента с автоматическим назначением (Wait for Assignment)
 * - UC1-002: Создание инцидента с немедленным созданием дела
 * - UC1-003: Валидация пустых обязательных полей
 * - UC1-004: Валидация невалидных координат
 * - UC1-005: Проверка прав доступа на создание инцидента
 */

describe('UC1: Обработка вызовов - Создание инцидентов', () => {
  beforeEach(() => {
    cy.clearCookies();
    cy.clearLocalStorage();
  });

  describe('UC1-001: Создание инцидента с Wait for Assignment', () => {
    it('Должен успешно создать инцидент с ожиданием назначения', () => {
      // Авторизация
      cy.visit('/login');
      cy.get('input[type="text"]').type('dispatcher');
      cy.get('input[type="password"]').type('test123');
      cy.contains('button', 'Login').click();
      cy.url().should('not.include', '/login');

      // Переход к форме
      cy.visit('/incidents/create');
      cy.get('h2').should('contain', 'Create Incident');

      // Заполнение формы
      cy.get('textarea').first().type('Кража личного имущества в парке на ул. Пушкина');
      cy.get('input[type="input"]').eq(0).type('Кража');
      cy.get('input[type="input"]').eq(1).type('59.9311');
      cy.get('input[type="input"]').eq(2).type('30.3609');
      cy.get('input[type="input"]').eq(3).type('10.5');
      cy.get('input[type="input"]').eq(4).type('Иванов Иван Иванович');

      // Установка флага
      cy.get('#waitForAssignment').check().should('be.checked');

      // Отправка
      cy.contains('button', 'Submit').click();

      // Проверка результата
      cy.url().should('match', /\/incidents\/\d+$/);
      cy.contains('Кража личного имущества в парке').should('be.visible');
    });
  });

  describe('UC1-002: Создание инцидента с немедленным созданием дела', () => {
    it('Должен создать инцидент и дело с выбранными участниками', () => {
      // Авторизация
      cy.visit('/login');
      cy.get('input[type="text"]').type('dispatcher');
      cy.get('input[type="password"]').type('test123');
      cy.contains('button', 'Login').click();
      cy.url().should('not.include', '/login');

      // Форма инцидента
      cy.visit('/incidents/create');
      cy.get('textarea').first().type('Ограбление магазина на Невском проспекте');
      cy.get('input[type="input"]').eq(0).type('Ограбление');
      cy.get('input[type="input"]').eq(1).type('59.9343');
      cy.get('input[type="input"]').eq(2).type('30.3351');
      cy.get('input[type="input"]').eq(3).type('5.0');
      cy.get('input[type="input"]').eq(4).type('Администратор магазина');

      // Отключаем Wait for Assignment → появляется форма дела
      cy.get('#waitForAssignment').uncheck().should('not.be.checked');
      cy.contains('Case details').should('be.visible');

      // Загрузка пользователей
      cy.contains('button', 'Load ranked').click();
      cy.get('table').should('be.visible');
      cy.get('tbody tr').should('have.length.greaterThan', 0);

      cy.get('tbody tr')
        .filter((index, el) => {
          const position = el.querySelectorAll('td')[2]?.textContent?.trim();
          return position === 'PATROL' || position === 'INVESTIGATOR';
        })
        .first()
        .find('input[type="checkbox"]')
        .check()
        .should('be.checked');

      // Отправка
      cy.contains('button', 'Submit').click();

      // Проверка результата
      cy.url().should('match', /\/cases\/\d+$/);
      cy.contains('Ограбление магазина на Невском проспекте').should('be.visible');


    });
  });

  describe('UC1-003: Валидация формы инцидента', () => {
    it('Должен показать ошибку при пустых обязательных полях', () => {
      // Авторизация
      cy.visit('/login');
      cy.get('input[type="text"]').type('dispatcher');
      cy.get('input[type="password"]').type('test123');
      cy.contains('button', 'Login').click();
      cy.url().should('not.include', '/login');
      cy.visit('/incidents/create');

      cy.contains('Submit').click();

      cy.contains('Type and Reason are required').should('be.visible');
      cy.url().should('include', '/incidents/create');
    });

    it('Должен показать ошибку при невалидных координатах', () => {
      cy.visit('/login');
      cy.get('input[type="text"]').type('dispatcher');
      cy.get('input[type="password"]').type('test123');
      cy.contains('button', 'Login').click();

      cy.visit('/incidents/create');
      cy.get('textarea').first().type('Тестовый инцидент');
      cy.get('input[type="input"]').eq(0).type('Тест');
      cy.get('input[type="input"]').eq(1).type('invalid_latitude');
      cy.get('input[type="input"]').eq(2).type('invalid_longitude');
      cy.get('input[type="input"]').eq(3).type('invalid_altitude');

      cy.contains('button', 'Submit').click();
      cy.contains('Coordinates must be numbers').should('be.visible');
    });
  });

  describe('UC1-004: Проверка прав доступа на создание инцидента', () => {
    it('Должен запретить создание инцидента пользователю без прав', () => {
      cy.visit('/login');
      cy.get('input[type="text"]').type('viewer');
      cy.get('input[type="password"]').type('test123');
      cy.contains('button', 'Login').click();

      cy.visit('/incidents/create');
      cy.contains('Insufficient permissions').should('be.visible');
      cy.contains('button', 'Submit').should('be.disabled');


    });

    it('Должен разрешить создание инцидента пользователю с правами', () => {
      cy.visit('/login');
      cy.get('input[type="text"]').type('dispatcher');
      cy.get('input[type="password"]').type('test123');
      cy.contains('button', 'Login').click();

      cy.visit('/incidents/create');
      cy.contains('Insufficient permissions').should('not.exist');
      cy.contains('button', 'Submit').should('not.be.disabled');


    });
  });

  describe('UC1-005: Отображение ranked users', () => {
    it('Должен корректно отобразить ранжированных пользователей', () => {
      cy.visit('/login');
      cy.get('input[type="text"]').type('dispatcher');
      cy.get('input[type="password"]').type('test123');
      cy.contains('button', 'Login').click();

      cy.visit('/incidents/create');
      cy.get('textarea').first().type('Тест');
      cy.get('input[type="input"]').eq(0).type('Тест');
      cy.get('input[type="input"]').eq(1).type('59.9343');
      cy.get('input[type="input"]').eq(2).type('30.3351');
      cy.get('input[type="input"]').eq(3).type('10.0');

      cy.get('#waitForAssignment').uncheck();
      cy.contains('button', 'Load ranked').click();

      cy.get('table').should('be.visible');
      cy.get('table thead')
        .should('contain', 'User')
        .and('contain', 'Distance (m)')
        .and('contain', 'Active cases')
        .and('contain', 'Score');

      cy.get('tbody tr').should('have.length.greaterThan', 0)
    });
  });
});
