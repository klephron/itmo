import { useEffect, useRef, useState } from 'react';
import { apiBase, authHeaders, isUnauthorized } from '../api/api';
import { useAuth } from '../context/AuthContext';
import Status from '../components/Status';
import { ThumbGrid } from "../components/ThumbGrid.jsx";
import { Avatar, Box, Button, Paper, Stack, Typography } from "@mui/material";
import { formatFollowers, formatFollowing } from "../utils/format.js";

export default function AccountPage({
                                        active,
                                        onTab,
                                        onOpenProfile,
                                        onRequireAuth,
                                        onGlobalPlayByUuid,
                                        onReloadFeed,
                                        jobsState,
                                        onLoadActiveJobsFromServer,
                                    }) {
    const { token, user, setAuth } = useAuth();

    const [status, setStatus] = useState({ msg: '', type: '' });

    const [profile, setProfile] = useState(null);
    const [following, setFollowing] = useState([]);
    const [followingStatus, setFollowingStatus] = useState('');

    const avatarFileRef = useRef(null);

    function setStatusAuth(msg = '', type = '') {
        setStatus({ msg, type });
    }

    function logout() {
        setAuth(null, null);
        setStatusAuth('Вы вышли из аккаунта', 'ok');
        setProfile(null);
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

    async function loadAccountProfile(u = user, tokenOverride = token) {
        if (!u || !tokenOverride) {
            setProfile(null);
            return;
        }

        try {
            const res = await fetch(
                apiBase + '/api/users/' + u.userId + '/profile',
                { headers: authHeaders(tokenOverride) }
            );

            if (!res.ok) throw new Error('HTTP ' + res.status);

            const prof = await res.json();
            setProfile(prof);
        } catch {
            setProfile(null);
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

    useEffect(() => {
        if (!active) return;

        if (user && token) {
            loadAccountProfile();
            onLoadActiveJobsFromServer?.(token);
        } else {
            setProfile(null);
        }
    }, [active, user?.userId, token]);

    useEffect(() => {
        if (!active || !user?.userId || !token) {
            setFollowing([]);
            setFollowingStatus('');
            return;
        }

        loadAccountFollowing(user.userId, token);
    }, [active, user?.userId, token]);

    if (!active) return null;

    const userShort = user ? (user.displayName || user.username || 'Пользователь') : 'Гость';
    const followers = profile?.followersCount ?? 0;
    const followingCount = profile?.followingCount ?? 0;

    return (
        <>
            <Stack component={Paper} direction="row" p={1}>
                <Typography variant="h6" px={1}>Ваш профиль</Typography>
            </Stack>
            <Stack component={Paper} p={2} spacing={1} flex={1} overflow="hidden">
                <Stack direction="row" spacing={1} alignItems="flex-start">
                    <Avatar
                        src={
                            profile?.avatarUrl
                                ? `${apiBase}${profile.avatarUrl}`
                                : user?.avatarUrl
                                    ? `${apiBase}${user.avatarUrl}`
                                    : undefined
                        }
                        sx={{ width: 42, height: 42 }}
                    >
                        {user?.username?.[0]?.toUpperCase()}
                    </Avatar>

                    <Box flex={1}>
                        <Typography variant="body1" fontWeight="bold">
                            {profile?.displayName || user?.displayName || user?.username}
                        </Typography>
                        <Typography component="p" variant="caption" fontWeight="medium">
                            @{profile?.username || user?.username}
                        </Typography>
                    </Box>
                </Stack>

                <Typography variant="caption" color="text.secondary">
                    {followers.toLocaleString()} {formatFollowers(followers)} ·{" "}
                    {followingCount.toLocaleString()} {formatFollowing(followingCount)}
                </Typography>

                {/* ===== Auth actions ===== */}
                {user && (
                    <>
                        <Stack spacing={1} pt={1}>
                            <Button
                                variant="contained"
                                color="error"
                                size="small"
                                onClick={logout}
                            >
                                Выйти
                            </Button>

                            {user.roles?.some((r) => r === "ADMIN" || r === "MODERATOR") && (
                                <Button
                                    variant="outlined"
                                    size="small"
                                    onClick={() => onTab("moderation")}
                                >
                                    Модерация
                                </Button>
                            )}
                        </Stack>
                    </>
                )}

                {/* ===== Avatar upload ===== */}
                {user && (
                    <Stack spacing={1} pt={1}>
                        <Typography variant="subtitle2">Аватар</Typography>
                        <input
                            ref={avatarFileRef}
                            type="file"
                            accept="image/*"
                        />
                        <Button
                            variant="outlined"
                            size="small"
                            onClick={uploadAvatar}
                        >
                            Обновить аватар
                        </Button>
                    </Stack>
                )}

                {/* ===== Following ===== */}
                <Typography variant="h6" pt={2}>
                    Мои подписки
                </Typography>

                {followingStatus ? (
                    <Typography variant="caption">{followingStatus}</Typography>
                ) : following.length === 0 ? (
                    <Typography variant="caption">Подписок пока нет</Typography>
                ) : (
                    <Stack spacing={1}>
                        {following.map((u) => (
                            <Paper
                                key={u.id}
                                variant="outlined"
                                sx={{ p: 1, cursor: "pointer" }}
                                onClick={() => onOpenProfile?.(u.id)}
                            >
                                <Stack direction="row" spacing={1} alignItems="center">
                                    <Avatar
                                        src={u.avatarUrl ? `${apiBase}${u.avatarUrl}` : undefined}
                                        sx={{ width: 32, height: 32 }}
                                    />
                                    <Box>
                                        <Typography variant="body2">
                                            {u.displayName || u.username}
                                        </Typography>
                                        <Typography variant="caption">@{u.username}</Typography>
                                    </Box>
                                </Stack>
                            </Paper>
                        ))}
                    </Stack>
                )}

                {/* ===== Active uploads ===== */}
                <Typography variant="h6" pt={2}>
                    Активные загрузки
                </Typography>

                {jobsState?.activeJobs?.length ? (
                    <Stack spacing={1}>
                        {jobsState.activeJobs.map((job) => {
                            const st = job.lastStatus || {
                                stage: "UPLOADING",
                                percent: 0,
                                message: "Загрузка…",
                            };

                            return (
                                <Paper key={job.jobId} variant="outlined" sx={{ p: 1 }}>
                                    <Stack spacing={0.5}>
                                        <Stack direction="row" justifyContent="space-between">
                                            <Typography variant="body2">
                                                {job.fileName || "(без названия)"}
                                            </Typography>
                                            <Typography variant="body2">
                                                {st.percent ?? 0}%
                                            </Typography>
                                        </Stack>
                                        <Typography variant="caption">
                                            {st.message || st.stage}
                                        </Typography>
                                        <Box
                                            sx={{
                                                height: 6,
                                                borderRadius: 3,
                                                backgroundColor: "grey.300",
                                                overflow: "hidden",
                                            }}
                                        >
                                            <Box
                                                sx={{
                                                    height: "100%",
                                                    width: `${st.percent ?? 0}%`,
                                                    backgroundColor: "success.main",
                                                }}
                                            />
                                        </Box>
                                    </Stack>
                                </Paper>
                            );
                        })}
                    </Stack>
                ) : (
                    <Typography variant="caption">
                        Нет активных загрузок
                    </Typography>
                )}
                <ThumbGrid
                    active={active}
                    userId={user?.userId}
                    onOpenVideo={onGlobalPlayByUuid}
                    onDeleteVideo={deleteVideo}
                    showDelete
                />
            </Stack>
        </>
    );

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

                    <div style={{ marginTop: 8, marginBottom: 8 }}>
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
                            active={active}
                            userId={user?.userId}
                            onOpenVideo={onGlobalPlayByUuid}
                            onDeleteVideo={deleteVideo}
                            showDelete={true}
                        />
                    )}
                </div>
            ) : null}

            <Status message={status.msg} type={status.type}/>
        </div>
    );
}