import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

// === Настройки нагрузки ===
export const options = {
    // сценарий: постепенно увеличиваем VU
    scenarios: {
        ramping_viewers: {
            executor: 'ramping-vus',
            startVUs: 1,
            stages: [
                { duration: '30s', target: 10 },   // разогрев
                { duration: '1m', target: 100 },
                { duration: '1m', target: 200 },
                { duration: '1m', target: 300 },
                { duration: '1m', target: 400 },
                { duration: '1m', target: 500 },
                { duration: '1m', target: 0 },     // плавное завершение
            ],
            gracefulRampDown: '30s',
        },
    },

    // базовые пороги
    thresholds: {
        http_req_failed: ['rate<0.01'],             // не более 1% ошибок
        http_req_duration: ['p(95)<500'],           // 95% запросов < 500мс (на вкус)
    },
};

// === Конфиг ===
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const CHUNK_SIZE = 1024 * 1024; // 1 МБ
// Можно передать VIDEO_IDS через env, но проще подтянуть из /list
let VIDEO_IDS = [];

// Метрики
const streamDuration = new Trend('stream_req_duration');
const streamErrors = new Counter('stream_errors');

export function setup() {
    const res = http.get(`${BASE_URL}/streaming/video/list`);
    check(res, { 'list ok': (r) => r.status === 200 });

    let json;
    try {
        json = res.json();
    } catch (e) {
        json = [];
    }

    if (!Array.isArray(json) || json.length === 0) {
        console.error('Нет видео в /streaming/video/list. Залей хотя бы 1–2 ролика.');
        return { videos: [] };
    }

    const videos = json
        .filter((v) => v.uuid && v.contentSize)
        .map((v) => ({
            uuid: v.uuid,
            size: v.contentSize,
            name: v.name,
        }));

    console.log(`Найдено видео: ${videos.length}`);
    return { videos };
}

export default function (data) {
    const vids = data.videos;
    if (!vids || vids.length === 0) {
        sleep(1);
        return;
    }

    const v = vids[Math.floor(Math.random() * vids.length)];
    const fileSize = v.size;

    // если файл меньше чанка — просто берём с начала
    if (!fileSize || fileSize <= CHUNK_SIZE) {
        const headers = { Range: `bytes=0-${CHUNK_SIZE - 1}` };
        const res = http.get(`${BASE_URL}/streaming/video/${v.uuid}`, { headers });
        const ok = check(res, {
            'status is 206': (r) => r.status === 206,
            'has body': (r) => r.body && r.body.length > 0,
        });
        streamDuration.add(res.timings.duration);
        if (!ok) streamErrors.add(1);
        sleep(1);
        return;
    }

    // максимум, откуда можно стартовать, чтобы не вылезти за конец
    const maxStart = Math.max(0, fileSize - CHUNK_SIZE);
    const start = Math.floor(Math.random() * (maxStart + 1));
    const end = start + CHUNK_SIZE - 1;

    const headers = {
        Range: `bytes=${start}-${end}`,
    };

    const res = http.get(`${BASE_URL}/streaming/video/${v.uuid}`, { headers });

    const ok = check(res, {
        'status is 206': (r) => r.status === 206,
        'has body': (r) => r.body && r.body.length > 0,
    });

    streamDuration.add(res.timings.duration);
    if (!ok) {
        streamErrors.add(1);
    }

    sleep(1);
}
