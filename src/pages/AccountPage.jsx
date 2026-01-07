import React, { useEffect, useRef, useState } from 'react';
import { apiBase, authHeaders, isUnauthorized } from '../api/api';
import { useAuth } from '../context/AuthContext';
import Avatar from '../components/Avatar';
import Status from '../components/Status';
import ThumbGrid, { ThumbCard } from '../components/ThumbGrid';

export default function AccountPage({
                                        active,
                                        onTab,
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

    const [profile, setProfile] = useState(null);
    const [myVideos, setMyVideos] = useState([]);
    const [myVideosPaging, setMyVideosPaging] = useState({ page: 0, size: 12, last: false, loading: false });

    const [following, setFollowing] = useState([]);
    const [followingStatus, setFollowingStatus] = useState('');

    /* =========================
       NEW: sort
       ========================= */
    const [sort, setSort] = useState('uploadedAt,desc');

    const avatarFileRef = useRef(null);

    function setStatusAuth(msg = '', type = '') {
        setStatus({ msg, type });
    }

    function logout() {
        setAuth(null, null);
        setStatusAuth('Вы вышли из аккаунта', 'ok');
        setProfile(null);
        setMyVideos([]);
        setFollowing([]);
        onTab("auth")
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

            // Обновляем user и profile с новым avatarUrl, добавляем параметр для обновления кеша
            const updatedUser = { ...user, avatarUrl: `${data.url}?t=${new Date().getTime()}` }; // Добавляем уникальную метку времени
            setAuth(token, updatedUser);

            // Обновляем локальное состояние профиля
            setProfile((prevProfile) => ({
                ...prevProfile,
                avatarUrl: `${data.url}?t=${new Date().getTime()}`, // Добавляем уникальную метку времени
            }));

            setStatusAuth('Аватар обновлён', 'ok');
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

    /* =========================
       LOAD PROFILE + VIDEOS
       ========================= */
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
                fetch(apiBase + '/api/users/' + u.userId + '/profile', {
                    headers: authHeaders(tokenOverride),
                }),
                fetch(apiBase + `/api/users/${u.userId}/videos?page=0&size=12&sort=${encodeURIComponent(sort)}`, {
                    headers: authHeaders(tokenOverride)
                }),
            ]);

            if (!userRes.ok || !vidsRes.ok) throw new Error('HTTP ' + userRes.status + '/' + vidsRes.status);

            const prof = await userRes.json();
            const vidsPage = await vidsRes.json();

            const vids = vidsPage?.content || [];
            const totalVideos =
                typeof vidsPage?.totalElements === 'number'
                    ? vidsPage.totalElements
                    : vids.length;

            setProfile({ ...prof, totalVideos });
            setMyVideos(vids);

            setMyVideosPaging({
                page: (vidsPage?.number ?? 0) + 1,
                size: 12,
                last: !!vidsPage?.last,
                loading: false,
            });

            loadAccountFollowing(u.userId, tokenOverride);
        } catch {
            setProfile(null);
            setMyVideos([]);
        }
    }

    async function loadMoreMyVideos() {
        if (!user || !token) return;
        if (myVideosPaging.loading || myVideosPaging.last) return;

        setMyVideosPaging((s) => ({ ...s, loading: true }));

        try {
            const res = await fetch(
                apiBase +
                `/api/users/${user.userId}/videos?page=${myVideosPaging.page}&size=${myVideosPaging.size}&sort=${encodeURIComponent(
                    sort
                )}`,
                { headers: authHeaders(token) }
            );

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
                page: (page?.number ?? s.page) + 1,
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
            const res = await fetch(apiBase + '/api/streaming/video/delete/' + uuid, {
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
    }, [active, myVideosPaging, sort]);

    useEffect(() => {
        if (!active) return;
        if (user && token) {
            loadAccountProfile();
            onLoadActiveJobsFromServer?.(token);
        } else {
            setProfile(null);
            setMyVideos([]);
        }
    }, [active, user?.userId, token, sort]);

    if (!active) return null;

    const userShort = user ? (user.displayName || user.username || 'Пользователь') : 'Гость';
    const followers = profile?.followersCount ?? 0;
    const followingCount = profile?.followingCount ?? 0;
    const totalVideos = profile?.totalVideos ?? (myVideos?.length || 0);

    return (
        <div className={'card page ' + (active ? 'active' : '')} id="pageAccount">
            {user ? (
                <>
                    <div id="authAvatar" className="profile-avatar" style={{ display: '' }}>
                        {/* Используем apiBase для аватара */}
                        {user.avatarUrl ? <img src={`${apiBase}${user.avatarUrl}`} alt=""/> : null}
                    </div>

                    <div className="form-row" id="avatarUploadRow" style={{ display: '' }}>
                        <label htmlFor="avatarFile">Аватар</label>
                        <input id="avatarFile" ref={avatarFileRef} type="file" accept="image/*"/>
                        <button className="full small" id="btnAvatarUpload" onClick={uploadAvatar}>
                            Обновить аватар
                        </button>
                    </div>
                </>
            ) : (
                <div id="authAvatar" className="profile-avatar" style={{ display: 'none' }}/>
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

                    {user?.roles?.some((r) => r === 'ADMIN' || r === 'MODERATOR') && (
                        <button className="full small" id="btnLogout" onClick={() => onTab("moderation")}>
                            Модерация
                        </button>
                    )}
                </div>
            ) : null}

            {user ? (
                <div id="authProfileBlock" style={{ display: '', marginTop: 12 }}>
                    <div className="studio-section-title">Ваш профиль</div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
                        <div className="profile-avatar" id="authProfileAvatar">
                            {/* Используем apiBase для аватара */}
                            {profile?.avatarUrl ?
                                <img src={`${apiBase}${profile.avatarUrl}`} alt=""/> : (user.avatarUrl ?
                                    <img src={`${apiBase}${user.avatarUrl}`} alt=""/> : null)}
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
                                    <div key={u.id} className="studio-row" style={{ cursor: 'pointer' }}
                                         onClick={() => onOpenProfile?.(u.id)}>
                                        <div className="studio-row-header">
                                            <div className="studio-row-title">{u.displayName || u.username}</div>
                                            <Avatar className="avatar-mini" url={u.avatarUrl}/>
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
                                            <div className="job-progress-fill"
                                                 style={{ width: (st.percent ?? 0) + '%' }}/>
                                        </div>
                                    </div>
                                );
                            })
                        ) : (
                            <div className="studio-row-meta">Нет активных загрузок</div>
                        )}
                    </div>

                    <div className="status" id="statusStudio">{jobsState?.statusStudio || ''}</div>

                    <div className="studio-section-title" style={{
                        marginTop: 8,
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center'
                    }}>
                        <span>Ваши видео</span>
                        <span className="badge" id="studioVideoCount">{totalVideos} видео</span>
                    </div>

                    <div style={{ marginTop: 8,  marginBottom: 8 }}>
                        <select
                            className="feed-select"
                            value={sort}
                            onChange={(e) => setSort(e.target.value)}
                        >
                            <option value="uploadedAt,desc">Сначала новые</option>
                            <option value="uploadedAt,asc">Сначала старые</option>
                            <option value="viewsCount,desc">По просмотрам</option>
                            <option value="likesCount,desc">По лайкам</option>
                        </select>
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
                                    showDelete
                                    onDelete={() => deleteVideo(v.uuid)}
                                    onOpen={() =>
                                        onGlobalPlayByUuid?.({
                                            userId: user.userId,
                                            sort,
                                            uuid: v.uuid,
                                        })
                                    }
                                />
                            )}
                        />
                    )}
                </div>
            ) : null}

            <Status message={status.msg} type={status.type}/>
        </div>
    );
}