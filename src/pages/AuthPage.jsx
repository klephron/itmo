import React, { useEffect, useRef, useState } from 'react';
import { apiBase, authHeaders, isUnauthorized } from '../api/api';
import { useAuth } from '../context/AuthContext';
import Avatar from '../components/Avatar';
import Status from '../components/Status';
import ThumbGrid, { ThumbCard } from '../components/ThumbGrid';

export default function AuthPage({
                                     active,
                                     onOpenProfile,
                                     onRequireAuth,
                                     onGlobalPlayByUuid,
                                     onReloadFeed,
                                     jobsState,
                                     onLoadActiveJobsFromServer,
                                     onRegisterLoadMore,
                                 }) {
    const { token, user, setAuth } = useAuth();

    const [status, setStatus] = useState({ msg: '', type: '' });

    const [login, setLogin] = useState({ username: '', password: '' });
    const [reg, setReg] = useState({ username: '', email: '', displayName: '', password: '' });

    const [profile, setProfile] = useState(null);
    const [myVideos, setMyVideos] = useState([]);
    const [myVideosPaging, setMyVideosPaging] = useState({ page: 0, size: 12, last: false, loading: false });

    const [following, setFollowing] = useState([]);
    const [followingStatus, setFollowingStatus] = useState('');

    const avatarFileRef = useRef(null);

    function setStatusAuth(msg = '', type = '') {
        setStatus({ msg, type });
    }

    async function doLogin() {
        const username = login.username.trim();
        const password = login.password;

        if (!username || !password) {
            setStatusAuth('Введите логин и пароль', 'error');
            return;
        }

        try {
            const res = await fetch(apiBase + '/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password }),
            });

            if (res.status === 401 || res.status === 403) {
                setStatusAuth('Неверный логин или пароль', 'error');
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            const data = await res.json();
            const nextUser = {
                userId: data.userId,
                username: data.username,
                displayName: data.displayName,
                roles: Array.isArray(data.roles) ? data.roles : [],
                avatarUrl: data.avatarUrl ?? null,
            };
            setAuth(data.token, nextUser);
            setStatusAuth('Успешный вход', 'ok');

            await loadAccountProfile(nextUser, data.token);
            await onLoadActiveJobsFromServer?.(data.token);
        } catch (e) {
            setStatusAuth('Ошибка входа: ' + e.message, 'error');
        }
    }

    async function doRegister() {
        const username = reg.username.trim();
        const email = reg.email.trim();
        const displayName = reg.displayName.trim();
        const password = reg.password;

        if (!username || !email || !displayName || !password) {
            setStatusAuth('Заполните все поля регистрации', 'error');
            return;
        }

        try {
            const res = await fetch(apiBase + '/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, email, displayName, password }),
            });

            if (res.status === 409) {
                setStatusAuth('Такой логин или email уже существует', 'error');
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            const data = await res.json();
            const nextUser = {
                userId: data.userId,
                username: data.username,
                displayName: data.displayName,
                roles: Array.isArray(data.roles) ? data.roles : [],
                avatarUrl: data.avatarUrl ?? null,
            };
            setAuth(data.token, nextUser);
            setStatusAuth('Регистрация прошла успешно', 'ok');

            await loadAccountProfile(nextUser, data.token);
            await onLoadActiveJobsFromServer?.(data.token);
        } catch (e) {
            setStatusAuth('Ошибка регистрации: ' + e.message, 'error');
        }
    }

    function logout() {
        setAuth(null, null);
        setStatusAuth('Вы вышли из аккаунта', 'ok');
        setProfile(null);
        setMyVideos([]);
        setFollowing([]);
    }

    async function uploadAvatar() {
        if (!token) {
            setStatusAuth('Войдите, чтобы обновлять аватар', 'error');
            onRequireAuth?.();
            return;
        }
        const file = avatarFileRef.current?.files?.[0];
        if (!file) {
            setStatusAuth('Выберите файл аватара', 'error');
            return;
        }

        const fd = new FormData();
        fd.append('avatar', file);

        try {
            const res = await fetch(apiBase + '/api/users/me/avatar', {
                method: 'POST',
                headers: authHeaders(token),
                body: fd,
            });

            if (isUnauthorized(res)) {
                setStatusAuth('Сессия истекла, войдите снова', 'error');
                onRequireAuth?.();
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            const data = await res.json(); // { url }
            setAuth(token, { ...user, avatarUrl: data.url });

            setStatusAuth('Аватар обновлён', 'ok');
            await loadAccountProfile({ ...user, avatarUrl: data.url }, token);
        } catch (e) {
            setStatusAuth('Ошибка загрузки аватара: ' + e.message, 'error');
        }
    }

    async function loadAccountFollowing(userId, tokenOverride = token) {
        if (!userId || !tokenOverride) {
            setFollowing([]);
            setFollowingStatus('Войдите, чтобы видеть подписки');
            return;
        }

        setFollowingStatus('Загрузка подписок…');

        try {
            const res = await fetch(apiBase + `/api/users/${userId}/following`, {
                headers: authHeaders(tokenOverride),
            });

            if (isUnauthorized(res)) {
                setFollowing([]);
                setFollowingStatus('Сессия истекла, войдите снова');
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            const list = await res.json();
            setFollowing(list || []);
            setFollowingStatus('');
        } catch (e) {
            setFollowing([]);
            setFollowingStatus('Ошибка загрузки подписок: ' + e.message);
        }
    }

    async function loadAccountProfile(u = user, tokenOverride = token) {
        if (!u || !tokenOverride) {
            setProfile(null);
            setMyVideos([]);
            setMyVideosPaging({ page: 0, size: 12, last: false, loading: false });
            return;
        }

        setMyVideosPaging({ page: 0, size: 12, last: false, loading: false });

        try {
            const [userRes, vidsRes] = await Promise.all([
                fetch(apiBase + '/api/users/' + u.userId + '/profile', { headers: authHeaders(tokenOverride) }),
                fetch(apiBase + `/api/users/${u.userId}/videos?page=0&size=12`, { headers: authHeaders(tokenOverride) }),
            ]);

            if (!userRes.ok || !vidsRes.ok) throw new Error('HTTP ' + userRes.status + '/' + vidsRes.status);

            const prof = await userRes.json();
            const vidsPage = await vidsRes.json();

            const vids = Array.isArray(vidsPage?.content) ? vidsPage.content : (Array.isArray(vidsPage) ? vidsPage : []);
            const totalVideos = typeof vidsPage?.totalElements === 'number' ? vidsPage.totalElements : vids.length;

            setProfile({ ...prof, totalVideos });

            setMyVideos(vids);
            setMyVideosPaging({
                page: (typeof vidsPage?.number === 'number' ? vidsPage.number : 0) + 1,
                size: 12,
                last: !!vidsPage?.last,
                loading: false,
            });

            loadAccountFollowing(u.userId, tokenOverride);
        } catch (e) {
            setProfile(null);
            setMyVideos([]);
        }
    }

    async function loadMoreMyVideos() {
        if (!user || !token) return;
        if (myVideosPaging.loading || myVideosPaging.last) return;

        setMyVideosPaging((s) => ({ ...s, loading: true }));

        try {
            const res = await fetch(apiBase + `/api/users/${user.userId}/videos?page=${myVideosPaging.page}&size=${myVideosPaging.size}`, {
                headers: authHeaders(token),
            });

            if (isUnauthorized(res)) {
                setStatusAuth('Сессия истекла, войдите снова', 'error');
                onRequireAuth?.();
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);
            const page = await res.json();
            const vids = page?.content || [];

            setMyVideos((prev) => prev.concat(vids));
            setMyVideosPaging((s) => ({
                ...s,
                page: (typeof page?.number === 'number' ? page.number : s.page) + 1,
                last: page?.last === true,
                loading: false,
            }));
            setStatusAuth('', '');
        } catch (e) {
            setStatusAuth('Ошибка загрузки видео: ' + e.message, 'error');
            setMyVideosPaging((s) => ({ ...s, loading: false }));
        }
    }

    async function deleteVideo(uuid) {
        if (!token) {
            setStatusAuth('Войдите, чтобы удалять видео', 'error');
            onRequireAuth?.();
            return;
        }
        if (!confirm('Удалить это видео?')) return;

        try {
            setStatusAuth('Удаляю видео…', 'ok');
            const res = await fetch(apiBase + '/streaming/video/delete/' + uuid, {
                method: 'POST',
                headers: authHeaders(token),
            });

            if (isUnauthorized(res)) {
                setStatusAuth('Сессия истекла, войдите снова', 'error');
                onRequireAuth?.();
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            setStatusAuth('Видео удалено', 'ok');
            await onReloadFeed?.();
            await loadAccountProfile();
        } catch (e) {
            setStatusAuth('Ошибка удаления: ' + e.message, 'error');
        }
    }

    // register load-more handler for parent scroll
    useEffect(() => {
        if (!active) return;
        onRegisterLoadMore?.(() => loadMoreMyVideos());
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [active, myVideosPaging, user?.userId, token]);

    useEffect(() => {
        if (!active) return;
        if (user && token) {
            loadAccountProfile();
            onLoadActiveJobsFromServer?.(token);
        } else {
            setProfile(null);
            setMyVideos([]);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [active, user?.userId, token]);

    if (!active) return null;

    const userShort = user ? (user.displayName || user.username || 'Пользователь') : 'Гость';
    const followers = profile?.followersCount ?? 0;
    const followingCount = profile?.followingCount ?? 0;
    const totalVideos = profile?.totalVideos ?? (myVideos?.length || 0);

    return (
        <div className={'card page ' + (active ? 'active' : '')} id="pageAuth">
            {user ? (
                <>
                    <div id="authAvatar" className="profile-avatar" style={{ display: '' }}>
                        {/* Используем apiBase для аватара */}
                        {user.avatarUrl ? <img src={`${apiBase}${user.avatarUrl}`} alt="" /> : null}
                    </div>

                    <div className="form-row" id="avatarUploadRow" style={{ display: '' }}>
                        <label htmlFor="avatarFile">Аватар</label>
                        <input id="avatarFile" ref={avatarFileRef} type="file" accept="image/*" />
                        <button className="full small" id="btnAvatarUpload" onClick={uploadAvatar}>
                            Обновить аватар
                        </button>
                    </div>
                </>
            ) : (
                <div id="authAvatar" className="profile-avatar" style={{ display: 'none' }} />
            )}

            <div className="header">
                <div className="title">Аккаунт</div>
                <div className="badge" id="authUserShort">{userShort}</div>
            </div>

            {user ? (
                <div id="authLoggedBlock" style={{ display: '' }}>
                    <div className="status ok" id="authWelcome">
                        Вы вошли как {user.displayName || user.username}
                    </div>
                    <button className="full danger small" id="btnLogout" onClick={logout}>
                        Выйти
                    </button>
                </div>
            ) : null}

            {user ? (
                <div id="authProfileBlock" style={{ display: '', marginTop: 12 }}>
                    <div className="studio-section-title">Ваш профиль</div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
                        <div className="profile-avatar" id="authProfileAvatar">
                            {/* Используем apiBase для аватара */}
                            {profile?.avatarUrl ? <img src={`${apiBase}${profile.avatarUrl}`} alt="" /> : (user.avatarUrl ? <img src={`${apiBase}${user.avatarUrl}`} alt="" /> : null)}
                        </div>
                        <div>
                            <div className="title" id="authProfileDisplayName">
                                {profile?.displayName || profile?.username || user.displayName || user.username}
                            </div>
                            <div className="studio-row-meta" id="authProfileUsername">
                                @{profile?.username || user.username}
                            </div>
                            <div className="studio-row-meta" id="authProfileStats">
                                {totalVideos} видео · {followers} подписчиков · {followingCount} подписок
                            </div>
                        </div>
                    </div>

                    <div className="studio-section-title" style={{ marginTop: 8 }}>Мои подписки</div>
                    <div className="studio-videos" id="authFollowingList">
                        {followingStatus ? (
                            <div className="studio-row-meta">{followingStatus}</div>
                        ) : (
                            (!following || following.length === 0) ? (
                                <div className="studio-row-meta">Подписок пока нет</div>
                            ) : (
                                following.map((u) => (
                                    <div key={u.id} className="studio-row" style={{ cursor: 'pointer' }} onClick={() => onOpenProfile?.(u.id)}>
                                        <div className="studio-row-header">
                                            <div className="studio-row-title">{u.displayName || u.username}</div>
                                            <Avatar className="avatar-mini" url={u.avatarUrl} />
                                        </div>
                                        <div className="studio-row-meta">@{u.username}</div>
                                    </div>
                                ))
                            )
                        )}
                    </div>

                    <div className="studio-section-title" style={{ marginTop: 8 }}>Активные загрузки</div>
                    <div className="studio-jobs" id="studioJobs">
                        {jobsState?.activeJobs?.length ? (
                            jobsState.activeJobs.map((job) => {
                                const st = job.lastStatus || { stage: 'UPLOADING', percent: 0, message: 'Загрузка…' };
                                return (
                                    <div className="job-row" key={job.jobId}>
                                        <div className="job-row-header">
                                            <div>{job.fileName || '(без названия)'}</div>
                                            <div>{st.percent ?? 0}%</div>
                                        </div>
                                        <div>{st.message || st.stage}</div>
                                        <div className="job-progress-bar">
                                            <div className="job-progress-fill" style={{ width: (st.percent ?? 0) + '%' }} />
                                        </div>
                                    </div>
                                );
                            })
                        ) : (
                            <div className="studio-row-meta">Нет активных загрузок</div>
                        )}
                    </div>

                    <div className="status" id="statusStudio">{jobsState?.statusStudio || ''}</div>

                    <div className="studio-section-title" style={{ marginTop: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span>Ваши видео</span>
                        <span className="badge" id="studioVideoCount">{totalVideos} видео</span>
                    </div>

                    {(!myVideos || myVideos.length === 0) ? (
                        <div className="studio-row-meta">Видео пока нет</div>
                    ) : (
                        <ThumbGrid
                            videos={myVideos}
                            renderCard={(v) => (
                                <ThumbCard
                                    key={v.uuid}
                                    video={v}
                                    showDelete={true}
                                    onDelete={() => deleteVideo(v.uuid)}
                                    onOpen={() => onGlobalPlayByUuid?.(v.uuid)}
                                />
                            )}
                        />
                    )}
                </div>
            ) : null}

            {!user ? (
                <div id="authFormsBlock">
                    <div className="studio-section-title">Вход</div>
                    <div className="form-row">
                        <label htmlFor="loginUsername">Логин</label>
                        <input id="loginUsername" type="text" placeholder="username"
                               value={login.username}
                               onChange={(e) => setLogin((s) => ({ ...s, username: e.target.value }))}
                        />
                    </div>
                    <div className="form-row">
                        <label htmlFor="loginPassword">Пароль</label>
                        <input id="loginPassword" type="password" placeholder="••••••••"
                               value={login.password}
                               onChange={(e) => setLogin((s) => ({ ...s, password: e.target.value }))}
                               onKeyDown={(e) => { if (e.key === 'Enter') doLogin(); }}
                        />
                    </div>
                    <button className="full" id="btnLogin" onClick={doLogin}>Войти</button>

                    <div className="studio-section-title" style={{ marginTop: 12 }}>Регистрация</div>
                    <div className="form-row">
                        <label htmlFor="regUsername">Логин</label>
                        <input id="regUsername" type="text" placeholder="username"
                               value={reg.username}
                               onChange={(e) => setReg((s) => ({ ...s, username: e.target.value }))}
                        />
                    </div>
                    <div className="form-row">
                        <label htmlFor="regEmail">Email</label>
                        <input id="regEmail" type="email" placeholder="you@example.com"
                               value={reg.email}
                               onChange={(e) => setReg((s) => ({ ...s, email: e.target.value }))}
                        />
                    </div>
                    <div className="form-row">
                        <label htmlFor="regDisplayName">Отображаемое имя</label>
                        <input id="regDisplayName" type="text" placeholder="Как показывать вас в ленте?"
                               value={reg.displayName}
                               onChange={(e) => setReg((s) => ({ ...s, displayName: e.target.value }))}
                        />
                    </div>
                    <div className="form-row">
                        <label htmlFor="regPassword">Пароль</label>
                        <input id="regPassword" type="password" placeholder="••••••••"
                               value={reg.password}
                               onChange={(e) => setReg((s) => ({ ...s, password: e.target.value }))}
                               onKeyDown={(e) => { if (e.key === 'Enter') doRegister(); }}
                        />
                    </div>
                    <button className="full secondary" id="btnRegister" onClick={doRegister}>
                        Зарегистрироваться
                    </button>
                </div>
            ) : null}

            <Status id="statusAuth" message={status.msg} type={status.type} />
        </div>
    );
}
