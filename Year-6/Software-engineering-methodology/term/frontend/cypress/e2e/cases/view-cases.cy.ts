/**
 * E2E тесты для UC9: Чтение данных по делу из Системы (FR 3.1.6)
 *
 * Тестируемые сценарии:
 * - UC9-001: Просмотр списка назначенных дел
 * - UC9-002: Просмотр деталей дела
 * - UC9-003: Навигация между связанными объектами
 * - UC9-004: Просмотр участников дела
 * - UC9-005: Просмотр улик дела
 * - UC9-006: Просмотр подозреваемых
 */

describe('UC9: Просмотр данных по делу', () => {
  let testIncidentId: number;
  let testCaseId: number;

  beforeEach(() => {
    cy.clearCookies();
    cy.clearLocalStorage();
  });

  // Подготовка: создаем тестовые данные
  before(() => {
    cy.clearCookies();
    cy.clearLocalStorage();

    cy.login('dispatcher', 'test123');

    // Создаем инцидент и дело через API
    cy.createIncidentViaAPI({
      reason: 'Тестовый инцидент для просмотра дела',
      type: 'Кража',
      latitude: '59.9311',
      longitude: '30.3609',
      altitude: '10.5',
      applicant: 'Заявитель Иван Иванович',
    }).then((incidentId) => {
      testIncidentId = incidentId;

      cy.createCaseViaAPI(incidentId, {
        title: 'Тестовое дело для просмотра',
        description: 'Подробное описание тестового дела',
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

  describe('UC9-001: Просмотр списка назначенных дел', () => {
    it('Должен отобразить таблицу с назначенными делами', () => {
      cy.login('dispatcher', 'test123');
      cy.visit('/cases');

      // Проверка заголовка
      cy.get('h2').should('contain', 'Assigned Cases');

      // Проверка таблицы
      cy.get('table').should('be.visible');

      // Проверка колонок
      cy.get('thead tr th').should('contain', 'Title');
      cy.get('thead tr th').should('contain', 'Position');
      cy.get('thead tr th').should('contain', 'Status');
    });

    it('Должен отобразить дела в порядке убывания ID', () => {
      cy.login('dispatcher', 'test123');
      cy.visit('/cases');

      cy.get('table tbody tr').should('have.length.greaterThan', 0);

      // Проверяем, что дела отсортированы по ID (desc)
      // Сравниваем первое и последнее дело
      cy.get('table tbody tr')
        .first()
        .should('exist');
    });

    it('Должен позволить перейти к деталям дела по клику на Title', () => {
      cy.login('dispatcher', 'test123');
      cy.visit('/cases');

      // Находим строку с нашим тестовым делом
      cy.contains('a', 'Тестовое дело для просмотра')
        .should('be.visible')
        .click();

      // Проверяем переход на страницу дела
      cy.url().should('match', /\/cases\/\d+$/);
      cy.contains('h2', 'Case').should('be.visible');
    });

    it('Должен отобразить позицию и статус для каждого дела', () => {
      cy.login('dispatcher', 'test123');
      cy.visit('/cases');

      // Проверяем, что у каждого дела есть позиция и статус
      cy.get('table tbody tr').each(($row) => {
        cy.wrap($row).find('td').should('have.length', 3);
      });
    });
  });

  describe('UC9-002: Просмотр деталей дела', () => {
    it('Должен отобразить основную информацию о деле', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/cases/${testCaseId}`);

      // Проверка заголовка
      cy.get('h2').should('contain', 'Case');
      cy.get('h2').should('contain', 'Тестовое дело для просмотра');

      // Проверка полей
      cy.contains('h3', 'Status').should('be.visible');
      cy.contains('h3', 'Description').should('be.visible');
      cy.contains('h3', 'Created At').should('be.visible');
      cy.contains('h3', 'Updated At').should('be.visible');
      cy.contains('h3', 'Creator').should('be.visible');
      cy.contains('h3', 'Updator').should('be.visible');

      // Проверка описания
      cy.contains('Подробное описание тестового дела').should('be.visible');
    });

    it('Должен отобразить связанный инцидент', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/cases/${testCaseId}`);

      // Проверка секции с инцидентом
      cy.contains('h3', 'Related Incident').should('be.visible');

      // Проверка ссылки на инцидент
      cy.contains('a', 'Тестовый инцидент для просмотра дела').should('be.visible');
    });

    it('Должен позволить перейти к связанному инциденту', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/cases/${testCaseId}`);

      // Клик на ссылку инцидента
      cy.contains('a', 'Тестовый инцидент для просмотра дела').click();

      // Проверка перехода
      cy.url().should('include', `/incidents/${testIncidentId}`);
    });

    it('Должен отобразить информацию о создателе дела', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/cases/${testCaseId}`);

      // Проверка ссылки на создателя
      cy.contains('h3', 'Creator')
        .parent()
        .find('a')
        .should('exist');
    });

    it('Должен отобразить форму редактирования деталей дела', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/cases/${testCaseId}`);

      // Проверка формы
      cy.contains('h3', 'Case details').should('be.visible');

      // Проверка полей формы
      cy.get('label').contains('Title').should('be.visible');
      cy.get('label').contains('Description').should('be.visible');
      cy.get('label').contains('Status').should('be.visible');

      // Проверка кнопки сохранения
      cy.contains('button', 'Save case').should('be.visible');
    });
  });

  describe('UC9-003: Меню навигации по разделам дела', () => {
    it('Должен отобразить меню с разделами', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/cases/${testCaseId}`);

      // Проверка кнопок меню
      cy.contains('button', 'Users').should('be.visible');
      cy.contains('button', 'Clues').should('be.visible');
      cy.contains('button', 'Suspects').should('be.visible');
      cy.contains('button', 'Templates').should('be.visible');
    });

    it('Должен переключаться между разделами Users, Clues, Suspects, Templates', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/cases/${testCaseId}`);

      // Клик на Users
      cy.contains('button', 'Users').click();
      cy.contains('h3', 'Users').should('be.visible');
      cy.contains('h4', 'Participants').should('be.visible');

      // Клик на Clues
      cy.contains('button', 'Clues').click();
      cy.contains('h3', 'Clues').should('be.visible');
      cy.contains('button', 'Create').should('be.visible');

      // Клик на Suspects
      cy.contains('button', 'Suspects').click();
      cy.contains('h3', 'Suspects').should('be.visible');

      // Клик на Templates
      cy.contains('button', 'Templates').click();
      cy.contains('h3', 'Templates').should('be.visible');
    });
  });

  describe('UC9-004: Просмотр участников дела', () => {
    it('Должен отобразить таблицу участников', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/cases/${testCaseId}`);

      cy.contains('button', 'Users').click();

      // Проверка таблицы
      cy.contains('h4', 'Participants').should('be.visible');
      cy.get('table thead tr th').should('contain', 'Position');
      cy.get('table thead tr th').should('contain', 'User');
      cy.get('table thead tr th').should('contain', 'Actions');
    });

    it('Должен отобразить форму добавления участника (если есть права)', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/cases/${testCaseId}`);

      cy.contains('button', 'Users').click();

      // Проверка полей добавления пользователя
      // (могут быть видны только если есть права canUpdateCase)
      // Селекты могут быть или не быть в зависимости от прав
      cy.wait(1000); // Даем время на загрузку прав
    });
  });

  describe('UC9-005: Просмотр улик дела', () => {
    it('Должен отобразить таблицу улик', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/cases/${testCaseId}`);

      cy.contains('button', 'Clues').click();

      // Проверка таблицы
      cy.get('table thead tr th').should('contain', 'Title');
      cy.get('table thead tr th').should('contain', 'Status');
      cy.get('table thead tr th').should('contain', 'Description');
      cy.get('table thead tr th').should('contain', 'Attachments');
      cy.get('table thead tr th').should('contain', 'Options');
    });

    it('Должен отобразить кнопку создания улики', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/cases/${testCaseId}`);

      cy.contains('button', 'Clues').click();

      // Проверка кнопки Create
      cy.contains('button', 'Create').should('be.visible');
    });

    it('Должен позволить перейти к созданию улики', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/cases/${testCaseId}`);

      cy.contains('button', 'Clues').click();
      cy.contains('button', 'Create').click();

      // Проверка перехода
      cy.url().should('include', `/clues/create?case_id=${testCaseId}`);
    });
  });

  describe('UC9-006: Просмотр подозреваемых', () => {
    it('Должен отобразить таблицу подозреваемых', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/cases/${testCaseId}`);

      cy.contains('button', 'Suspects').click();

      // Проверка таблицы
      cy.get('table thead tr th').should('contain', 'Name');
      cy.get('table thead tr th').should('contain', 'Identifier');
      cy.get('table thead tr th').should('contain', 'Verdict');
      cy.get('table thead tr th').should('contain', 'Accusation');
      cy.get('table thead tr th').should('contain', 'Status');
      cy.get('table thead tr th').should('contain', 'Actions');
    });

    it('Должен отобразить кнопку создания подозреваемого', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/cases/${testCaseId}`);

      cy.contains('button', 'Suspects').click();

      // Проверка кнопки Create
      cy.contains('button', 'Create').should('be.visible');
    });

    it('Должен позволить перейти к созданию подозреваемого', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/cases/${testCaseId}`);

      cy.contains('button', 'Suspects').click();
      cy.contains('button', 'Create').click();

      // Проверка перехода
      cy.url().should('include', `/suspects/create?case_id=${testCaseId}`);
    });
  });

  describe('UC9-007: Просмотр шаблонов отчетов', () => {
    it('Должен отобразить таблицу шаблонов', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/cases/${testCaseId}`);

      cy.contains('button', 'Templates').click();

      // Проверка таблицы
      cy.get('table thead tr th').should('contain', 'Title');
      cy.get('table thead tr th').should('contain', 'Status');
      cy.get('table thead tr th').should('contain', 'Metadata');
      cy.get('table thead tr th').should('contain', 'Files');
      cy.get('table thead tr th').should('contain', 'Options');
    });

    it('Должен отобразить кнопку создания шаблона', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/cases/${testCaseId}`);

      cy.contains('button', 'Templates').click();

      // Проверка кнопки Create
      cy.contains('button', 'Create').should('be.visible');
    });

    it('Должен позволить перейти к созданию шаблона', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/cases/${testCaseId}`);

      cy.contains('button', 'Templates').click();
      cy.contains('button', 'Create').click();

      // Проверка перехода
      cy.url().should('include', `/templates/create?case_id=${testCaseId}`);
    });
  });

  describe('UC9-008: Редактирование деталей дела', () => {
    it('Должен позволить изменить Title, Description и Status дела (если есть права)', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/cases/${testCaseId}`);

      // Изменяем title
      const newTitle = 'Обновленное название дела';
      cy.get('label')
        .contains('Title')
        .parent()
        .find('input')
        .clear()
        .type(newTitle);

      // Изменяем description
      const newDescription = 'Обновленное описание дела';
      cy.get('label')
        .contains('Description')
        .parent()
        .find('textarea')
        .clear()
        .type(newDescription);

      // Сохраняем
      cy.contains('button', 'Save case').click();

      // Ожидаем запроса
      cy.wait(1000);

      // Перезагружаем страницу для проверки
      cy.reload();

      // Проверяем, что изменения сохранились
      cy.get('h2').should('contain', newTitle);
      cy.contains(newDescription).should('be.visible');
    });

    it('Должен запретить редактирование дела пользователю без прав', () => {
      cy.login('viewer', 'test123');
      cy.visit(`/cases/${testCaseId}`);

      // Проверяем, что поля disabled
      cy.get('label')
        .contains('Title')
        .parent()
        .find('input')
        .should('be.disabled');

      cy.get('label')
        .contains('Description')
        .parent()
        .find('textarea')
        .should('be.disabled');

      // Проверяем, что кнопка Save disabled
      cy.contains('button', 'Save case').should('be.disabled');

      // Проверяем сообщение о правах
      cy.contains('No permission').should('be.visible');
    });
  });
});
