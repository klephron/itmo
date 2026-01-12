import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';

// === Настройки стресс-теста ===
export const options = {
    scenarios: {
        stress_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                // Фаза 1: Нормальная нагрузка
                { duration: '2m', target: 100 },

                // Фаза 2: Увеличение нагрузки
                { duration: '2m', target: 300 },

                // Фаза 3: Стресс (breaking point)
                { duration: '2m', target: 600 },

                // Фаза 4: Extreme stress
                { duration: '2m', target: 1000 },

                // Фаза 5: Peak stress
                { duration: '2m', target: 1500 },

                // Фаза 6: Recovery test
                { duration: '2m', target: 100 },

                // Фаза 7: Cooldown
                { duration: '1m', target: 0 },
            ],
        },
    },
    thresholds: {
        // Более мягкие пороги для стресс-теста
        http_req_failed: ['rate<0.10'], // До 10% ошибок допустимо
        http_req_duration: ['p(95)<3000', 'p(99)<5000'],
    },
};

// === Конфиг ===
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || 'test-jwt-token';

// Метрики
const stressErrors = new Counter('stress_errors');
const stressSuccess = new Counter('stress_success');
const responseTime = new Trend('stress_response_time');
const errorRate = new Rate('stress_error_rate');

let VIDEOS = [];

export function setup() {
    console.log('=== Stress Test Setup ===');

    const res = http.get(`${BASE_URL}/api/streaming/video/list?page=0&size=50`);

    if (res.status !== 200) {
        return { videos: [] };
    }

    try {
        const data = res.json();
        const videos = data.content.filter(v => v.uuid);
        console.log(`Loaded ${videos.length} videos for stress test`);
        return { videos };
    } catch (e) {
        return { videos: [] };
    }
}

export default function (data) {
    if (!data.videos || data.videos.length === 0) {
        sleep(1);
        return;
    }

    // Микс различных операций для создания реалистичной нагрузки
    const operations = [
        () => getFeed(),
        () => streamVideo(data.videos),
        () => likeRandomVideo(data.videos),
        () => searchVideos(),
        () => viewProfile(data.videos),
        () => checkHealth(),
    ];

    // Выполняем случайную операцию
    const operation = operations[Math.floor(Math.random() * operations.length)];

    const start = Date.now();
    try {
        operation();
        const duration = Date.now() - start;
        responseTime.add(duration);
        stressSuccess.add(1);
        errorRate.add(false);
    } catch (e) {
        stressErrors.add(1);
        errorRate.add(true);
    }

    // Под высокой нагрузкой sleep короче
    sleep(0.1 + Math.random() * 0.5);
}

function getFeed() {
    const page = Math.floor(Math.random() * 10);
    const res = http.get(`${BASE_URL}/api/streaming/video/list?page=${page}&size=10`);
    check(res, { 'feed ok': (r) => r.status === 200 });
}

function streamVideo(videos) {
    const video = videos[Math.floor(Math.random() * videos.length)];
    const start = Math.floor(Math.random() * Math.max(0, video.size - CHUNK_SIZE));
    const end = start + CHUNK_SIZE - 1;

    const res = http.get(
        `${BASE_URL}/api/streaming/video/${video.uuid}`,
        { headers: { 'Range': `bytes=${start}-${end}` } }
    );

    check(res, { 'stream ok': (r) => r.status === 206 });
}

function likeRandomVideo(videos) {
    const video = videos[Math.floor(Math.random() * videos.length)];
    const res = http.post(
        `${BASE_URL}/api/interactions/videos/${video.uuid}/like`,
        null,
        { headers: { 'Authorization': `Bearer ${AUTH_TOKEN}` } }
    );

    check(res, { 'like ok': (r) => r.status === 200 || r.status === 204 });
}

function searchVideos() {
    const queries = ['test', 'demo', 'video', 'sample', 'tutorial'];
    const query = queries[Math.floor(Math.random() * queries.length)];

    const res = http.get(`${BASE_URL}/api/search/videos?q=${query}&page=0&size=10`);
    check(res, { 'search ok': (r) => r.status === 200 });
}

function viewProfile(videos) {
    const video = videos[Math.floor(Math.random() * videos.length)];
    if (video.uploaderId) {
        const res = http.get(`${BASE_URL}/api/users/${video.uploaderId}`);
        check(res, { 'profile ok': (r) => r.status === 200 });
    }
}

function checkHealth() {
    const res = http.get(`${BASE_URL}/actuator/health`);
    check(res, { 'health ok': (r) => r.status === 200 });
}

export function teardown(data) {
    console.log('=== Stress Test Complete ===');
    console.log('Check metrics for breaking point analysis');
}
