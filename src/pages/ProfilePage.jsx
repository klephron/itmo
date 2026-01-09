import { useEffect, useState } from 'react';
import { apiBase, authHeaders, isUnauthorized } from '../api/api';
import { useAuth } from '../context/AuthContext';
import { Avatar, Box, Button, Grid, MenuItem, Paper, Stack, TextField, Typography } from "@mui/material";
import { formatFollowers, formatFollowing } from "../utils/format.js";
import { ThumbCard } from "../components/ThumbCard.jsx";
import { LoadMoreButton } from "../components/LoadMoreButton.jsx";

const PAGE_SIZE = 12;

export default function ProfilePage({ active, userId, onRequireAuth, onGlobalPlayByUuid }) {
    const { token, user } = useAuth();

    const [status, setStatus] = useState({ msg: '', type: '' });
    const [profile, setProfile] = useState(null);
    const [videos, setVideos] = useState([]);
    const [paging, setPaging] = useState({ page: 0, size: PAGE_SIZE, last: true, loading: false });
    const [sort, setSort] = useState('uploadedAt,desc');

    function setStatusProfile(msg = '', type = '') {
        setStatus({ msg, type });
    }

    const isSelf = user && Number(user.userId) === Number(userId);

    async function loadPage(pageNumber, { replace = false } = {}) {
        if (!userId) return;

        setPaging((prevState) => ({ ...prevState, loading: true }));

        try {
            const res = await fetch(
                `${apiBase}/api/users/${userId}/videos?page=${pageNumber}&size=${PAGE_SIZE}&sort=${encodeURIComponent(sort)}`,
                { headers: authHeaders(token) }
            );

            if (!res.ok) throw new Error('HTTP ' + res.status);

            const page = await res.json();
            const vids = page?.content ?? [];

            setVideos((prev) =>
                replace ? vids : prev.concat(vids)
            );

            setPaging({
                page: page.number,
                size: PAGE_SIZE,
                last: page.last,
                loading: false,
            });
        } catch (e) {
            setStatusProfile('Ошибка загрузки видео: ' + e.message, 'error');
            setPaging((prevState) => ({ ...prevState, loading: false }));
        }
    }

    async function loadProfile() {
        try {
            const res = await fetch(
                `${apiBase}/api/users/${userId}/profile`,
                { headers: authHeaders(token) }
            );

            if (!res.ok) throw new Error('HTTP ' + res.status);
            const prof = await res.json();
            setProfile(prof);
        } catch (e) {
            setStatusProfile('Ошибка загрузки профиля: ' + e.message, 'error');
        }
    }

    async function toggleFollow() {
        if (!token) {
            setStatusProfile('Войдите, чтобы подписываться', 'error');
            onRequireAuth?.();
            return;
        }
        if (!profile) return;

        const method = profile.following ? 'DELETE' : 'POST';

        try {
            const res = await fetch(`${apiBase}/api/users/${userId}/follow`, {
                method,
                headers: authHeaders(token),
            });

            if (isUnauthorized(res)) {
                setStatusProfile('Сессия истекла, войдите снова', 'error');
                onRequireAuth?.();
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            setProfile((prevProfile) => ({
                ...prevProfile,
                following: !prevProfile.following,  // Меняем статус подписки
            }));

            setStatusProfile(profile.following ? 'Отписались от пользователя' : 'Подписались на пользователя', 'ok');
        } catch (e) {
            setStatusProfile('Ошибка изменения подписки: ' + e.message, 'error');
        }
    }

    useEffect(() => {
        if (!active || !userId) return;
        loadPage(0, { replace: true });
    }, [active, userId, sort]);

    useEffect(() => {
        if (!active || !userId) return;
        loadProfile();
    }, [active, userId, token]);

    const handleLoadMore = () => loadPage(paging.page + 1, { replace: false });

    if (!active) return null;

    const followers = profile?.followersCount ?? 0;
    const followingCount = profile?.followingCount ?? 0;

    return (
        <>
            <Stack component={Paper} direction="row" p={1}>
                <Typography variant="h6" px={1}>Профиль пользователя</Typography>
            </Stack>
            <Stack component={Paper} p={2} spacing={1} flex={1} overflow="hidden">
                <Stack direction="row" spacing={1} alignItems="flex-start">
                    <Avatar
                        src={profile?.avatarUrl ? `${apiBase}${profile.avatarUrl}` : undefined}
                        sx={{ width: 42, height: 42 }}
                    >
                        {profile?.username?.[0]?.toUpperCase()}
                    </Avatar>
                    <Box flex={1}>
                        <Typography variant="body1" fontWeight="bold">
                            {profile?.displayName}
                        </Typography>
                        <Typography component="p" variant="caption" fontWeight="medium">
                            @{profile?.username}
                        </Typography>
                    </Box>
                    {!isSelf && !!userId && (
                        <Button
                            size="large"
                            variant="contained"
                            color={profile?.following ? "inherit" : "primary"}
                            onClick={toggleFollow}
                        >
                            {profile?.following ? "Отписаться" : "Подписаться"}
                        </Button>
                    )}
                </Stack>
                <Typography variant="caption" color="text.secondary">
                    {followers.toLocaleString()} {formatFollowers(followers)} · {followingCount.toLocaleString()} {formatFollowing(followingCount)}
                </Typography>
                <Stack direction="row" justifyContent="space-between" alignItems="center" pb={1}>
                    <Stack direction="row" spacing={1}>
                        <Typography variant="h6" fontWeight="bold">Видео</Typography>
                        <Typography variant="body2" color="text.secondary" pt={1}>
                            {profile?.totalVideos ?? 0}
                        </Typography>
                    </Stack>
                    <TextField
                        select
                        size="small"
                        value={sort}
                        onChange={(e) => setSort(e.target.value)}
                    >
                        <MenuItem value="uploadedAt,desc">Сначала новые</MenuItem>
                        <MenuItem value="uploadedAt,asc">Сначала старые</MenuItem>
                        <MenuItem value="viewsCount,desc">По просмотрам</MenuItem>
                        <MenuItem value="likesCount,desc">По лайкам</MenuItem>
                    </TextField>
                </Stack>
                {!videos || videos.length === 0 ? (
                    <Typography>Видео пока нет</Typography>
                ) : (
                    <Box sx={{ borderRadius: 3, flex: 1, overflow: "hidden", position: "relative" }}>
                        <Box sx={{ height: 1, overflowX: "hidden", overflowY: "auto" }}>
                            <Grid container spacing={"1px"}>
                                {videos.map((v) => (
                                    <Grid key={v.uuid} size={4}>
                                        <ThumbCard
                                            video={v}
                                            showDelete={false}
                                            onOpen={() =>
                                                onGlobalPlayByUuid?.({
                                                    userId,
                                                    sort,
                                                    uuid: v.uuid,
                                                })
                                            }
                                        />
                                    </Grid>
                                ))}
                            </Grid>
                        </Box>
                        {!paging.last && (
                            <LoadMoreButton
                                sx={(theme) => ({
                                    position: 'absolute',
                                    bottom: theme.spacing(1),
                                    left: "50%",
                                    transform: `translateX(-50%)`,
                                })}
                                onClick={handleLoadMore}
                                loading={paging.loading}
                            />
                        )}
                    </Box>
                )}
            </Stack>
        </>
    );
}
