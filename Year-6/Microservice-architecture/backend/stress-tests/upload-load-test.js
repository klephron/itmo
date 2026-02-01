import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';
import encoding from 'k6/encoding';

// === Настройки нагрузки ===
export const options = {
    scenarios: {
        upload_burst: {
            executor: 'ramping-vus',
            startVUs: 1,
            stages: [
                { duration: '30s', target: 10 },
                { duration: '2m', target: 50 },
                { duration: '1m', target: 100 },
                { duration: '2m', target: 50 },
                { duration: '30s', target: 0 },
            ],
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<5000'],  // Загрузка может быть медленнее
        'upload_duration': ['p(95)<10000'],
    },
};

// === Конфиг ===
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || 'test-jwt-token';

// Метрики
const uploadDuration = new Trend('upload_duration');
const uploadErrors = new Counter('upload_errors');
const uploadSuccess = new Counter('upload_success');
const jobStatusChecks = new Counter('job_status_checks');

// Тестовые данные для имитации видео файлов
const testVideoNames = new SharedArray('video-names', function () {
    return [
        'test-video-1.mp4',
        'demo-clip-2.mp4',
        'sample-video-3.mp4',
        'performance-test-4.mp4',
        'load-test-video-5.mp4',
        'stress-test-6.mp4',
        'benchmark-7.mp4',
        'scenario-test-8.mp4',
    ];
});

const testDescriptions = new SharedArray('descriptions', function () {
    return [
        'Performance test video',
        'Load testing scenario',
        'Stress test upload',
        'K6 benchmark video',
        'Upload stress test',
        null, // Иногда без описания
    ];
});

export default function () {
    const videoName = testVideoNames[Math.floor(Math.random() * testVideoNames.length)];
    const description = testDescriptions[Math.floor(Math.random() * testDescriptions.length)];

    // Создаем "фейковый" multipart file (небольшой, чтобы не перегружать)
    // В реальном тесте можно использовать настоящие видео файлы
    const fakeVideoSize = 100 * 1024; // 100 KB для теста
    const fakeVideoData = new Uint8Array(fakeVideoSize);
    for (let i = 0; i < fakeVideoSize; i++) {
        fakeVideoData[i] = Math.floor(Math.random() * 256);
    }

    const boundary = '----WebKitFormBoundary7MA4YWxkTrZu0gW';
    const formData = buildMultipartFormData({
        video: {
            fileName: videoName,
            contentType: 'video/mp4',
            data: fakeVideoData,
        },
        videoName: videoName,
        description: description,
    }, boundary);

    const uploadStart = Date.now();

    const uploadRes = http.post(
        `${BASE_URL}/api/streaming/video/upload`,
        formData,
        {
            headers: {
                'Content-Type': `multipart/form-data; boundary=${boundary}`,
                'Authorization': `Bearer ${AUTH_TOKEN}`,
            },
            timeout: '30s',
        }
    );

    const uploadTime = Date.now() - uploadStart;
    uploadDuration.add(uploadTime);

    const uploadOk = check(uploadRes, {
        'upload status 200': (r) => r.status === 200,
        'has jobId': (r) => {
            try {
                const json = r.json();
                return json.jobId !== undefined;
            } catch (e) {
                return false;
            }
        },
    });

    if (!uploadOk) {
        uploadErrors.add(1);
        sleep(1);
        return;
    }

    uploadSuccess.add(1);

    let jobId;
    try {
        jobId = uploadRes.json().jobId;
    } catch (e) {
        uploadErrors.add(1);
        return;
    }

    sleep(1);

    // Проверяем статус загрузки (polling)
    let checks = 0;
    const maxChecks = 10;

    while (checks < maxChecks) {
        const statusRes = http.get(
            `${BASE_URL}/api/streaming/video/upload/status/${jobId}`,
            {
                headers: { 'Authorization': `Bearer ${AUTH_TOKEN}` },
            }
        );

        jobStatusChecks.add(1);
        checks++;

        const statusOk = check(statusRes, {
            'status check ok': (r) => r.status === 200 || r.status === 404,
        });

        if (!statusOk) {
            uploadErrors.add(1);
            break;
        }

        try {
            const status = statusRes.json();
            if (status.stage === 'DONE' || status.stage === 'ERROR') {
                break;
            }
        } catch (e) {}

        sleep(2);
    }

    sleep(1);
}

function buildMultipartFormData(fields, boundary) {
    let body = '';

    for (const [name, value] of Object.entries(fields)) {
        if (value === null || value === undefined) continue;

        body += `--${boundary}\r\n`;

        if (typeof value === 'object' && value.fileName) {
            // File field
            body += `Content-Disposition: form-data; name="${name}"; filename="${value.fileName}"\r\n`;
            body += `Content-Type: ${value.contentType}\r\n\r\n`;
            // В k6 нельзя напрямую добавить бинарные данные, используем base64
            body += encoding.b64encode(value.data);
            body += '\r\n';
        } else {
            // Regular field
            body += `Content-Disposition: form-data; name="${name}"\r\n\r\n`;
            body += value;
            body += '\r\n';
        }
    }

    body += `--${boundary}--\r\n`;
    return body;
}

export function teardown(data) {
    console.log('=== Upload Load Test Complete ===');
}
