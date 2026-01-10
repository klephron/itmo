import { useEffect, useState } from 'react';
import { apiBase, authHeaders, isUnauthorized } from '../api/api';
import { useAuth } from '../context/AuthContext';
import { ThumbGrid } from "../components/ThumbGrid.jsx";
import {
    Avatar,
    Box,
    Button,
    Card,
    CardActionArea,
    Grid,
    LinearProgress,
    Paper,
    Stack,
    Typography
} from "@mui/material";
import { formatFollowers, formatFollowing } from "../utils/format.js";
import SettingsRoundedIcon from "@mui/icons-material/SettingsRounded";
import LogoutRoundedIcon from "@mui/icons-material/LogoutRounded";
import { AvatarUploadDialog } from "../components/AvatarUploadDialog.jsx";

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

    const [activeTab, setActiveTab] = useState("videos");
    const [avatarDialogOpen, setAvatarDialogOpen] = useState(false);

    const [status, setStatus] = useState({ msg: '', type: '' });

    const [profile, setProfile] = useState(null);
    const [following, setFollowing] = useState([]);
    const [followingStatus, setFollowingStatus] = useState('');

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

    console.log(jobsState);

    const testJobsState = {
        "activeJobs": [
            {
                "jobId": "cf652161-84e0-47ca-a94a-f7defed7169e",
                "fileName": "1 minute of rally cars jumping at Rally Italy.mp4",
                "lastStatus": {
                    "stage": "UPLOADING_TO_MINIO",
                    "percent": 90,
                    "message": "Загрузка конвертированных файлов…",
                    "fileName": "1 minute of rally cars jumping at Rally Italy.mp4",
                    "userId": 1
                }
            }
        ],
        "statusStudio": ""
    }

    return (
        <>
            <Stack component={Paper} direction="row" p={1}>
                <Typography variant="h6" px={1}>Ваш профиль</Typography>
            </Stack>
            <Stack component={Paper} pt={2} flex={1} overflow="hidden">
                <Stack direction="row" px={2} pr={8.5} spacing={1} alignItems="flex-start" position="relative">
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
                    <Stack spacing={1} direction="row" sx={{ position: "absolute", top: -4, right: 12 }}>
                        <Button size="uniSmall" onClick={() => setAvatarDialogOpen(true)}>
                            <SettingsRoundedIcon fontSize="small" sx={{ color: "text.secondary" }}/>
                        </Button>
                        <Button size="uniSmall" onClick={logout}>
                            <LogoutRoundedIcon fontSize="small" sx={{ color: "text.secondary" }}/>
                        </Button>
                    </Stack>
                </Stack>

                <Typography variant="caption" color="text.secondary" mt={1} px={2}>
                    {followers.toLocaleString()} {formatFollowers(followers)} ·{" "}
                    {followingCount.toLocaleString()} {formatFollowing(followingCount)}
                </Typography>

                <Grid container spacing={1} mt={1.5} mb={2} px={2}>
                    <Button
                        variant="contained"
                        color={activeTab === "videos" ? "primary" : "inherit"}
                        size="small"
                        onClick={() => setActiveTab("videos")}
                    >
                        Видео
                    </Button>
                    <Button
                        variant="contained"
                        color={activeTab === "following" ? "primary" : "inherit"}
                        size="small"
                        onClick={() => setActiveTab("following")}
                    >
                        Подписки
                    </Button>
                    <Button
                        variant="contained"
                        color={activeTab === "jobs" ? "primary" : "inherit"}
                        size="small"
                        onClick={() => setActiveTab("jobs")}
                    >
                        Загрузки
                    </Button>
                    <Button
                        variant="contained"
                        color={activeTab === "moderation" ? "primary" : "inherit"}
                        size="small"
                        onClick={() => onTab("moderation")}
                    >
                        Модерация
                    </Button>
                </Grid>
                {activeTab === "videos" ? (
                    <ThumbGrid
                        active={activeTab === "videos"}
                        userId={user?.userId}
                        onOpenVideo={onGlobalPlayByUuid}
                        onDeleteVideo={deleteVideo}
                        showDelete
                    />
                ) : activeTab === "following" ? (
                    <>
                        <Stack direction="row" spacing={1} py={0.5} px={2}>
                            <Typography variant="h6" fontWeight="bold">Ваши подписки</Typography>
                            <Typography variant="body2" color="text.secondary" pt={1}>{following?.length}</Typography>
                        </Stack>

                        {!following?.length ? (
                            <Typography mt={1.5} px={2}>Подписок пока нет</Typography>
                        ) : (
                            <Box sx={{ borderRadius: 3, flex: 1, overflow: "hidden", position: "relative", m: 1 }}>
                                <Stack sx={{ height: 1, overflowX: "hidden", overflowY: "auto", }}>
                                    {following.map((u) => (
                                        <Card key={u.id} sx={{ flexShrink: 0 }}>
                                            <CardActionArea onClick={() => onOpenProfile?.(u.id)}>
                                                <Stack direction="row" spacing={1} p={1} alignItems="center">
                                                    <Avatar
                                                        src={u.avatarUrl ? `${apiBase}${u.avatarUrl}` : undefined}
                                                        sx={{ width: 32, height: 32 }}
                                                    />
                                                    <Stack spacing={0.5}>
                                                        <Typography variant="caption" fontWeight="medium"
                                                                    lineHeight={1}>
                                                            {u.displayName || u.username}
                                                        </Typography>
                                                        <Typography variant="caption" color="textSecondary"
                                                                    lineHeight={1}>
                                                            @{u.username}
                                                        </Typography>
                                                    </Stack>
                                                </Stack>
                                            </CardActionArea>
                                        </Card>
                                    ))}
                                </Stack>
                            </Box>
                        )}
                    </>
                ) : (
                    <>
                        <Stack direction="row" spacing={1} py={0.5} px={2}>
                            <Typography variant="h6" fontWeight="bold">
                                Активные загрузки
                            </Typography>
                            <Typography variant="body2" color="text.secondary" pt={1}>
                                {jobsState?.activeJobs?.length ?? 0}
                            </Typography>
                        </Stack>

                        {!jobsState?.activeJobs?.length ? (
                            <Typography mt={1.5} px={2}>
                                Загрузок пока нет
                            </Typography>
                        ) : (
                            <Box
                                sx={{
                                    borderRadius: 3,
                                    flex: 1,
                                    overflow: "hidden",
                                    position: "relative",
                                    m: 1,
                                }}
                            >
                                <Stack
                                    sx={{
                                        height: 1,
                                        overflowX: "hidden",
                                        overflowY: "auto",
                                    }}
                                    spacing={1}
                                >
                                    {jobsState.activeJobs.map((job) => {
                                        const st = job.lastStatus || {
                                            stage: "UPLOADING",
                                            percent: 0,
                                            message: "Загрузка…",
                                        };

                                        const percent = st.percent ?? 0;

                                        return (
                                            <Card key={job.jobId} sx={{ flexShrink: 0 }}>
                                                <Stack spacing={0.75} p={1}>
                                                    <Stack
                                                        direction="row"
                                                        justifyContent="space-between"
                                                        alignItems="center"
                                                    >
                                                        <Typography
                                                            variant="body2"
                                                            fontWeight="medium"
                                                            noWrap
                                                        >
                                                            {job.fileName || "(без названия)"}
                                                        </Typography>
                                                        <Typography
                                                            variant="caption"
                                                            color="text.secondary"
                                                        >
                                                            {percent}%
                                                        </Typography>
                                                    </Stack>
                                                    <Typography
                                                        variant="caption"
                                                        color="text.secondary"
                                                        noWrap
                                                    >
                                                        {st.message || st.stage}
                                                    </Typography>
                                                    <LinearProgress
                                                        variant="determinate"
                                                        value={percent}
                                                        color="success"
                                                    />
                                                </Stack>
                                            </Card>
                                        );
                                    })}
                                </Stack>
                            </Box>
                        )}
                    </>
                )}
                <AvatarUploadDialog
                    open={avatarDialogOpen}
                    setOpen={setAvatarDialogOpen}
                    onSuccess={(avatarUrl) => {
                        setProfile((p) => p ? { ...p, avatarUrl } : p);
                    }}
                />
            </Stack>
        </>
    );
}