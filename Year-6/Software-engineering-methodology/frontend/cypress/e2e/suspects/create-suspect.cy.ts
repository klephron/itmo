/**
 * E2E тесты для работы с подозреваемыми
 *
 * Тестируемые сценарии:
 * - SUSPECT-001: Создание подозреваемого с полными данными
 * - SUSPECT-002: Создание подозреваемого с минимальными данными
 * - SUSPECT-003: Выбор статуса подозреваемого
 */

describe('Работа с подозреваемыми', () => {
  let testCaseId: number;

  beforeEach(() => {
    cy.clearCookies();
    cy.clearLocalStorage();
  });

  // Подготовка: создаем тестовое дело
  before(() => {
    cy.clearCookies();
    cy.clearLocalStorage();

    cy.login('dispatcher', 'test123');

    cy.createIncidentViaAPI({
      reason: 'Тестовый инцидент для подозреваемых',
      type: 'Кража',
      latitude: '59.9311',
      longitude: '30.3609',
      altitude: '10.5',
      applicant: 'Заявитель',
    }).then((incidentId) => {
      cy.createCaseViaAPI(incidentId, {
        title: 'Тестовое дело для подозреваемых',
        description: 'Описание',
        users: {
          '3': 'PATROL',           // officer_1 (PATROL) → PATROL
          '5': 'INVESTIGATOR'      // investigator_1 (INVESTIGATOR) → INVESTIGATOR
        },
      }).then((caseId) => {
        testCaseId = caseId;
        cy.log(`Created test case with ID: ${caseId}`);
      });
    });
  });

  describe('SUSPECT-001: Создание подозреваемого с полными данными', () => {
    it('Должен успешно создать подозреваемого со всеми заполненными полями', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/suspects/create?case_id=${testCaseId}`);

      // Генерируем уникальный identifier
      const uniqueId = `TEST${Date.now()}`;

      // Проверка формы
      cy.get('h2').should('contain', 'Create Suspect');

      // Заполнение полей
      cy.contains('h3', 'Name')
        .parent()
        .find('input')
        .type('Иван');

      cy.contains('h3', 'Surname')
        .parent()
        .find('input')
        .type('Петров');

      cy.contains('h3', 'Patronymic')
        .parent()
        .find('input')
        .type('Сергеевич');

      cy.contains('h3', 'Identifier')
        .parent()
        .find('input')
        .type(uniqueId);

      cy.contains('h3', 'Verdict')
        .parent()
        .find('input')
        .type('Невиновен');

      cy.contains('h3', 'Accusation')
        .parent()
        .find('textarea')
        .type('Подозревается в совершении кражи');

      // Выбор статуса
      cy.contains('h3', 'Status')
        .parent()
        .find('select')
        .select('ACCUSED');

      // Отправка формы
      cy.contains('button', 'Submit').click();

      // Проверка редиректа
      cy.url().should('include', `/cases/${testCaseId}`);

      // Переходим к просмотру подозреваемых
      cy.contains('button', 'Suspects').click();

      // Проверяем, что подозреваемый создан
      cy.contains('Иван').should('be.visible');
      cy.contains('Петров').should('be.visible');
      cy.contains(uniqueId).should('be.visible');
    });
  });

  describe('SUSPECT-002: Создание подозреваемого с минимальными данными', () => {
    it('Должен успешно создать подозреваемого с обязательными полями', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/suspects/create?case_id=${testCaseId}`);

      // Генерируем уникальный identifier
      const uniqueId = `TEST${Date.now()}`;

      // Заполняем только обязательные поля
      cy.contains('h3', 'Name')
        .parent()
        .find('input')
        .type('Мария');

      cy.contains('h3', 'Surname')
        .parent()
        .find('input')
        .type('Сидорова');

      cy.contains('h3', 'Identifier')
        .parent()
        .find('input')
        .type(uniqueId);

      cy.contains('h3', 'Accusation')
        .parent()
        .find('textarea')
        .type('Подозревается в хищении');

      cy.contains('button', 'Submit').click();

      // Проверка редиректа
      cy.url().should('include', `/cases/${testCaseId}`);
    });
  });

  describe('SUSPECT-003: Выбор статуса подозреваемого', () => {
    it('Должен отобразить все доступные статусы', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/suspects/create?case_id=${testCaseId}`);

      // Проверяем, что select содержит все статусы
      cy.contains('h3', 'Status')
        .parent()
        .find('select option')
        .should('have.length.greaterThan', 0);

      // Проверяем наличие основных статусов
      cy.contains('h3', 'Status')
        .parent()
        .find('select')
        .find('option')
        .should('contain', 'ACCUSED');
    });

    it('Должен позволить выбрать статус из списка', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/suspects/create?case_id=${testCaseId}`);

      // Выбираем статус
      cy.contains('h3', 'Status')
        .parent()
        .find('select')
        .select('ACCUSED')
        .should('have.value', 'ACCUSED');
    });
  });

  describe('SUSPECT-004: Проверка формы создания', () => {
    it('Должен отобразить все необходимые поля формы', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/suspects/create?case_id=${testCaseId}`);

      // Проверка всех полей
      cy.contains('h3', 'Name').should('be.visible');
      cy.contains('h3', 'Surname').should('be.visible');
      cy.contains('h3', 'Patronymic').should('be.visible');
      cy.contains('h3', 'Identifier').should('be.visible');
      cy.contains('h3', 'Status').should('be.visible');
      cy.contains('h3', 'Verdict').should('be.visible');
      cy.contains('h3', 'Accusation').should('be.visible');

      // Проверка кнопки Submit
      cy.contains('button', 'Submit').should('be.visible');
    });

    it('Должен корректно принимать ввод во все поля', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/suspects/create?case_id=${testCaseId}`);

      // Вводим данные
      cy.contains('h3', 'Name')
        .parent()
        .find('input')
        .type('Тест')
        .should('have.value', 'Тест');

      cy.contains('h3', 'Surname')
        .parent()
        .find('input')
        .type('Тестов')
        .should('have.value', 'Тестов');

      cy.contains('h3', 'Patronymic')
        .parent()
        .find('input')
        .type('Тестович')
        .should('have.value', 'Тестович');

      const testId = `TEST${Date.now()}`;
      cy.contains('h3', 'Identifier')
        .parent()
        .find('input')
        .type(testId)
        .should('have.value', testId);

      cy.contains('h3', 'Verdict')
        .parent()
        .find('input')
        .type('Виновен')
        .should('have.value', 'Виновен');

      cy.contains('h3', 'Accusation')
        .parent()
        .find('textarea')
        .type('Обвиняется в...')
        .should('have.value', 'Обвиняется в...');
    });
  });
});
