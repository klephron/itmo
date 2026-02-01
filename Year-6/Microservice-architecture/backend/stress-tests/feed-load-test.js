import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';

// === Настройки нагрузки ===
export const options = {
    scenarios: {
        // Сценарий 1: Постоянная нагрузка (пользователи листают ленту)
        constant_feed_load: {
            executor: 'constant-vus',
            vus: 50,
            duration: '3m',
        },
        // Сценарий 2: Пиковая нагрузка (burst traffic)
        spike_load: {
            executor: 'ramping-vus',
            startTime: '3m',
            startVUs: 50,
            stages: [
                { duration: '30s', target: 500 },  // резкий скачок
                { duration: '1m', target: 500 },   // удержание пика
                { duration: '30s', target: 50 },   // возврат к норме
            ],
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.02'],
        http_req_duration: ['p(95)<1000', 'p(99)<2000'],
        'feed_page_duration': ['p(95)<800'],
    },
};

// === Конфиг ===
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PAGE_SIZE = 10;
const MAX_PAGES_TO_SCROLL = 20;

// Метрики
const feedPageDuration = new Trend('feed_page_duration');
const feedErrors = new Counter('feed_errors');
const feedSuccessRate = new Rate('feed_success_rate');
const sessionCreated = new Counter('feed_sessions_created');
const itemsViewed = new Counter('feed_items_viewed');

export function setup() {
    console.log('=== Feed Load Test Setup ===');
    // Проверяем доступность API
    const res = http.get(`${BASE_URL}/api/streaming/video/list?page=0&size=1`);
    check(res, { 'API available': (r) => r.status === 200 });
    return {};
}

export default function () {
    // Симулируем пользователя, листающего ленту

    // 1. Получить первую страницу
    const page1Start = Date.now();
    const res1 = http.get(`${BASE_URL}/api/streaming/video/list?page=0&size=${PAGE_SIZE}`);
    const ok1 = check(res1, {
        'page 1 status 200': (r) => r.status === 200,
        'page 1 has content': (r) => {
            try {
                const data = r.json();
                return data.content && Array.isArray(data.content);
            } catch (e) {
                return false;
            }
        },
    });

    feedPageDuration.add(Date.now() - page1Start);
    feedSuccessRate.add(ok1);
    if (!ok1) feedErrors.add(1);

    try {
        const data = res1.json();
        itemsViewed.add(data.content ? data.content.length : 0);
    } catch (e) {}

    sleep(2 + Math.random() * 3); // Пользователь смотрит видео 2-5 сек

    // 2. Листать ленту (paginated scrolling)
    const pagesToScroll = Math.floor(Math.random() * Math.min(MAX_PAGES_TO_SCROLL, 10)) + 1;

    for (let page = 1; page < pagesToScroll; page++) {
        const pageStart = Date.now();
        const res = http.get(`${BASE_URL}/api/streaming/video/list?page=${page}&size=${PAGE_SIZE}`);

        const ok = check(res, {
            [`page ${page} ok`]: (r) => r.status === 200,
        });

        feedPageDuration.add(Date.now() - pageStart);
        feedSuccessRate.add(ok);
        if (!ok) feedErrors.add(1);

        try {
            const data = res.json();
            if (data.content && data.content.length === 0) {
                // Дошли до конца ленты
                break;
            }
            itemsViewed.add(data.content ? data.content.length : 0);
        } catch (e) {}

        sleep(1 + Math.random() * 2);
    }

    // 3. Иногда пользователь использует Feed Session API
    if (Math.random() < 0.3) {
        testFeedSession();
    }

    sleep(1);
}

function testFeedSession() {
    const deviceId = `device-${__VU}`;

    // Создать сессию
    const startRes = http.post(
        `${BASE_URL}/api/streaming/video/feed/session/start?count=10`,
        null,
        { headers: { 'X-Device-Id': deviceId } }
    );

    const sessionOk = check(startRes, {
        'session created': (r) => r.status === 200,
    });

    if (!sessionOk) {
        feedErrors.add(1);
        return;
    }

    sessionCreated.add(1);

    let feedSession;
    try {
        feedSession = startRes.json();
    } catch (e) {
        feedErrors.add(1);
        return;
    }

    if (!feedSession.feedId) {
        feedErrors.add(1);
        return;
    }

    sleep(2);

    // Листать next несколько раз
    const swipes = Math.floor(Math.random() * 5) + 1;
    for (let i = 0; i < swipes; i++) {
        const nextRes = http.get(
            `${BASE_URL}/api/streaming/video/feed/session/${feedSession.feedId}/next?count=1`,
            { headers: { 'X-Device-Id': deviceId } }
        );

        check(nextRes, {
            'next items ok': (r) => r.status === 200,
        });

        sleep(3 + Math.random() * 2); // Смотрим видео
    }

    // Иногда скролим назад
    if (Math.random() < 0.3) {
        const prevRes = http.get(
            `${BASE_URL}/api/streaming/video/feed/session/${feedSession.feedId}/prev`,
            { headers: { 'X-Device-Id': deviceId } }
        );

        check(prevRes, {
            'prev items ok': (r) => r.status === 200,
        });
    }
}

export function teardown(data) {
    console.log('=== Feed Load Test Complete ===');
}
