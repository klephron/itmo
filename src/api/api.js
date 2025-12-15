// Используем переменную окружения для URL
export const apiBase = import.meta.env.VITE_BACKEND_URL || '';  // В dev это будет http://localhost:8080, в prod — https://your-production-url.com

export function authHeaders(token) {
    const h = {};
    if (token) h['Authorization'] = 'Bearer ' + token;
    return h;
}

export function parseVideoListPayload(data) {
    const items =
        Array.isArray(data?.items) ? data.items :
            Array.isArray(data?.content) ? data.content :
                Array.isArray(data) ? data :
                    [];
    const total =
        typeof data?.total === 'number' ? data.total :
            typeof data?.totalElements === 'number' ? data.totalElements :
                items.length;
    const page =
        typeof data?.page === 'number' ? data.page :
            typeof data?.number === 'number' ? data.number :
                0;
    const hasNext =
        typeof data?.hasNext === 'boolean' ? data.hasNext :
            typeof data?.last === 'boolean' ? !data.last :
                items.length > 0;
    return { items, total, page, hasNext };
}

export function isUnauthorized(res) {
    return res.status === 401 || res.status === 403;
}
