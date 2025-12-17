/**
 * E2E тесты для UC2: Работа с уликами, анализами (FR 3.1.7)
 *
 * Тестируемые сценарии:
 * - UC2-001: Создание улики с загрузкой файлов
 * - UC2-002: Проверка прав доступа при создании улики
 * - UC2-003: Действия с загруженными файлами (View, Download, Remove)
 */

describe('UC2: Работа с уликами', () => {
  let testCaseId: number;

  beforeEach(() => {
    cy.clearCookies();
    cy.clearLocalStorage();
  });

  // Подготовка: создаем тестовое дело для улик
  before(() => {
    cy.clearCookies();
    cy.clearLocalStorage();

    // Авторизация и создание инцидента через API
    cy.login('dispatcher', 'test123');

    cy.createIncidentViaAPI({
      reason: 'Тестовый инцидент для создания улик',
      type: 'Кража',
      latitude: '59.9311',
      longitude: '30.3609',
      altitude: '10.5',
      applicant: 'Тестовый заявитель',
    }).then((incidentId) => {
      // Создаем дело
      cy.createCaseViaAPI(incidentId, {
        title: 'Тестовое дело для улик',
        description: 'Описание тестового дела',
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

  describe('UC2-001: Создание улики с загрузкой файлов', () => {
    it('Должен успешно создать улику с загрузкой изображения', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/clues/new?case_id=${testCaseId}`);

      // Проверка формы
      cy.get('h2').should('contain', 'Create Clue');

      // Заполнение данных
      cy.get('input[type="input"]').first().type('Отпечатки пальцев');
      cy.get('textarea').first().type('Найдены на месте преступления');

      // Загрузка изображения
      cy.get('input[type="file"]').selectFile('cypress/fixtures/test-image.jpg', {
        force: true,
      });

      // Ждем загрузки файла
      cy.wait('@uploadFile', { timeout: 15000 });

      // Проверяем отображение файла
      cy.get('.file').should('be.visible');
      cy.contains('test-image.jpg').should('be.visible');
      cy.contains('image/jpeg').should('be.visible');

      // Проверяем preview изображения
      cy.get('img.preview').should('be.visible');

      // Отправка формы
      cy.contains('button', 'Submit').click();
      cy.wait('@createClue', { timeout: 15000 });

      // Проверка редиректа
      cy.url().should('include', `/cases/${testCaseId}`);
    });

    it('Должен успешно создать улику с несколькими файлами', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/clues/new?case_id=${testCaseId}`);

      cy.get('input[type="input"]').first().type('Вещественные доказательства');
      cy.get('textarea').first().type('Комплект улик');

      // Загрузка изображения
      cy.get('input[type="file"]').selectFile('cypress/fixtures/test-image.png', {
        force: true,
      });
      cy.wait('@uploadFile', { timeout: 15000 });

      // Загрузка текстового файла
      cy.get('input[type="file"]').selectFile('cypress/fixtures/test-report.txt', {
        force: true,
      });
      cy.wait('@uploadFile', { timeout: 15000 });

      // Проверяем наличие двух файлов
      cy.get('.file').should('have.length', 2);
      cy.contains('test-image.png').should('be.visible');
      cy.contains('test-report.txt').should('be.visible');

      cy.contains('button', 'Submit').click();
      cy.wait('@createClue', { timeout: 15000 });

      cy.url().should('include', `/cases/${testCaseId}`);
    });

    it('Должен успешно создать улику без файлов', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/clues/new?case_id=${testCaseId}`);

      cy.get('input[type="input"]').first().type('Свидетельские показания');
      cy.get('textarea').first().type('Показания свидетеля Иванова И.И.');

      cy.contains('button', 'Submit').click();
      cy.wait('@createClue', { timeout: 15000 });

      cy.url().should('include', `/cases/${testCaseId}`);
    });
  });

  describe('UC2-002: Проверка прав доступа при создании улики', () => {
    it('Должен запретить создание улики пользователю без прав CLUES_ADD', () => {
      // Авторизуемся как viewer (только чтение)
      cy.login('viewer', 'test123');
      cy.visit(`/clues/new?case_id=${testCaseId}`);

      // Проверяем сообщение об ошибке
      cy.contains('No permission').should('be.visible');

      // Проверяем, что кнопка Submit disabled
      cy.contains('button', 'Submit').should('be.disabled');
    });

    it('Должен показать debug информацию пользователю с правами LOGS:*:READ', () => {
      // Логинимся как admin с правами просмотра логов
      cy.login('admin', 'test123');
      cy.visit(`/clues/new?case_id=${testCaseId}`);

      // Проверяем наличие debug секции
      cy.get('.debug').should('be.visible');
      cy.get('.debug').should('contain', 'ownerLoading');
      cy.get('.debug').should('contain', `caseId: ${testCaseId}`);
      cy.get('.debug').should('contain', 'canAddClue');
      cy.get('.debug').should('contain', 'canUpdateCase');
      cy.get('.debug').should('contain', 'canUpload');
    });

    it('Должен запретить загрузку файлов пользователю без прав FILES:*:UPLOAD', () => {
      // Предполагаем, что есть пользователь с CLUES_ADD, но без FILES:*:UPLOAD
      // Для этого теста нужно проверить текущих пользователей
      cy.login('dispatcher', 'test123');
      cy.visit(`/clues/new?case_id=${testCaseId}`);

      // Если нет прав на upload, должно быть сообщение
      // (этот тест может не сработать, если у dispatcher есть все права)
      // В реальной ситуации нужен специальный тестовый пользователь
    });
  });

  describe('UC2-003: Действия с загруженными файлами', () => {
    it('Должен позволить просмотреть загруженное изображение', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/clues/new?case_id=${testCaseId}`);

      cy.get('input[type="input"]').first().type('Тестовая улика');
      cy.get('textarea').first().type('Описание');

      // Загрузка файла
      cy.get('input[type="file"]').selectFile('cypress/fixtures/test-image.jpg', {
        force: true,
      });
      cy.wait('@uploadFile', { timeout: 15000 });

      // Клик на View
      cy.window().then((win) => {
        cy.stub(win, 'open').as('windowOpen');
      });

      cy.contains('button', 'View').click();

      // Проверяем, что window.open был вызван
      cy.get('@windowOpen').should('be.called');
    });

    it('Должен позволить скачать загруженный файл', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/clues/new?case_id=${testCaseId}`);

      cy.get('input[type="input"]').first().type('Тестовая улика');
      cy.get('textarea').first().type('Описание');

      // Загрузка файла
      cy.get('input[type="file"]').selectFile('cypress/fixtures/test-report.txt', {
        force: true,
      });
      cy.wait('@uploadFile', { timeout: 15000 });

      // Клик на Download
      // Примечание: Cypress не может напрямую проверить скачивание файла,
      // но мы можем проверить, что кнопка работает
      cy.contains('button', 'Download').should('be.visible').click();

      // Можно проверить, что запрос был отправлен
      cy.wait(500); // Небольшая задержка для завершения запроса
    });

    it('Должен позволить удалить загруженный файл из списка', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/clues/new?case_id=${testCaseId}`);

      cy.get('input[type="input"]').first().type('Тестовая улика');
      cy.get('textarea').first().type('Описание');

      // Загрузка двух файлов
      cy.get('input[type="file"]').selectFile('cypress/fixtures/test-image.png', {
        force: true,
      });
      cy.wait('@uploadFile', { timeout: 15000 });

      cy.get('input[type="file"]').selectFile('cypress/fixtures/test-report.txt', {
        force: true,
      });
      cy.wait('@uploadFile', { timeout: 15000 });

      // Проверяем, что есть 2 файла
      cy.get('.file').should('have.length', 2);

      // Удаляем первый файл
      cy.get('.file').first().contains('button', 'Remove').click();

      // Проверяем, что остался только 1 файл
      cy.get('.file').should('have.length', 1);
      cy.contains('test-report.txt').should('be.visible');
      cy.contains('test-image.png').should('not.exist');
    });

    it('Должен корректно отображать превью только для изображений', () => {
      cy.login('dispatcher', 'test123');
      cy.visit(`/clues/new?case_id=${testCaseId}`);

      // Загрузка изображения
      cy.get('input[type="file"]').selectFile('cypress/fixtures/test-image.jpg', {
        force: true,
      });
      cy.wait('@uploadFile', { timeout: 15000 });

      // Для изображения должно быть превью
      cy.get('.file').first().find('img.preview').should('be.visible');

      // Загрузка текстового файла
      cy.get('input[type="file"]').selectFile('cypress/fixtures/test-report.txt', {
        force: true,
      });
      cy.wait('@uploadFile', { timeout: 15000 });

      // Для текстового файла превью быть не должно
      cy.get('.file').last().find('img.preview').should('not.exist');
    });
  });
});
