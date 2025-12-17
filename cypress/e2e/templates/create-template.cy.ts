/**
 * E2E тесты для UC3: Формирование шаблона отчета (FR 3.1.10)
 * и UC12: Сохранение отчетов Системой в ЦБД (FR 3.1.11)
 *
 * Тестируемые сценарии:
 * - UC3-001: Создание шаблона отчета с выбором данных дела
 * - UC3-002: Валидация обязательных полей
 * - UC3-003: Автоматическая загрузка данных дела
 * - UC12-001: Автоматическая загрузка шаблона в ЦБД
 */

describe('UC3/UC12: Формирование и сохранение шаблонов отчетов', () => {
  let testCaseId: number;

  beforeEach(() => {
    cy.clearCookies();
    cy.clearLocalStorage();
  });

  // Подготовка: создаем тестовое дело с участниками, уликами и подозреваемыми
  before(() => {
    cy.clearCookies();
    cy.clearLocalStorage();

    cy.login('dispatcher', 'test123');

    // Создаем инцидент
    cy.createIncidentViaAPI({
      reason: 'Тестовый инцидент для шаблона',
      type: 'Кража',
      latitude: '59.9311',
      longitude: '30.3609',
      altitude: '10.5',
      applicant: 'Заявитель',
    }).then((incidentId) => {
      // Создаем дело с участниками
      cy.createCaseViaAPI(incidentId, {
        title: 'Тестовое дело для шаблона',
        description: 'Описание дела',
        users: {
          '3': 'PATROL',           // officer_1 (PATROL) → PATROL
          '5': 'INVESTIGATOR'      // investigator_1 (INVESTIGATOR) → INVESTIGATOR
        },
      }).then((caseId) => {
        testCaseId = caseId;
        cy.log(`Created test case with ID: ${caseId}`);

        // Создаем подозреваемого через API
        cy.window().then((win) => {
          const tokenStr = win.localStorage.getItem('token');
          if (!tokenStr) throw new Error('No token found');
          const tokenObj = JSON.parse(tokenStr);
          const authHeader = `${tokenObj.tokenType} ${tokenObj.accessToken}`;

          const apiUrl = Cypress.env('apiUrl') || 'http://localhost:8081';

          // Генерируем уникальный identifier используя timestamp
          const uniqueIdentifier = `TEST${Date.now()}`;

          cy.request({
            method: 'POST',
            url: `${apiUrl}/api/cases/${caseId}/suspects`,
            headers: {
              Authorization: authHeader,
              'Content-Type': 'application/json',
            },
            body: {
              name: 'Иван',
              surname: 'Подозреваемый',
              patronymic: 'Иванович',
              identifier: uniqueIdentifier,
              accusation: 'Кража',
              status: 'ACCUSED',
              verdict: '',
            },
          }).then((response) => {
            cy.log(`Created suspect with ID: ${response.body.id}, identifier: ${uniqueIdentifier}`);
          });
        });

        // Создаем улику через API
        cy.window().then((win) => {
          const tokenStr = win.localStorage.getItem('token');
          if (!tokenStr) throw new Error('No token found');
          const tokenObj = JSON.parse(tokenStr);
          const authHeader = `${tokenObj.tokenType} ${tokenObj.accessToken}`;

          const apiUrl = Cypress.env('apiUrl') || 'http://localhost:8081';

          cy.request({
            method: 'POST',
            url: `${apiUrl}/api/cases/${caseId}/clues`,
            headers: {
              Authorization: authHeader,
              'Content-Type': 'application/json',
            },
            body: {
              title: 'Тестовая улика',
              description: 'Описание улики',
              status: 'PENDING',
            },
          }).then((response) => {
            cy.log(`Created clue with ID: ${response.body.id}`);
          });
        });
      });
    });
  });

  describe('UC3-001: Создание шаблона отчета', () => {
    it('Должен отобразить форму создания шаблона', () => {
      cy.login('officer_1', 'test123');
      cy.visit(`/templates/create?case_id=${testCaseId}`);

      // Проверка формы
      cy.contains('h2', 'Create Template').should('be.visible');

      // Проверка полей
      cy.contains('h3', 'Title').should('be.visible');
      cy.contains('h3', 'Description').should('be.visible');
      cy.contains('h3', 'Users').should('be.visible');
      cy.contains('h3', 'Clues').should('be.visible');
      cy.contains('h3', 'Suspects').should('be.visible');

      cy.contains('button', 'Submit').should('be.visible');
    });

    it('Должен автоматически загрузить данные дела', () => {
      cy.login('officer_1', 'test123');
      cy.visit(`/templates/create?case_id=${testCaseId}`);

      cy.wait(2000); // Даем время на загрузку данных

      // Проверяем, что таблицы загрузились
      cy.contains('h3', 'Users')
        .parent()
        .should('not.contain', 'Loading...');

      cy.contains('h3', 'Clues')
        .parent()
        .should('not.contain', 'Loading...');

      cy.contains('h3', 'Suspects')
        .parent()
        .should('not.contain', 'Loading...');

      // Проверяем, что данные отображаются
      cy.contains('h3', 'Suspects')
        .parent()
        .find('table tbody tr')
        .should('have.length.greaterThan', 0);

      cy.contains('h3', 'Clues')
        .parent()
        .find('table tbody tr')
        .should('have.length.greaterThan', 0);
    });

    it('Должен позволить выбрать/снять выбор пользователей, улик и подозреваемых', () => {
      cy.login('officer_1', 'test123');
      cy.visit(`/templates/create?case_id=${testCaseId}`);

      cy.wait(2000);

      // Проверяем, что чекбоксы по умолчанию checked
      cy.contains('h3', 'Users')
        .parent()
        .find('table tbody tr')
        .first()
        .find('input[type="checkbox"]')
        .should('be.checked');

      // Снимаем выбор с первого пользователя (если есть)
      cy.contains('h3', 'Users')
        .parent()
        .find('table tbody tr')
        .first()
        .find('input[type="checkbox"]')
        .uncheck()
        .should('not.be.checked');

      // Снова выбираем
      cy.contains('h3', 'Users')
        .parent()
        .find('table tbody tr')
        .first()
        .find('input[type="checkbox"]')
        .check()
        .should('be.checked');
    });

    it('Должен успешно создать шаблон с заполненными полями', () => {
      cy.login('officer_1', 'test123');
      cy.visit(`/templates/create?case_id=${testCaseId}`);

      cy.wait(2000);

      // Заполняем поля
      cy.contains('h3', 'Title')
        .parent()
        .find('input')
        .type('Итоговый отчет по делу');

      cy.contains('h3', 'Description')
        .parent()
        .find('textarea')
        .type('Отчет по результатам расследования дела');

      // Отправляем форму
      cy.contains('button', 'Submit').click();

      // Проверяем редирект на страницу дела
      cy.url().should('include', `/cases/${testCaseId}`, { timeout: 15000 });

      // Переходим к шаблонам
      cy.contains('button', 'Templates').click();

      // Проверяем, что шаблон создан
      cy.contains('Итоговый отчет по делу').should('be.visible');
    });
  });

  describe('UC3-002: Валидация формы шаблона', () => {
    it('Должен показать ошибку при пустом Title', () => {
      cy.login('officer_1', 'test123');
      cy.visit(`/templates/create?case_id=${testCaseId}`);

      cy.wait(2000);

      // Не заполняем Title
      // Пытаемся отправить
      cy.contains('button', 'Submit').click();

      // Проверяем ошибку
      cy.contains('Title is required').should('be.visible');

      // Проверяем, что редиректа не произошло
      cy.url().should('include', '/templates/create');
    });

    it('Должен показать ошибку, если не выбран ни один пользователь', () => {
      cy.login('officer_1', 'test123');
      cy.visit(`/templates/create?case_id=${testCaseId}`);

      cy.wait(2000);

      // Заполняем Title
      cy.contains('h3', 'Title')
        .parent()
        .find('input')
        .type('Тестовый отчет');

      // Снимаем выбор со всех пользователей
      cy.contains('h3', 'Users')
        .parent()
        .find('table tbody tr')
        .each(($row) => {
          cy.wrap($row).find('input[type="checkbox"]').uncheck();
        });

      // Отправляем
      cy.contains('button', 'Submit').click();

      // Проверяем ошибку
      cy.contains('Select at least one user').should('be.visible');
    });

    it('Должен показать ошибку, если не выбрана ни одна улика', () => {
      cy.login('officer_1', 'test123');
      cy.visit(`/templates/create?case_id=${testCaseId}`);

      cy.wait(2000);

      // Заполняем Title
      cy.contains('h3', 'Title')
        .parent()
        .find('input')
        .type('Тестовый отчет');

      // Снимаем выбор со всех улик
      cy.contains('h3', 'Clues')
        .parent()
        .find('table tbody tr')
        .each(($row) => {
          cy.wrap($row).find('input[type="checkbox"]').uncheck();
        });

      // Отправляем
      cy.contains('button', 'Submit').click();

      // Проверяем ошибку
      cy.contains('Select at least one clue').should('be.visible');
    });

    it('Должен показать ошибку, если не выбран ни один подозреваемый', () => {
      cy.login('officer_1', 'test123');
      cy.visit(`/templates/create?case_id=${testCaseId}`);

      cy.wait(2000);

      // Заполняем Title
      cy.contains('h3', 'Title')
        .parent()
        .find('input')
        .type('Тестовый отчет');

      // Снимаем выбор со всех подозреваемых
      cy.contains('h3', 'Suspects')
        .parent()
        .find('table tbody tr')
        .each(($row) => {
          cy.wrap($row).find('input[type="checkbox"]').uncheck();
        });

      // Отправляем
      cy.contains('button', 'Submit').click();

      // Проверяем ошибку
      cy.contains('Select at least one suspect').should('be.visible');
    });
  });

  describe('UC3-003: Отображение загруженных данных', () => {
    it('Должен отобразить таблицу с пользователями', () => {
      cy.login('officer_1', 'test123');
      cy.visit(`/templates/create?case_id=${testCaseId}`);

      cy.wait(2000);

      // Проверка таблицы пользователей
      cy.contains('h3', 'Users')
        .parent()
        .find('table thead tr th')
        .should('contain', 'Select');

      cy.contains('h3', 'Users')
        .parent()
        .find('table thead tr th')
        .should('contain', 'Name');

      cy.contains('h3', 'Users')
        .parent()
        .find('table thead tr th')
        .should('contain', 'Position');
    });

    it('Должен отобразить таблицу с уликами', () => {
      cy.login('officer_1', 'test123');
      cy.visit(`/templates/create?case_id=${testCaseId}`);

      cy.wait(2000);

      // Проверка таблицы улик
      cy.contains('h3', 'Clues')
        .parent()
        .find('table thead tr th')
        .should('contain', 'Select');

      cy.contains('h3', 'Clues')
        .parent()
        .find('table thead tr th')
        .should('contain', 'Title');
    });

    it('Должен отобразить таблицу с подозреваемыми', () => {
      cy.login('officer_1', 'test123');
      cy.visit(`/templates/create?case_id=${testCaseId}`);

      cy.wait(2000);

      // Проверка таблицы подозреваемых
      cy.contains('h3', 'Suspects')
        .parent()
        .find('table thead tr th')
        .should('contain', 'Name');

      cy.contains('h3', 'Suspects')
        .parent()
        .find('table thead tr th')
        .should('contain', 'Identifier');

      cy.contains('h3', 'Suspects')
        .parent()
        .find('table thead tr th')
        .should('contain', 'Accusation');
    });
  });

  describe('UC12-001: Автоматическая загрузка шаблона в ЦБД', () => {
    it('Должен автоматически загрузить шаблон в ЦБД после создания', () => {
      cy.login('officer_1', 'test123');
      cy.visit(`/templates/create?case_id=${testCaseId}`);

      cy.wait(2000);

      // Заполняем форму
      cy.contains('h3', 'Title')
        .parent()
        .find('input')
        .type('Отчет для автозагрузки в ЦБД');

      cy.contains('h3', 'Description')
        .parent()
        .find('textarea')
        .type('Этот шаблон должен быть автоматически загружен в ЦБД');

      // Перехватываем запрос на загрузку файла
      cy.intercept('POST', '/api/files').as('uploadToDatabase');

      // Отправляем форму
      cy.contains('button', 'Submit').click();

      // Проверяем, что файл был загружен
      cy.wait('@uploadToDatabase', { timeout: 15000 });

      // Проверяем редирект
      cy.url().should('include', `/cases/${testCaseId}`);

      // Переходим к шаблонам
      cy.contains('button', 'Templates').click();

      // Проверяем, что шаблон создан
      cy.contains('Отчет для автозагрузки в ЦБД').should('be.visible');

      // Проверяем статус шаблона (должен быть CDB_ADDED)
      cy.contains('Отчет для автозагрузки в ЦБД')
        .closest('tr')
        .find('select')
        .should('have.value', 'CDB_ADDED');
    });
  });
});
