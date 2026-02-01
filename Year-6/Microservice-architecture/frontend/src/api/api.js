// Используем переменную окружения для URL
export const apiBase = import.meta.env.VITE_BACKEND_URL || '';  // В dev это будет http://localhost:8080, в prod — https://your-production-url.com

export function authHeaders(token) {
    const h = {};
    if (token) h['Authorization'] = 'Bearer ' + token;
    return h;
}

export function deviceIdHeaders(deviceId) {
    const h = {};
    if (deviceId) h['X-Device-Id'] = deviceId;
    return h;
}

export function isUnauthorized(res) {
    return res.status === 401 || res.status === 403;
}

export const getAvatarUrl = (userId, forceReload = false) => (
    userId ? `${apiBase}/api/users/${userId}/avatar${forceReload ? `?t=${Date.now()}` : ""}` : null
);

export function getOrCreateDeviceId() {
    const key = 'feed-device-id';
    let id = localStorage.getItem(key);
    if (!id) {
        id = crypto.randomUUID();
        localStorage.setItem(key, id);
    }
    return id;
}

export async function throwError(res) {
    const fallback = `HTTP ${res.status}`;

    // 1. Пробуем JSON
    try {
        const data = await res.clone().json();

        if (data && typeof data === 'object') {
            let message = data.message || data.error || fallback;

            // 2. Ошибки валидации
            if (Array.isArray(data.fields) && data.fields.length > 0) {
                const details = data.fields
                    .map((f) => {
                        if (!f) return null;
                        return `${f.field} - ${f.message || 'неверно'}`;
                    })
                    .filter(Boolean)
                    .join(', ');

                if (details) message += ` (${details})`;
            }

            throw new Error(message);
        }
    } catch (e) {
        // важно: если это уже Error — пробрасываем дальше
        if (e instanceof Error) {
            throw e;
        }
        // иначе — это ошибка JSON.parse, идём дальше
    }

    // 3. Пробуем text/plain
    try {
        const text = await res.clone().text();
        if (text) {
            throw new Error(text);
        }
    } catch (e) {
        if (e instanceof Error) {
            throw e;
        }
    }

    // 4. Только код ошибки
    throw new Error(fallback);
}