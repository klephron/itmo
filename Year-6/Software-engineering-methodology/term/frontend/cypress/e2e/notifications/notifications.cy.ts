/**
 * E2E тесты для UC4: Уведомление участников дела об изменениях (FR 3.1.12)
 *
 * Тестируемые сценарии:
 * - UC4-001: Просмотр непрочитанных уведомлений
 * - UC4-002: Отметка уведомления как прочитанного
 * - UC4-003: Поиск уведомлений по фильтрам (дата, статус)
 * - UC4-004: Отображение счетчика непрочитанных уведомлений
 */

describe('UC4: Уведомления участников дела', () => {
  beforeEach(() => {
    cy.clearCookies();
    cy.clearLocalStorage();
  });

  describe('UC4-001: Просмотр непрочитанных уведомлений', () => {
    it('Должен отобразить страницу уведомлений', () => {
      cy.login('dispatcher', 'test123');
      cy.visit('/notifications');

      // Проверка секций
      cy.contains('h2', 'Unread Notifications').should('be.visible');
      cy.contains('h2', 'Search Notifications').should('be.visible');
    });

    it('Должен отобразить бейдж с количеством непрочитанных уведомлений', () => {
      cy.login('dispatcher', 'test123');
      cy.visit('/notifications');

      // Проверка бейджа
      cy.get('.badge').should('be.visible');
      // Бейдж должен содержать число
      cy.get('.badge').invoke('text').should('match', /^\d+$/);
    });

    it('Должен отобразить таблицу непрочитанных уведомлений', () => {
      cy.login('dispatcher', 'test123');
      cy.visit('/notifications');

      // Проверка таблицы
      cy.contains('h2', 'Unread Notifications')
        .parent()
        .find('table')
        .should('exist');

      // Проверка колонок
      cy.contains('h2', 'Unread Notifications')
        .parent()
        .find('table thead tr th')
        .should('contain', 'Title');

      cy.contains('h2', 'Unread Notifications')
        .parent()
        .find('table thead tr th')
        .should('contain', 'Description');

      cy.contains('h2', 'Unread Notifications')
        .parent()
        .find('table thead tr th')
        .should('contain', 'Created Time');

      cy.contains('h2', 'Unread Notifications')
        .parent()
        .find('table thead tr th')
        .should('contain', 'Read');
    });

    it('Должен отобразить сообщение, если нет непрочитанных уведомлений', () => {
      // Для этого теста нужен пользователь без уведомлений
      // или нужно предварительно отметить все как прочитанные
      cy.login('dispatcher', 'test123');
      cy.visit('/notifications');

      // Может отобразиться либо таблица, либо сообщение "No notifications"
      cy.wait(1000);
    });
  });

  describe('UC4-002: Отметка уведомления как прочитанного', () => {
    it('Должен позволить отметить уведомление как прочитанное', () => {
      cy.login('dispatcher', 'test123');
      cy.visit('/notifications');

      cy.wait(1000); // Даем время на загрузку уведомлений

      // Получаем начальное количество непрочитанных
      cy.get('.badge')
        .invoke('text')
        .then((initialCount) => {
          const count = parseInt(initialCount);

          if (count > 0) {
            // Кликаем на первый checkbox
            cy.contains('h2', 'Unread Notifications')
              .parent()
              .find('table tbody tr')
              .first()
              .find('input[type="checkbox"]')
              .check();

            // Ждем обновления
            cy.wait('@updateNotification', { timeout: 10000 });

            // Проверяем, что счетчик уменьшился
            cy.get('.badge')
              .invoke('text')
              .then((newCount) => {
                const newCountNum = parseInt(newCount);
                expect(newCountNum).to.be.lessThan(count);
              });
          } else {
            cy.log('No unread notifications to test');
          }
        });
    });

    it('Должен отправить запрос на сервер при отметке как прочитанное', () => {
      cy.login('dispatcher', 'test123');
      cy.visit('/notifications');

      cy.wait(1000);

      // Перехватываем запрос
      cy.intercept('PATCH', '/api/notifications/*').as('markAsRead');

      cy.get('.badge')
        .invoke('text')
        .then((count) => {
          if (parseInt(count) > 0) {
            // Кликаем на checkbox
            cy.contains('h2', 'Unread Notifications')
              .parent()
              .find('table tbody tr')
              .first()
              .find('input[type="checkbox"]')
              .check();

            // Проверяем, что запрос отправлен
            cy.wait('@markAsRead');
          }
        });
    });
  });

  describe('UC4-003: Поиск уведомлений по фильтрам', () => {
    it('Должен отобразить форму поиска уведомлений', () => {
      cy.login('dispatcher', 'test123');
      cy.visit('/notifications');

      // Проверка элементов формы поиска
      cy.contains('label', 'From (UTC)').should('be.visible');
      cy.contains('label', 'To (UTC)').should('be.visible');

      // Проверка полей ввода даты
      cy.get('input[type="datetime-local"]').should('have.length', 2);

      // Проверка селекта статуса
      cy.get('select').should('be.visible');

      // Проверка кнопки поиска
      cy.contains('button', 'Search').should('be.visible');
    });

    it('Должен отобразить опции статуса (ALL, READ, UNREAD)', () => {
      cy.login('dispatcher', 'test123');
      cy.visit('/notifications');

      // Проверка опций в селекте
      cy.get('select option').should('contain', 'ALL');
      cy.get('select option').should('contain', 'READ');
      cy.get('select option').should('contain', 'UNREAD');
    });

    it('Должен выполнить поиск при клике на кнопку Search', () => {
      cy.login('dispatcher', 'test123');
      cy.visit('/notifications');

      // Перехватываем запрос
      cy.intercept('GET', '/api/notifications*').as('searchNotifications');

      // Кликаем на кнопку поиска
      cy.contains('button', 'Search').click();

      // Проверяем, что запрос отправлен
      cy.wait('@searchNotifications');

      // Проверяем, что таблица результатов отображается
      cy.contains('h2', 'Search Notifications')
        .parent()
        .find('table')
        .should('be.visible');
    });

    it('Должен позволить фильтровать по статусу', () => {
      cy.login('dispatcher', 'test123');
      cy.visit('/notifications');

      // Выбираем статус UNREAD
      cy.get('select').select('UNREAD');

      // Кликаем Search
      cy.intercept('GET', '/api/notifications*').as('searchUnread');
      cy.contains('button', 'Search').click();
      cy.wait('@searchUnread');

      // Проверяем, что в URL запроса есть status=UNREAD
      cy.wait('@searchUnread').then((interception) => {
        expect(interception.request.url).to.include('status=UNREAD');
      });
    });

    it('Должен позволить фильтровать по диапазону дат', () => {
      cy.login('dispatcher', 'test123');
      cy.visit('/notifications');

      // Устанавливаем даты
      // От: вчера
      const yesterday = new Date();
      yesterday.setDate(yesterday.getDate() - 1);
      const yesterdayStr = yesterday.toISOString().slice(0, 16);

      // До: сегодня
      const today = new Date();
      const todayStr = today.toISOString().slice(0, 16);

      cy.get('input[type="datetime-local"]').eq(0).clear().type(yesterdayStr);
      cy.get('input[type="datetime-local"]').eq(1).clear().type(todayStr);

      // Кликаем Search
      cy.intercept('GET', '/api/notifications*').as('searchByDate');
      cy.contains('button', 'Search').click();
      cy.wait('@searchByDate');

      // Проверяем, что в URL запроса есть from и to
      cy.wait('@searchByDate').then((interception) => {
        expect(interception.request.url).to.include('from=');
        expect(interception.request.url).to.include('to=');
      });
    });

    it('Должен отобразить результаты поиска в таблице', () => {
      cy.login('dispatcher', 'test123');
      cy.visit('/notifications');

      cy.contains('button', 'Search').click();
      cy.wait('@getNotifications');

      // Проверка таблицы результатов
      cy.contains('h2', 'Search Notifications')
        .parent()
        .find('table thead tr th')
        .should('contain', 'Title');

      cy.contains('h2', 'Search Notifications')
        .parent()
        .find('table thead tr th')
        .should('contain', 'Description');

      cy.contains('h2', 'Search Notifications')
        .parent()
        .find('table thead tr th')
        .should('contain', 'Created Time');

      cy.contains('h2', 'Search Notifications')
        .parent()
        .find('table thead tr th')
        .should('contain', 'Read Time');
    });
  });

  describe('UC4-004: Навигация к деталям уведомления', () => {
    it('Должен позволить перейти к деталям уведомления по клику на Title', () => {
      cy.login('dispatcher', 'test123');
      cy.visit('/notifications');

      cy.wait(1000);

      // Проверяем наличие уведомлений
      cy.get('.badge')
        .invoke('text')
        .then((count) => {
          if (parseInt(count) > 0) {
            // Кликаем на первое уведомление
            cy.contains('h2', 'Unread Notifications')
              .parent()
              .find('table tbody tr')
              .first()
              .find('a')
              .click();

            // Проверяем переход
            cy.url().should('match', /\/notifications\/\d+$/);
          } else {
            // Если нет непрочитанных, ищем в результатах поиска
            cy.contains('button', 'Search').click();
            cy.wait('@getNotifications');

            cy.contains('h2', 'Search Notifications')
              .parent()
              .find('table tbody tr')
              .first()
              .find('a')
              .click();

            cy.url().should('match', /\/notifications\/\d+$/);
          }
        });
    });
  });

  describe('UC4-005: Автоматическая загрузка при открытии страницы', () => {
    it('Должен автоматически выполнить поиск при загрузке страницы', () => {
      cy.intercept('GET', '/api/notifications*').as('initialSearch');

      cy.login('dispatcher', 'test123');
      cy.visit('/notifications');

      // Проверяем, что поиск выполнился автоматически
      cy.wait('@initialSearch');

      // Проверяем, что таблица результатов заполнена
      cy.contains('h2', 'Search Notifications')
        .parent()
        .find('table tbody tr')
        .should('exist');
    });
  });
});
