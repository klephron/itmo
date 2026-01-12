import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';

// === Настройки нагрузки ===
export const options = {
    scenarios: {
        search_users: {
            executor: 'ramping-vus',
            startVUs: 5,
            stages: [
                { duration: '30s', target: 20 },
                { duration: '1m', target: 50 },
                { duration: '2m', target: 100 },
                { duration: '1m', target: 150 },
                { duration: '2m', target: 100 },
                { duration: '30s', target: 0 },
            ],
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.02'],
        http_req_duration: ['p(95)<800'],
        'search_duration': ['p(95)<1000'],
        'suggest_duration': ['p(95)<500'],
    },
};

// === Конфиг ===
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Метрики
const searchDuration = new Trend('search_duration');
const suggestDuration = new Trend('suggest_duration');
const searchErrors = new Counter('search_errors');
const suggestErrors = new Counter('suggest_errors');
const searchesPerformed = new Counter('searches_performed');
const suggestsPerformed = new Counter('suggests_performed');

// Поисковые запросы
const searchQueries = new SharedArray('search-queries', function () {
    return [
        'tutorial',
        'gaming',
        'music',
        'vlog',
        'review',
        'how to',
        'funny',
        'cooking',
        'travel',
        'education',
        'tech',
        'science',
        'news',
        'sports',
        'fitness',
        'test',
        'demo',
        'sample',
        'performance',
        'benchmark',
    ];
});

export default function () {
    const query = searchQueries[Math.floor(Math.random() * searchQueries.length)];

    // 70% пользователей делают полный поиск
    if (Math.random() < 0.7) {
        performSearch(query);
        sleep(2 + Math.random() * 3);
    }

    // 50% используют автодополнение
    if (Math.random() < 0.5) {
        performSuggest(query.substring(0, 3 + Math.floor(Math.random() * 5)));
        sleep(0.5);
    }

    // Иногда пользователь листает результаты поиска
    if (Math.random() < 0.4) {
        const page = Math.floor(Math.random() * 5) + 1;
        performSearchWithPagination(query, page);
    }

    sleep(1);
}

function performSearch(query) {
    const start = Date.now();
    const res = http.get(
        `${BASE_URL}/api/search/videos?q=${encodeURIComponent(query)}&page=0&size=20`
    );

    searchDuration.add(Date.now() - start);

    const ok = check(res, {
        'search status 200': (r) => r.status === 200,
        'search has results': (r) => {
            try {
                const data = r.json();
                return data.content !== undefined;
            } catch (e) {
                return false;
            }
        },
    });

    if (ok) {
        searchesPerformed.add(1);
    } else {
        searchErrors.add(1);
    }
}

function performSuggest(prefix) {
    const start = Date.now();
    const res = http.get(
        `${BASE_URL}/api/search/suggest?q=${encodeURIComponent(prefix)}&limit=5`
    );

    suggestDuration.add(Date.now() - start);

    const ok = check(res, {
        'suggest status 200': (r) => r.status === 200,
    });

    if (ok) {
        suggestsPerformed.add(1);
    } else {
        suggestErrors.add(1);
    }
}

function performSearchWithPagination(query, page) {
    const res = http.get(
        `${BASE_URL}/api/search/videos?q=${encodeURIComponent(query)}&page=${page}&size=10`
    );

    check(res, {
        'paginated search ok': (r) => r.status === 200,
    });
}

export function teardown(data) {
    console.log('=== Search Load Test Complete ===');
}
