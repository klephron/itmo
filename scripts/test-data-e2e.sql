-- PAI E2E Testing - Test Data Generation Script
-- Generates realistic test data for E2E business scenario testing
-- Target: Support full business cycle testing (UC1 → UC12)

-- ==============================================
-- ВАЖНО: Этот скрипт создает минимальный набор данных
-- для выполнения E2E бизнес-сценариев через UI
-- ==============================================

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==============================================
-- CREATE SEQUENCES (if not exist)
-- ==============================================
CREATE SEQUENCE IF NOT EXISTS users_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS incidents_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS cases_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS clues_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS notifications_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS user_cases_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS user_permissions_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS templates_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS log_entries_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS suspects_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS files_seq START WITH 1 INCREMENT BY 1;

-- ==============================================
-- CLEAN UP (опционально - если нужен чистый тест)
-- ==============================================
-- TRUNCATE TABLE log_entries, templates, clues, user_cases, cases, incidents,
--          user_permissions, notifications, suspects, files, users CASCADE;

-- ==============================================
-- ТЕСТОВЫЕ ПОЛЬЗОВАТЕЛИ (для E2E сценариев)
-- ==============================================

-- Диспетчер (создает инциденты и дела)
INSERT INTO users (user_id, username, name, surname, patronymic, primary_position, password,
                   location_latitude, location_longitude, location_altitude)
VALUES (
    nextval('users_seq'),
    'dispatcher',
    'Анна',
    'Диспетчерова',
    'Сергеевна',
    'DISPATCHER',
    '$2a$10$vWM0Nxese0N2eJuHeSBzLO9p0ah093rX0GQrpnW9NK5tYbvLHOElu', -- пароль: test123
    59.9343,  -- СПб центр
    30.3351,
    10.0
);

-- Офицер #1 (подтверждает улики, создает отчеты)
INSERT INTO users (user_id, username, name, surname, patronymic, primary_position, password,
                   location_latitude, location_longitude, location_altitude)
VALUES (
    nextval('users_seq'),
    'officer_1',
    'Иван',
    'Офицеров',
    'Петрович',
    'PATROL',
    '$2a$10$vWM0Nxese0N2eJuHeSBzLO9p0ah093rX0GQrpnW9NK5tYbvLHOElu', -- test123
    59.9311,  -- Близко к центру
    30.3609,
    8.0
);

-- Офицер #2 (участник отряда)
INSERT INTO users (user_id, username, name, surname, patronymic, primary_position, password,
                   location_latitude, location_longitude, location_altitude)
VALUES (
    nextval('users_seq'),
    'officer_2',
    'Мария',
    'Офицерова',
    'Дмитриевна',
    'SENIOR_PATROL',
    '$2a$10$vWM0Nxese0N2eJuHeSBzLO9p0ah093rX0GQrpnW9NK5tYbvLHOElu', -- test123
    59.9280,  -- Чуть дальше
    30.3500,
    12.0
);

-- Следователь (собирает улики)
INSERT INTO users (user_id, username, name, surname, patronymic, primary_position, password,
                   location_latitude, location_longitude, location_altitude)
VALUES (
    nextval('users_seq'),
    'investigator_1',
    'Петр',
    'Следователев',
    'Андреевич',
    'INVESTIGATOR',
    '$2a$10$vWM0Nxese0N2eJuHeSBzLO9p0ah093rX0GQrpnW9NK5tYbvLHOElu', -- test123
    59.9250,  -- Средняя дистанция
    30.3400,
    15.0
);

-- Эксперт-криминалист (работа с уликами)
INSERT INTO users (user_id, username, name, surname, patronymic, primary_position, password,
                   location_latitude, location_longitude, location_altitude)
VALUES (
    nextval('users_seq'),
    'forensics_1',
    'Ольга',
    'Экспертова',
    'Викторовна',
    'FORENSICS',
    '$2a$10$vWM0Nxese0N2eJuHeSBzLO9p0ah093rX0GQrpnW9NK5tYbvLHOElu', -- test123
    59.9200,  -- Дальше от места
    30.3300,
    20.0
);

-- Администратор (просмотр логов, управление системой)
INSERT INTO users (user_id, username, name, surname, patronymic, primary_position, password,
                   location_latitude, location_longitude, location_altitude)
VALUES (
    nextval('users_seq'),
    'admin',
    'Администратор',
    'Системович',
    'Главный',
    'OPERATOR',
    '$2a$10$vWM0Nxese0N2eJuHeSBzLO9p0ah093rX0GQrpnW9NK5tYbvLHOElu', -- test123
    59.9343,
    30.3351,
    10.0
);

-- Viewer (только просмотр - для тестирования ограничений)
INSERT INTO users (user_id, username, name, surname, patronymic, primary_position, password,
                   location_latitude, location_longitude, location_altitude)
VALUES (
    nextval('users_seq'),
    'viewer',
    'Виктор',
    'Просмотров',
    'Иванович',
    'ANALYST',
    '$2a$10$vWM0Nxese0N2eJuHeSBzLO9p0ah093rX0GQrpnW9NK5tYbvLHOElu', -- test123
    59.9400,
    30.3600,
    5.0
);

-- ==============================================
-- ПРАВА ДОСТУПА (Permissions)
-- ==============================================

-- Диспетчер - может создавать инциденты и дела
DO $$
DECLARE
    dispatcher_id BIGINT;
BEGIN
    SELECT user_id INTO dispatcher_id FROM users WHERE username = 'dispatcher';

    INSERT INTO user_permissions (user_id, privilege) VALUES
        (dispatcher_id, 'INCIDENTS:*:ADD'),
        (dispatcher_id, 'INCIDENTS:*:READ'),
        (dispatcher_id, 'INCIDENTS:*:UPDATE'),
        (dispatcher_id, 'INCIDENTS:*:CASE_ADD'),
        (dispatcher_id, 'CASES:*:READ'),
        (dispatcher_id, 'CASES:*:UPDATE'),
        (dispatcher_id, 'USERS:*:READ');
END $$;

-- Офицер #1 - работа с делами, улики, отчеты
DO $$
DECLARE
    officer_id BIGINT;
BEGIN
    SELECT user_id INTO officer_id FROM users WHERE username = 'officer_1';

    INSERT INTO user_permissions (user_id, privilege) VALUES
        (officer_id, 'CASES:*:READ'),
        (officer_id, 'CASES:*:UPDATE'),
        (officer_id, 'CASES:*:CLUES_ADD'),
        (officer_id, 'CASES:*:CLUES_UPDATE'),
        (officer_id, 'CASES:*:TEMPLATE_ADD'),
        (officer_id, 'CLUES:*:READ'),
        (officer_id, 'CLUES:*:UPDATE'),
        (officer_id, 'FILES:*:UPLOAD'),
        (officer_id, 'FILES:*:READ'),
        (officer_id, 'TEMPLATES:*:READ'),
        (officer_id, 'USERS:*:READ');
END $$;

-- Офицер #2 - чтение дел, добавление улик
DO $$
DECLARE
    officer_id BIGINT;
BEGIN
    SELECT user_id INTO officer_id FROM users WHERE username = 'officer_2';

    INSERT INTO user_permissions (user_id, privilege) VALUES
        (officer_id, 'CASES:*:READ'),
        (officer_id, 'CASES:*:CLUES_ADD'),
        (officer_id, 'CLUES:*:READ'),
        (officer_id, 'FILES:*:UPLOAD'),
        (officer_id, 'FILES:*:READ');
END $$;

-- Следователь - сбор улик
DO $$
DECLARE
    inv_id BIGINT;
BEGIN
    SELECT user_id INTO inv_id FROM users WHERE username = 'investigator_1';

    INSERT INTO user_permissions (user_id, privilege) VALUES
        (inv_id, 'CASES:*:READ'),
        (inv_id, 'CASES:*:CLUES_ADD'),
        (inv_id, 'CASES:*:SUSPECT_ADD'),
        (inv_id, 'CLUES:*:READ'),
        (inv_id, 'CLUES:*:UPDATE'),
        (inv_id, 'FILES:*:UPLOAD'),
        (inv_id, 'FILES:*:READ'),
        (inv_id, 'SUSPECTS:*:ADD'),
        (inv_id, 'SUSPECTS:*:READ');
END $$;

-- Эксперт - работа с уликами
DO $$
DECLARE
    forensics_id BIGINT;
BEGIN
    SELECT user_id INTO forensics_id FROM users WHERE username = 'forensics_1';

    INSERT INTO user_permissions (user_id, privilege) VALUES
        (forensics_id, 'CASES:*:READ'),
        (forensics_id, 'CASES:*:CLUES_ADD'),
        (forensics_id, 'CLUES:*:READ'),
        (forensics_id, 'CLUES:*:UPDATE'),
        (forensics_id, 'FILES:*:UPLOAD'),
        (forensics_id, 'FILES:*:READ');
END $$;

-- Администратор - полные права
DO $$
DECLARE
    admin_id BIGINT;
BEGIN
    SELECT user_id INTO admin_id FROM users WHERE username = 'admin';

    INSERT INTO user_permissions (user_id, privilege) VALUES
        (admin_id, 'INCIDENTS:*:*'),
        (admin_id, 'CASES:*:*'),
        (admin_id, 'CLUES:*:*'),
        (admin_id, 'TEMPLATES:*:*'),
        (admin_id, 'SUSPECTS:*:*'),
        (admin_id, 'FILES:*:*'),
        (admin_id, 'USERS:*:*'),
        (admin_id, 'LOGS:*:READ'),
        (admin_id, 'NOTIFICATIONS:*:*');
END $$;

-- Viewer - только чтение (для негативных тестов)
DO $$
DECLARE
    viewer_id BIGINT;
BEGIN
    SELECT user_id INTO viewer_id FROM users WHERE username = 'viewer';

    INSERT INTO user_permissions (user_id, privilege) VALUES
        (viewer_id, 'CASES:*:READ'),
        (viewer_id, 'INCIDENTS:*:READ'),
        (viewer_id, 'CLUES:*:READ'),
        (viewer_id, 'USERS:*:READ');
END $$;

-- ==============================================
-- ДОПОЛНИТЕЛЬНЫЕ ПОЛЬЗОВАТЕЛИ (для ranked users)
-- ==============================================

-- Создаем еще 10 офицеров на разном расстоянии от центра
-- для тестирования функции ранжирования

DO $$
DECLARE
    positions TEXT[] := ARRAY['PATROL', 'SENIOR_PATROL', 'RAPID_RESPONSE', 'INVESTIGATOR'];
    base_lat FLOAT := 59.9343;
    base_lon FLOAT := 30.3351;
BEGIN
    FOR i IN 1..10 LOOP
        INSERT INTO users (user_id, username, name, surname, patronymic, primary_position, password,
                           location_latitude, location_longitude, location_altitude)
        VALUES (
            nextval('users_seq'),
            'ranked_user_' || i,
            'Офицер',
            'Ранжированный' || i,
            'Тестович',
            positions[(i % 4) + 1],
            '$2a$10$vWM0Nxese0N2eJuHeSBzLO9p0ah093rX0GQrpnW9NK5tYbvLHOElu', -- test123
            base_lat + (random() * 0.1 - 0.05),  -- +/- 5km от центра
            base_lon + (random() * 0.1 - 0.05),
            (random() * 30)  -- 0-30m высота
        );

        -- Даем базовые права для участия в делах
        INSERT INTO user_permissions (user_id, privilege)
        SELECT currval('users_seq'), 'CASES:*:READ';
    END LOOP;
END $$;

-- ==============================================
-- ТЕСТОВЫЙ ИНЦИДЕНТ (для E2E сценария)
-- ==============================================

-- Инцидент: Кража в магазине на Невском проспекте
DO $$
DECLARE
    dispatcher_id BIGINT;
    incident_timestamp TIMESTAMP := NOW() - INTERVAL '2 hours';
BEGIN
    SELECT user_id INTO dispatcher_id FROM users WHERE username = 'dispatcher';

    INSERT INTO incidents (
        incident_id, creator_user_id, created_at, updator_user_id, updated_at,
        applicant, type, reason,
        location_latitude, location_longitude, location_altitude,
        estimated_time, wait_for_assignment
    ) VALUES (
        nextval('incidents_seq'),
        dispatcher_id,
        incident_timestamp,
        dispatcher_id,
        incident_timestamp,
        'Администратор магазина "Продукты 24/7"',
        'Кража',
        'Кража товаров из магазина на ул. Невский пр., д. 15. Предположительная сумма ущерба 50000р. Время инцидента: около 14:00. На месте работает охрана, есть видеозаписи.',
        59.9311,  -- Невский проспект
        30.3609,
        10.0,
        incident_timestamp + INTERVAL '30 minutes',
        false  -- Создаем дело сразу
    );
END $$;

-- ==============================================
-- ПРИМЕРЫ ДОПОЛНИТЕЛЬНЫХ ИНЦИДЕНТОВ
-- (для тестирования списков и фильтров)
-- ==============================================

DO $$
DECLARE
    dispatcher_id BIGINT;
BEGIN
    SELECT user_id INTO dispatcher_id FROM users WHERE username = 'dispatcher';

    -- Инцидент #2: Нарушение общественного порядка
    INSERT INTO incidents (
        incident_id, creator_user_id, created_at, updator_user_id, updated_at,
        applicant, type, reason,
        location_latitude, location_longitude, location_altitude,
        estimated_time, wait_for_assignment
    ) VALUES (
        nextval('incidents_seq'),
        dispatcher_id,
        NOW() - INTERVAL '1 day',
        dispatcher_id,
        NOW() - INTERVAL '1 day',
        'Жалоба от жильцов',
        'Нарушение порядка',
        'Шумная компания в парке. Просьба разобраться.',
        59.9200,
        30.3400,
        5.0,
        NOW() - INTERVAL '23 hours',
        true  -- Ждет назначения
    );

    -- Инцидент #3: ДТП
    INSERT INTO incidents (
        incident_id, creator_user_id, created_at, updator_user_id, updated_at,
        applicant, type, reason,
        location_latitude, location_longitude, location_altitude,
        estimated_time, wait_for_assignment
    ) VALUES (
        nextval('incidents_seq'),
        dispatcher_id,
        NOW() - INTERVAL '3 hours',
        dispatcher_id,
        NOW() - INTERVAL '3 hours',
        'Водитель автомобиля',
        'ДТП',
        'Столкновение двух автомобилей на перекрестке. Пострадавших нет.',
        59.9500,
        30.3200,
        8.0,
        NOW() - INTERVAL '2 hours 30 minutes',
        true
    );
END $$;

-- ==============================================
-- СПРАВОЧНЫЕ ДАННЫЕ
-- ==============================================

-- Позиции (primary positions) - используются для ранжирования
-- Примечание: эти данные могут храниться в enum или отдельной таблице
-- Здесь для справки:
-- PATROL, SENIOR_PATROL, RAPID_RESPONSE, INVESTIGATOR, SENIOR_INVESTIGATOR,
-- OPERATOR, DISPATCHER, ANALYST, FORENSICS, CYBERSECURITY, DRONE_OPERATOR,
-- K9_UNIT, NEGOTIATOR, SPECIAL_FORCES, TRAFFIC_UNIT, COMMUNITY_LIAISON,
-- TECH_ENGINEER, INTERNAL_AFFAIRS

-- ==============================================
-- СТАТИСТИКА
-- ==============================================

\echo '=============================================='
\echo 'E2E Test Data Generation Completed!'
\echo '=============================================='
\echo ''

SELECT 'users' as table_name, count(*) as row_count FROM users
UNION ALL
SELECT 'user_permissions', count(*) FROM user_permissions
UNION ALL
SELECT 'incidents', count(*) FROM incidents
ORDER BY table_name;

\echo ''
\echo 'Test Users Created:'
\echo '  - dispatcher / test123 (DISPATCHER)'
\echo '  - officer_1 / test123 (PATROL)'
\echo '  - officer_2 / test123 (SENIOR_PATROL)'
\echo '  - investigator_1 / test123 (INVESTIGATOR)'
\echo '  - forensics_1 / test123 (FORENSICS)'
\echo '  - admin / test123 (OPERATOR - full permissions)'
\echo '  - viewer / test123 (ANALYST - read only)'
\echo '  - ranked_user_1..10 / test123 (various positions)'
\echo ''
\echo 'Test Incidents Created:'
\echo '  - Incident #1: Кража в магазине (59.9311, 30.3609) - ready for case'
\echo '  - Incident #2: Нарушение порядка - waiting for assignment'
\echo '  - Incident #3: ДТП - waiting for assignment'
\echo ''
\echo 'Ready for E2E business scenario testing!'
\echo '=============================================='

-- ==============================================
-- ПОЛЕЗНЫЕ ЗАПРОСЫ ДЛЯ ТЕСТИРОВАНИЯ
-- ==============================================

-- Получить ID пользователей для тестов:
-- SELECT user_id, username, primary_position FROM users WHERE username LIKE 'dispatcher' OR username LIKE 'officer%';

-- Получить ID инцидента для теста:
-- SELECT incident_id, type, reason FROM incidents WHERE type = 'Кража';

-- Проверить права пользователя:
-- SELECT u.username, up.privilege
-- FROM users u
-- JOIN user_permissions up ON u.user_id = up.user_id
-- WHERE u.username = 'officer_1';

-- Ранжирование пользователей от места инцидента:
-- SELECT u.username, u.primary_position,
--        SQRT(
--          POWER((u.location_latitude - 59.9311) * 111000, 2) +
--          POWER((u.location_longitude - 30.3609) * 111000 * COS(RADIANS(59.9311)), 2) +
--          POWER(u.location_altitude - 10.0, 2)
--        ) as distance_meters
-- FROM users u
-- WHERE u.primary_position IN ('PATROL', 'SENIOR_PATROL', 'INVESTIGATOR', 'FORENSICS')
-- ORDER BY distance_meters ASC
-- LIMIT 10;
