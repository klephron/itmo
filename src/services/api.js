const API_BASE = import.meta.env.VITE_BACKEND_URL ?? "";

/**
 * Универсальный fetch с обработкой ошибок
 */
async function request(url, options = {}) {
    const opts = {
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        },
        ...options
    };

    try {
        const res = await fetch(url, opts);

        if (!res.ok) {
            let message = `HTTP error ${res.status}`;
            try {
                const json = await res.json();
                if (json?.message) message = json.message;
            } catch {
                /* ignore */
            }
            throw new Error(message);
        }

        // если сервер вернул пустой ответ — не падаем
        const text = await res.text();
        try {
            return text ? JSON.parse(text) : null;
        } catch {
            return text;
        }

    } catch (err) {
        console.error("API request failed:", err);
        throw err;
    }
}

/* ============================
   AUTH
   ============================ */

export const api = {
    login: ({ email, password }) =>
        request(`${API_BASE}/auth/login`, {
            method: "POST",
            body: JSON.stringify({ email, password })
        }),

    register: ({ email, password }) =>
        request(`${API_BASE}/auth/register`, {
            method: "POST",
            body: JSON.stringify({ email, password })
        }),

    me: () => request(`${API_BASE}/auth/me`),

    /* ============================
       FEED
       ============================ */

    getVideosPage: (page = 0, size = 10) =>
        request(`${API_BASE}/streaming/video/list?page=${page}&size=${size}`),

    likeVideo: ({ uuid }) =>
        request(`${API_BASE}/streaming/videos/${uuid}/like`, {
            method: "POST"
        }),

    viewVideo: ({ uuid }) =>
        request(`${API_BASE}/videos/${uuid}/view`, {
            method: "POST"
        }),

    getComments: ({ videoUuid }) =>
        request(`${API_BASE}/videos/${videoUuid}/comments`),

    addComment: ({ videoUuid, text }) =>
        request(`${API_BASE}/videos/${videoUuid}/comments`, {
            method: "POST",
            body: JSON.stringify({ text })
        }),

    /* ============================
       PROFILE
       ============================ */

    getUserProfile: ({ userId }) =>
        request(`${API_BASE}/users/${userId}/profile`),

    getUserVideos: ({ userId, page = 0, size = 10 }) =>
        request(`${API_BASE}/users/${userId}/videos?page=${page}&size=${size}`),

    followUser: ({ userId }) =>
        request(`${API_BASE}/users/${userId}/follow`, {
            method: "POST"
        }),

    getFollowing: ({ userId }) =>
        request(`${API_BASE}/users/${userId}/following`),

    uploadAvatar: ({ file }) => {
        const form = new FormData();
        form.append("file", file);

        return fetch(`${API_BASE}/users/me/avatar`, {
            method: "POST",
            body: form
        }).then((r) => r.json());
    },

    /* ============================
       VIDEO UPLOAD JOBS
       ============================ */

    getUploadStatus: ({ jobId }) =>
        request(`/streaming/video/upload/status/${jobId}`),

    getActiveJobs: () =>
        request(`/streaming/video/upload/active`),

    /* ============================
       MODERATION
       ============================ */

    listUsersModeration: () =>
        request(`/moderation/users`),

    getUserModeration: ({ userId }) =>
        request(`/moderation/users/${userId}`),

    getUserModerationVideos: ({ userId }) =>
        request(`/moderation/users/${userId}/videos`),

    getUserRolesAdmin: ({ userId }) =>
        request(`/admin/users/${userId}/roles`),
};