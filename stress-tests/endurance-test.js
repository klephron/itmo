import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter, Gauge } from 'k6/metrics';

// === Настройки теста на выносливость ===
export const options = {
    scenarios: {
        endurance: {
            executor: 'constant-vus',
            vus: 100,
            duration: '30m', // Длительный тест
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.02'],
        http_req_duration: ['p(95)<800'],
        'memory_leak_indicator': ['value<2.0'], // Проверка на утечки памяти
    },
};

// === Конфиг ===
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || 'test-jwt-token';

// Метрики для обнаружения утечек
const memoryLeakIndicator = new Gauge('memory_leak_indicator');
const responseTimeGrowth = new Trend('response_time_growth');
const errorRateOverTime = new Counter('errors_over_time');
const requestsPerMinute = new Counter('requests_per_minute');

let initialResponseTime = 0;
let requestCount = 0;
let VIDEOS = [];

export function setup() {
    console.log('=== Endurance Test Setup ===');
    console.log('Duration: 30 minutes');
    console.log('VUs: 100 constant');

    const res = http.get(`${BASE_URL}/api/streaming/video/list?page=0&size=50`);

    if (res.status !== 200) {
        return { videos: [], startTime: Date.now() };
    }

    try {
        const data = res.json();
        return {
            videos: data.content.filter(v => v.uuid),
            startTime: Date.now()
        };
    } catch (e) {
        return { videos: [], startTime: Date.now() };
    }
}

export default function (data) {
    requestCount++;
    requestsPerMinute.add(1);

    if (!data.videos || data.videos.length === 0) {
        sleep(1);
        return;
    }

    const iteration = __ITER;
    const elapsedMinutes = (Date.now() - data.startTime) / 60000;

    // Выполняем типичный набор операций
    const operations = [
        () => testFeedEndurance(),
        () => testStreamEndurance(data.videos),
        () => testInteractionsEndurance(data.videos),
        () => testSearchEndurance(),
    ];

    const start = Date.now();
    const operation = operations[iteration % operations.length];

    try {
        operation();
        const duration = Date.now() - start;

        // Отслеживаем рост времени ответа
        if (initialResponseTime === 0) {
            initialResponseTime = duration;
        } else {
            const growth = duration / initialResponseTime;
            memoryLeakIndicator.add(growth);
            responseTimeGrowth.add(duration);
        }
    } catch (e) {
        errorRateOverTime.add(1);
    }

    // Логируем каждые 5 минут
    if (iteration % 300 === 0) {
        console.log(`[${elapsedMinutes.toFixed(1)}m] Iteration ${iteration}, Requests: ${requestCount}`);
    }

    sleep(0.5 + Math.random() * 1.5);
}

function testFeedEndurance() {
    const page = Math.floor(Math.random() * 20);
    const res = http.get(`${BASE_URL}/api/streaming/video/list?page=${page}&size=10`);
    check(res, { 'feed endurance ok': (r) => r.status === 200 });
}

function testStreamEndurance(videos) {
    const video = videos[Math.floor(Math.random() * videos.length)];
    const res = http.get(
        `${BASE_URL}/api/streaming/video/${video.uuid}`,
        { headers: { 'Range': 'bytes=0-1048575' } }
    );
    check(res, { 'stream endurance ok': (r) => r.status === 206 });
}

function testInteractionsEndurance(videos) {
    const video = videos[Math.floor(Math.random() * videos.length)];

    // View
    http.post(
        `${BASE_URL}/api/interactions/videos/${video.uuid}/view`,
        null,
        { headers: { 'Authorization': `Bearer ${AUTH_TOKEN}` } }
    );

    // Like с 50% вероятностью
    if (Math.random() < 0.5) {
        http.post(
            `${BASE_URL}/api/interactions/videos/${video.uuid}/like`,
            null,
            { headers: { 'Authorization': `Bearer ${AUTH_TOKEN}` } }
        );
    }
}

function testSearchEndurance() {
    const queries = ['test', 'video', 'demo', 'sample'];
    const query = queries[Math.floor(Math.random() * queries.length)];

    const res = http.get(`${BASE_URL}/api/search/videos?q=${query}&page=0&size=10`);
    check(res, { 'search endurance ok': (r) => r.status === 200 });
}

export function teardown(data) {
    const duration = (Date.now() - data.startTime) / 60000;
    console.log('=== Endurance Test Complete ===');
    console.log(`Total duration: ${duration.toFixed(2)} minutes`);
    console.log(`Total requests: ${requestCount}`);
    console.log('Check metrics for memory leaks and performance degradation');
}
