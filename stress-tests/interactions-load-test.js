import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';

// === Настройки нагрузки ===
export const options = {
    scenarios: {
        // Активные пользователи лайкают, комментируют, смотрят
        active_users: {
            executor: 'ramping-arrival-rate',
            startRate: 10,
            timeUnit: '1s',
            preAllocatedVUs: 50,
            maxVUs: 500,
            stages: [
                { duration: '1m', target: 50 },
                { duration: '2m', target: 100 },
                { duration: '2m', target: 200 },
                { duration: '1m', target: 50 },
                { duration: '1m', target: 10 },
            ],
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.02'],
        http_req_duration: ['p(95)<500'],
        'like_duration': ['p(95)<300'],
        'view_duration': ['p(95)<200'],
        'comment_duration': ['p(95)<500'],
    },
};

// === Конфиг ===
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || 'test-jwt-token';

// Метрики
const likeDuration = new Trend('like_duration');
const viewDuration = new Trend('view_duration');
const commentDuration = new Trend('comment_duration');
const likeErrors = new Counter('like_errors');
const viewErrors = new Counter('view_errors');
const commentErrors = new Counter('comment_errors');
const likesCreated = new Counter('likes_created');
const viewsCreated = new Counter('views_created');
const commentsCreated = new Counter('comments_created');

let VIDEO_IDS = [];

export function setup() {
    console.log('=== Interactions Load Test Setup ===');

    // Получаем список видео
    const res = http.get(`${BASE_URL}/api/streaming/video/list?page=0&size=50`);

    if (res.status !== 200) {
        console.error('Failed to fetch videos');
        return { videoIds: [] };
    }

    try {
        const data = res.json();
        const videoIds = data.content
            .filter(v => v.uuid)
            .map(v => v.uuid);

        console.log(`Found ${videoIds.length} videos for testing`);
        return { videoIds };
    } catch (e) {
        return { videoIds: [] };
    }
}

export default function (data) {
    if (!data.videoIds || data.videoIds.length === 0) {
        sleep(1);
        return;
    }

    const videoId = data.videoIds[Math.floor(Math.random() * data.videoIds.length)];
    const userId = __VU; // Каждая VU = свой пользователь

    // 80% пользователей лайкают
    if (Math.random() < 0.8) {
        performLike(videoId, userId);
        sleep(0.5);
    }

    // 95% регистрируют просмотр
    if (Math.random() < 0.95) {
        performView(videoId, userId);
        sleep(0.5);
    }

    // 30% оставляют комментарий
    if (Math.random() < 0.3) {
        performComment(videoId, userId);
        sleep(0.5);
    }

    // 20% обновляют прогресс просмотра
    if (Math.random() < 0.2) {
        updateWatchProgress(videoId, userId);
    }

    sleep(1 + Math.random() * 2);
}

function performLike(videoId, userId) {
    const start = Date.now();
    const res = http.post(
        `${BASE_URL}/api/interactions/videos/${videoId}/like`,
        null,
        { headers: { 'Authorization': `Bearer ${AUTH_TOKEN}` } }
    );

    likeDuration.add(Date.now() - start);

    const ok = check(res, {
        'like status ok': (r) => r.status === 200 || r.status === 204,
    });

    if (ok) {
        likesCreated.add(1);
    } else {
        likeErrors.add(1);
    }
}

function performView(videoId, userId) {
    const start = Date.now();
    const res = http.post(
        `${BASE_URL}/api/interactions/videos/${videoId}/view`,
        null,
        { headers: { 'Authorization': `Bearer ${AUTH_TOKEN}` } }
    );

    viewDuration.add(Date.now() - start);

    const ok = check(res, {
        'view status ok': (r) => r.status === 200 || r.status === 204,
    });

    if (ok) {
        viewsCreated.add(1);
    } else {
        viewErrors.add(1);
    }
}

function performComment(videoId, userId) {
    const comments = [
        'Great video!',
        'Nice content',
        'Interesting!',
        'Thanks for sharing',
        'Amazing work',
    ];

    const comment = comments[Math.floor(Math.random() * comments.length)];

    const start = Date.now();
    const res = http.post(
        `${BASE_URL}/api/interactions/videos/${videoId}/comments`,
        JSON.stringify({ text: comment }),
        {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${AUTH_TOKEN}`,
            },
        }
    );

    commentDuration.add(Date.now() - start);

    const ok = check(res, {
        'comment status ok': (r) => r.status === 200 || r.status === 201,
    });

    if (ok) {
        commentsCreated.add(1);
    } else {
        commentErrors.add(1);
    }
}

function updateWatchProgress(videoId, userId) {
    const watchedSeconds = Math.floor(Math.random() * 300) + 10; // 10-310 сек
    const totalSeconds = 600; // Предполагаем 10 мин видео

    http.post(
        `${BASE_URL}/api/interactions/videos/${videoId}/watch-progress`,
        JSON.stringify({
            watchedSeconds,
            totalSeconds,
        }),
        {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${AUTH_TOKEN}`,
            },
        }
    );
}

export function teardown(data) {
    console.log('=== Interactions Load Test Complete ===');
}
