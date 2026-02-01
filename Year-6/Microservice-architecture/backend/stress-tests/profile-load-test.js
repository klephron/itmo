import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

// === Настройки нагрузки ===
export const options = {
    scenarios: {
        profile_browsing: {
            executor: 'ramping-vus',
            startVUs: 10,
            stages: [
                { duration: '30s', target: 50 },
                { duration: '1m', target: 100 },
                { duration: '2m', target: 200 },
                { duration: '1m', target: 300 },
                { duration: '1m', target: 100 },
                { duration: '30s', target: 0 },
            ],
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.02'],
        http_req_duration: ['p(95)<600'],
        'profile_view_duration': ['p(95)<400'],
        'follow_duration': ['p(95)<300'],
    },
};

// === Конфиг ===
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || 'test-jwt-token';

// Метрики
const profileViewDuration = new Trend('profile_view_duration');
const followDuration = new Trend('follow_duration');
const unfollowDuration = new Trend('unfollow_duration');
const profileErrors = new Counter('profile_errors');
const followOperations = new Counter('follow_operations');
const unfollowOperations = new Counter('unfollow_operations');
const profilesViewed = new Counter('profiles_viewed');
const videosViewed = new Counter('user_videos_viewed');

let USER_IDS = [];

export function setup() {
    console.log('=== Profile Load Test Setup ===');

    // Получаем список пользователей (из видео)
    const res = http.get(`${BASE_URL}/api/streaming/video/list?page=0&size=100`);

    if (res.status !== 200) {
        return { userIds: [] };
    }

    try {
        const data = res.json();
        const userIds = [...new Set(
            data.content
                .filter(v => v.uploaderId)
                .map(v => v.uploaderId)
        )];

        console.log(`Found ${userIds.length} unique users`);
        return { userIds };
    } catch (e) {
        return { userIds: [] };
    }
}

export default function (data) {
    if (!data.userIds || data.userIds.length === 0) {
        sleep(1);
        return;
    }

    const targetUserId = data.userIds[Math.floor(Math.random() * data.userIds.length)];
    const currentUserId = __VU;

    // 1. Просмотр профиля
    viewProfile(targetUserId);
    sleep(1);

    // 2. Просмотр видео пользователя
    viewUserVideos(targetUserId);
    sleep(1);

    // 3. Follow/Unfollow операции
    if (Math.random() < 0.6) {
        // 60% пользователей подписываются
        performFollow(currentUserId, targetUserId);
        sleep(2);

        // 20% из подписавшихся отписываются
        if (Math.random() < 0.2) {
            sleep(5); // Подождали немного
            performUnfollow(currentUserId, targetUserId);
        }
    }

    // 4. Просмотр подписчиков/подписок
    if (Math.random() < 0.4) {
        viewFollowers(targetUserId);
        sleep(0.5);
        viewFollowing(targetUserId);
    }

    sleep(1);
}

function viewProfile(userId) {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/users/${userId}`);

    profileViewDuration.add(Date.now() - start);

    const ok = check(res, {
        'profile view ok': (r) => r.status === 200,
        'profile has data': (r) => {
            try {
                const data = r.json();
                return data.id !== undefined;
            } catch (e) {
                return false;
            }
        },
    });

    if (ok) {
        profilesViewed.add(1);
    } else {
        profileErrors.add(1);
    }
}

function viewUserVideos(userId) {
    const res = http.get(
        `${BASE_URL}/api/users/${userId}/videos?page=0&size=10`
    );

    const ok = check(res, {
        'user videos ok': (r) => r.status === 200,
    });

    if (ok) {
        try {
            const data = res.json();
            videosViewed.add(data.content ? data.content.length : 0);
        } catch (e) {}
    } else {
        profileErrors.add(1);
    }
}

function performFollow(followerId, targetId) {
    const start = Date.now();
    const res = http.post(
        `${BASE_URL}/api/profile/follow/${targetId}`,
        null,
        { headers: { 'Authorization': `Bearer ${AUTH_TOKEN}` } }
    );

    followDuration.add(Date.now() - start);

    const ok = check(res, {
        'follow ok': (r) => r.status === 200 || r.status === 204,
    });

    if (ok) {
        followOperations.add(1);
    } else {
        profileErrors.add(1);
    }
}

function performUnfollow(followerId, targetId) {
    const start = Date.now();
    const res = http.del(
        `${BASE_URL}/api/profile/follow/${targetId}`,
        null,
        { headers: { 'Authorization': `Bearer ${AUTH_TOKEN}` } }
    );

    unfollowDuration.add(Date.now() - start);

    const ok = check(res, {
        'unfollow ok': (r) => r.status === 200 || r.status === 204,
    });

    if (ok) {
        unfollowOperations.add(1);
    } else {
        profileErrors.add(1);
    }
}

function viewFollowers(userId) {
    http.get(
        `${BASE_URL}/api/users/${userId}/followers?page=0&size=20`
    );
}

function viewFollowing(userId) {
    http.get(
        `${BASE_URL}/api/users/${userId}/following?page=0&size=20`
    );
}

export function teardown(data) {
    console.log('=== Profile Load Test Complete ===');
}
