import React, { useEffect, useState } from 'react';
import { apiBase, authHeaders, isUnauthorized } from '../api/api';
import { useAuth } from '../context/AuthContext';
import Status from '../components/Status';
import { humanSize } from '../utils/format';

export default function ModerationPage({ active, onRequireAuth }) {
    const { token, isModerator, isAdmin } = useAuth();

    const [status, setStatus] = useState({ msg: '', type: '' });
    const [users, setUsers] = useState([]);
    const [selectedUserId, setSelectedUserId] = useState(null);

    const [userDetails, setUserDetails] = useState(null);
    const [userVideos, setUserVideos] = useState([]);

    function setStatusModeration(msg = '', type = '') {
        setStatus({ msg, type });
    }

    async function loadUsers() {
        if (!token || !isModerator) {
            setUsers([]);
            return;
        }

        try {
            const res = await fetch(apiBase + '/moderation/users', { headers: authHeaders(token) });

            if (isUnauthorized(res)) {
                setStatusModeration('Сессия истекла, войдите снова', 'error');
                onRequireAuth?.();
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            const list = await res.json();
            setUsers(list || []);
        } catch (e) {
            setStatusModeration('Ошибка загрузки пользователей: ' + e.message, 'error');
        }
    }

    useEffect(() => {
        if (!active) return;
        setStatusModeration('');
        if (!token || !isModerator) return;
        loadUsers();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [active, token, isModerator]);

    async function loadUserDetails(id) {
        if (!token || !isModerator) return;

        try {
            const res = await fetch(apiBase + '/moderation/users/' + id, { headers: authHeaders(token) });

            if (res.status === 404) {
                setUserDetails(null);
                return;
            }

            if (isUnauthorized(res)) {
                setStatusModeration('Сессия истекла, войдите снова', 'error');
                onRequireAuth?.();
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            const u = await res.json();
            setSelectedUserId(u.id);
            setUserDetails(u);
        } catch (e) {
            setStatusModeration('Ошибка загрузки деталей: ' + e.message, 'error');
        }
    }

    async function loadUserVideos(id) {
        if (!token || !isModerator) return;

        try {
            const res = await fetch(apiBase + '/moderation/users/' + id + '/videos', { headers: authHeaders(token) });

            if (res.status === 404) {
                setUserVideos([]);
                return;
            }

            if (isUnauthorized(res)) {
                setStatusModeration('Сессия истекла, войдите снова', 'error');
                onRequireAuth?.();
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            const list = await res.json();
            setUserVideos(list || []);
        } catch (e) {
            setStatusModeration('Ошибка загрузки видео пользователя: ' + e.message, 'error');
        }
    }

    async function toggleBan(u) {
        if (!token || !isModerator) return;

        const action = u.enabled ? 'ban' : 'unban';
        const url = apiBase + `/moderation/users/${u.id}/${action}`;

        try {
            const res = await fetch(url, { method: 'POST', headers: authHeaders(token) });

            if (isUnauthorized(res)) {
                setStatusModeration('Сессия истекла, войдите снова', 'error');
                onRequireAuth?.();
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            setStatusModeration(u.enabled ? 'Пользователь забанен' : 'Пользователь разбанен', 'ok');
            await loadUsers();
            if (selectedUserId === u.id) loadUserDetails(u.id);
        } catch (e) {
            setStatusModeration('Ошибка изменения статуса: ' + e.message, 'error');
        }
    }

    async function saveRoles(userId, roles) {
        if (!token || !isAdmin) return;

        try {
            const res = await fetch(apiBase + '/admin/users/' + userId + '/roles', {
                method: 'POST',
                headers: { ...authHeaders(token), 'Content-Type': 'application/json' },
                body: JSON.stringify({ roles }),
            });

            if (isUnauthorized(res)) {
                setStatusModeration('Сессия истекла, войдите снова', 'error');
                onRequireAuth?.();
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            setStatusModeration('Роли обновлены', 'ok');
            await loadUsers();
            if (selectedUserId === userId) loadUserDetails(userId);
        } catch (e) {
            setStatusModeration('Ошибка сохранения ролей: ' + e.message, 'error');
        }
    }

    async function rebuildThumbs() {
        if (!token || !isAdmin) return;
        if (!confirm('Перегенерировать превью для всех видео? Это может занять время.')) return;

        try {
            const res = await fetch(apiBase + '/admin/thumbnails/rebuild', {
                method: 'POST',
                headers: authHeaders(token),
            });

            if (isUnauthorized(res)) {
                setStatusModeration('Сессия истекла, войдите снова', 'error');
                onRequireAuth?.();
                return;
            }

            if (!res.ok && res.status !== 202) throw new Error('HTTP ' + res.status);

            setStatusModeration('Запущена перегенерация превью (может занять время)', 'ok');
        } catch (e) {
            setStatusModeration('Ошибка запуска перегенерации: ' + e.message, 'error');
        }
    }

    if (!active) return null;

    if (!token || !isModerator) {
        return (
            <div className={'card page ' + (active ? 'active' : '')} id="pageModeration">
                <div className="header">
                    <div className="title">Модерация</div>
                    <div className="badge" id="modUsersCount">—</div>
                </div>
                <div className="studio-row-meta">Нет прав для модерации</div>
            </div>
        );
    }

    return (
        <div className={'card page ' + (active ? 'active' : '')} id="pageModeration">
            <div className="header">
                <div className="title">Модерация</div>
                <div className="badge" id="modUsersCount">{users.length} пользователей</div>
            </div>

            <div className="studio-section-title">Сервис</div>
            <button
                className="full small"
                id="btnRebuildThumbs"
                style={{ marginBottom: 8, display: isAdmin ? '' : 'none' }}
                onClick={rebuildThumbs}
            >
                Перегенерировать превью всех видео
            </button>

            <div className="studio-section-title">Пользователи</div>
            <div className="studio-videos" id="modUsersList">
                {!users.length ? (
                    <div className="studio-row-meta">Пользователи не найдены</div>
                ) : (
                    users.map((u) => (
                        <div key={u.id} className="studio-row">
                            <div className="studio-row-header">
                                <div className="studio-row-title">{u.username} ({u.displayName})</div>
                                <div>
                                    <button className="small secondary" onClick={() => loadUserDetails(u.id)}>Детали</button>{' '}
                                    <button className="small secondary" onClick={() => loadUserVideos(u.id)}>Видео</button>{' '}
                                    <button className="small danger" onClick={() => toggleBan(u)}>
                                        {u.enabled ? 'Забанить' : 'Разбанить'}
                                    </button>
                                </div>
                            </div>
                            <div className="studio-row-meta">
                                ID: {u.id} · Email: {u.email} · Роли: {(Array.isArray(u.roles) ? u.roles.join(', ') : '—')} · Статус: {u.enabled ? 'активен' : 'забанен'}
                            </div>
                        </div>
                    ))
                )}
            </div>

            <div className="studio-section-title" style={{ marginTop: 10 }}>Детали пользователя</div>
            <div className="studio-jobs" id="modUserDetails" style={{ minHeight: 60 }}>
                {!userDetails ? (
                    <div className="studio-row-meta">Выберите пользователя</div>
                ) : (
                    <UserDetailsCard user={userDetails} canEditRoles={isAdmin} onSaveRoles={saveRoles} />
                )}
            </div>

            <div className="studio-section-title" style={{ marginTop: 10 }}>Видео пользователя</div>
            <div className="studio-jobs" id="modUserVideos" style={{ minHeight: 60 }}>
                {!userVideos || userVideos.length === 0 ? (
                    <div className="studio-row-meta">Видео пользователя будут здесь</div>
                ) : (
                    userVideos.map((v) => (
                        <div key={v.uuid} className="job-row">
                            <div className="job-row-header">
                                <div>{v.name || '(без названия)'}</div>
                                <div>UUID: {v.uuid}</div>
                            </div>
                            <div>{humanSize(v.contentSize)} · {v.contentType || ''} · {v.uploadedAt || ''}</div>
                        </div>
                    ))
                )}
            </div>

            <Status id="statusModeration" message={status.msg} type={status.type} />
        </div>
    );
}

function UserDetailsCard({ user, canEditRoles, onSaveRoles }) {
    const [roles, setRoles] = useState(() => new Set(user.roles || []));

    useEffect(() => {
        setRoles(new Set(user.roles || []));
    }, [user]);

    const allRoles = ['USER', 'MODERATOR', 'ADMIN'];

    return (
        <div className="job-row">
            <div className="job-row-header">
                <div>{user.username} ({user.displayName})</div>
                <div>ID: {user.id}</div>
            </div>
            <div>Email: {user.email} · Статус: {user.enabled ? 'активен' : 'забанен'}</div>

            <div style={{ marginTop: 4 }}>
                <div>Роли: {(Array.isArray(user.roles) ? user.roles.join(', ') : '—')}</div>

                {canEditRoles ? (
                    <>
                        <div style={{ marginTop: 4 }}>
                            {allRoles.map((r) => (
                                <label key={r} style={{ fontSize: 11, marginRight: 8 }}>
                                    <input
                                        type="checkbox"
                                        value={r}
                                        checked={roles.has(r)}
                                        onChange={(e) => {
                                            const next = new Set(roles);
                                            if (e.target.checked) next.add(r);
                                            else next.delete(r);
                                            setRoles(next);
                                        }}
                                        style={{ marginRight: 3 }}
                                    />
                                    {r}
                                </label>
                            ))}
                        </div>

                        <button className="small" style={{ marginTop: 4 }} onClick={() => onSaveRoles?.(user.id, Array.from(roles))}>
                            Сохранить роли
                        </button>
                    </>
                ) : null}
            </div>
        </div>
    );
}
