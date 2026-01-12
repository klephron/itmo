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