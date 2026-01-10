import { useEffect, useRef, useState } from 'react';
import { Avatar, Box, Button, IconButton, Paper, Stack, Typography } from "@mui/material";
import FavoriteRoundedIcon from "@mui/icons-material/FavoriteRounded";
import FavoriteBorderRoundedIcon from "@mui/icons-material/FavoriteBorderRounded";
import CommentOutlined from "@mui/icons-material/CommentOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import OutlinedFlagRoundedIcon from "@mui/icons-material/OutlinedFlagRounded";
import SettingsOutlinedIcon from "@mui/icons-material/SettingsOutlined";
import SearchRoundedIcon from "@mui/icons-material/SearchRounded";

import { apiBase, authHeaders, deviceIdHeaders, isUnauthorized } from '../api/api';
import { formatCompactNumber } from '../utils/format';
import { useAuth } from '../context/AuthContext';
import { VideoInfoDialog } from "../components/VideoInfoDialog.jsx";
import { CommentsDialog } from "../components/CommentsDialog.jsx";
import { QualityDialog } from "../components/QualityDialog.jsx";
import { ComplaintDialog } from "../components/ComplaintDialog.jsx";

const PAGE_SIZE = 10;
const MAX_WINDOW = 2 * PAGE_SIZE;
const PREFETCH_DELTA = 2;

function getOrCreateDeviceId() {
    const key = 'feed-device-id';
    let id = localStorage.getItem(key);
    if (!id) {
        id = crypto.randomUUID();
        localStorage.setItem(key, id);
    }
    return id;
}

export default function FeedPage({ active, exposeControls, onPage }) {
    const { token, user, authReady } = useAuth();

    const [videoInfoDialogOpen, setVideoInfoDialogOpen] = useState(false);
    const [commentsDialogOpen, setCommentsDialogOpen] = useState(false);
    const [qualityDialogOpen, setQualityDialogOpen] = useState(false);
    const [complaintDialogOpen, setComplaintDialogOpen] = useState(false);

    const playerRef = useRef(null);
    const preload1Ref = useRef(null);
    const preload2Ref = useRef(null);
    const playerStateRef = useRef(null);

    const feedModeRef = useRef({ type: 'global' }); // { type: 'global' } | { type: 'profile', userId, sort }
    const targetUuidRef = useRef(null);
    const deviceIdRef = useRef(getOrCreateDeviceId());

    const [window, setWindow] = useState({ videos: [], currentIndex: -1 });
    const { videos, currentIndex } = window;
    const currentVideo = videos[currentIndex] ?? null;

    const {
        uuid,
        name,
        uploaderId,
        uploaderName,
        uploaderAvatarUrl,
        likesCount = 0,
        likedByMe = false,
        viewsCount = 0,
        commentsCount = 0,
        snapshot = {},
        localIndex = -1,
        selectedQuality = null,
    } = currentVideo ?? {};

    const [loading, setLoading] = useState(false);

    const setVideos = (callback) => setWindow((prev) => ({ ...prev, videos: callback(prev.videos) }));
    const setCurrentIndex = (currentIndex) => setWindow((prev) => ({ ...prev, currentIndex }));

    async function startFeed() {
        if (loading) return;
        setLoading(true);

        try {
            let url, res;

            if (feedModeRef.current.type === 'global') {
                url = apiBase + `/api/streaming/video/feed/session/start?size=${PAGE_SIZE}`;
                res = await fetch(url, {
                    method: 'POST',
                    headers: { ...authHeaders(token), ...deviceIdHeaders(deviceIdRef.current) }
                });
            } else {
                const { userId, sort } = feedModeRef.current;
                url = apiBase +
                    `/api/users/${userId}/videos?page=0&size=${PAGE_SIZE}&sort=${encodeURIComponent(sort)}`;
                res = await fetch(url, { headers: authHeaders(token) });
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);
            const data = await res.json();
            const items = data.items ?? data.content ?? [];

            const newSnapshot = {
                feedId: data.feedId ?? null,
                prevPos: data.prevPos ?? null,
                curPos: 0,
                nextPos: data.nextPos ?? null,
                number: data.number ?? 0,
                first: data.first ?? true,
                last: data.last ?? true,
            };
            const newVideos = items.map((v, index) => ({
                ...v,
                snapshot: newSnapshot,
                localIndex: index,
                lastIndex: items.length - 1,
            }));

            let newIndex = 0;
            const targetUuid = targetUuidRef.current;
            if (targetUuid) {
                const idx = newVideos.findIndex(v => v.uuid === targetUuid);
                if (idx !== -1) {
                    newIndex = idx;
                    targetUuidRef.current = null; // цель достигнута
                }
            }

            setWindow({ videos: newVideos, currentIndex: newIndex });
        } finally {
            setLoading(false);
        }
    }

    const reloadFeed = () => {
        feedModeRef.current = { type: 'global' };
        targetUuidRef.current = null;
        startFeed();
    }

    async function prefetch(direction, snapshot, restoreCurPage = false) {
        if (loading) return;
        setLoading(true);

        try {
            let pos, page, url, res;

            if (feedModeRef.current.type === 'global') {
                const { feedId, nextPos, curPos, prevPos } = snapshot;
                if (direction === 'next') {
                    pos = nextPos ?? 0; // если nextPos null, то берем 0
                } else {
                    pos = restoreCurPage ? curPos : prevPos;
                }

                if ((prevPos === null && nextPos === null) || pos === null) {
                    return; // если страница одна либо pos null - выходим
                }

                url = `${apiBase}/api/streaming/video/feed/session/${feedId}/${direction}?count=${PAGE_SIZE}&pos=${pos}`;
                res = await fetch(url, { headers: { ...authHeaders(token), ...deviceIdHeaders(deviceIdRef.current) } });
            } else {
                const { number, first, last } = snapshot;
                if (direction === 'next') {
                    page = last ? 0 : number + 1;
                } else {
                    page = restoreCurPage ? number : number - 1;
                }

                if ((first && last) || page < 0) {
                    return; // если страница одна либо номер меньше 0 - выходим
                }

                const { userId, sort } = feedModeRef.current;
                url = `${apiBase}/api/users/${userId}/videos?page=${page}&size=${PAGE_SIZE}&sort=${encodeURIComponent(sort)}`;
                res = await fetch(url, { headers: authHeaders(token) });
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);
            const data = await res.json();
            const items = data.items ?? data.content ?? [];

            const newSnapshot = {
                feedId: data.feedId ?? null,
                prevPos: data.prevPos ?? null,
                curPos: pos ?? 0,
                nextPos: data.nextPos ?? null,
                number: data.number ?? 0,
                first: data.first ?? true,
                last: data.last ?? true,
            };
            const newVideos = (data.items ?? data.content ?? []).map((v, index) => ({
                ...v,
                snapshot: newSnapshot,
                localIndex: index,
                lastIndex: items.length - 1,
            }));

            setWindow((prev) => {
                let nextVideos, nextIndex;

                if (direction === 'next') {
                    nextVideos = [...prev.videos, ...newVideos];
                    nextIndex = prev.currentIndex;
                } else if (!restoreCurPage) {
                    nextVideos = [...newVideos, ...prev.videos];
                    nextIndex = prev.currentIndex + newVideos.length;
                } else {
                    const current = prev.videos[prev.currentIndex];
                    if (!current) return prev;

                    nextVideos = [
                        ...newVideos.slice(0, current.localIndex), // все видео левее текущего (на его странице)
                        ...prev.videos.slice(prev.currentIndex), // все видео правее текущего (в окне)
                    ];
                    nextIndex = current.localIndex;
                }

                if (nextVideos.length > MAX_WINDOW) {
                    const overflow = nextVideos.length - MAX_WINDOW;
                    if (direction === 'next') {
                        nextVideos = nextVideos.slice(overflow);
                        nextIndex = Math.max(nextIndex - overflow, 0);
                    } else {
                        nextVideos = nextVideos.slice(0, MAX_WINDOW);
                    }
                }

                return { videos: nextVideos, currentIndex: nextIndex };
            });
        } finally {
            setLoading(false);
        }
    }

    const registerView = async () => {
        if (!currentVideo || !token) return;

        try {
            const res = await fetch(`${apiBase}/api/videos/${uuid}/view`, {
                method: 'POST',
                headers: authHeaders(token)
            });

            if (!res.ok) return;

            const data = await res.json();
            const viewsCount =
                typeof data?.viewsCount === 'number'
                    ? data.viewsCount
                    : typeof data?.count === 'number'
                        ? data.count
                        : null;

            if (viewsCount == null) return;

            setVideos((prev) => {
                const copy = [...prev];
                const idx = copy.findIndex(v => v.uuid === uuid);
                if (idx >= 0) {
                    copy[idx] = { ...copy[idx], viewsCount };
                }
                return copy;
            });
        } catch {
            // silently ignore — view is not critical
        }
    };

    const toggleLike = async () => {
        if (!currentVideo) return;
        if (!token) {
            onPage("auth");
            return;
        }

        try {
            const res = await fetch(`${apiBase}/api/videos/${uuid}/like`, {
                method: likedByMe ? 'DELETE' : 'POST',
                headers: authHeaders(token),
            });

            if (isUnauthorized(res)) {
                onPage("auth");
                return;
            }
            if (!res.ok) return;

            const data = await res.json();
            const { likesCount, liked } = data ?? {};

            setVideos((prev) => {
                const copy = [...prev];
                const idx = copy.findIndex(v => v.uuid === uuid);
                if (idx >= 0) {
                    copy[idx] = { ...copy[idx], likesCount, likedByMe: liked };
                }
                return copy;
            });
        } catch {
            // silently ignore — like is non-critical
        }
    };

    function setStatusFeed(msg = '', type = '') {
        console.log(msg, type)
    }

    function preloadVideo(videoIndex, el, mode = 'metadata') {
        if (!el || videoIndex < 0 || videoIndex >= videos.length) return;

        const video = videos[videoIndex];
        const url = video.selectedQuality
            ? `${apiBase}/api/streaming/video/${video.uuid}?quality=${video.selectedQuality}`
            : `${apiBase}/api/streaming/video/${video.uuid}`;

        // сли уже загружаем это видео — ничего не делаем
        if (el.src === url) return;

        el.preload = mode;
        el.src = url;
    }

    function preloadAround(index) {
        preloadVideo(index + 1, preload1Ref.current, 'auto');
        preloadVideo(index + 2, preload2Ref.current, 'metadata');
    }

    const goNext = () => {
        if (!videos.length) return;

        if (currentIndex < videos.length - 1) {
            const nextIdx = currentIndex + 1;
            setCurrentIndex(nextIdx);

            const nextVideo = videos[nextIdx];
            if (!nextVideo) return;

            const { snapshot, localIndex, lastIndex } = nextVideo;
            const distToWindowEnd = videos.length - 1 - nextIdx;
            const distToPageEnd = lastIndex - localIndex;

            // Подгружаем только если видео находится на последней странице в окне
            if (distToWindowEnd <= PREFETCH_DELTA && distToWindowEnd <= distToPageEnd) {
                prefetch('next', snapshot); // тут snapshot следующего видео!
            }
        } else {
            const type = feedModeRef.current.type;
            const { prevPos, nextPos, first, last } = snapshot; // тут snapshot текущего видео!

            // Конец окна и одна страница - перемещаемся в начало окна
            if ((type === 'global' && prevPos === nextPos) || (type === 'profile' && first === last)) {
                setCurrentIndex(0);
            }
        }
    }

    const goPrev = () => {
        if (!videos.length) return;

        const type = feedModeRef.current.type;
        const { prevPos, first } = snapshot;

        // Если нулевой элемент первой страницы - назад идти нельзя!
        if (localIndex === 0 && ((type === 'global' && prevPos === null) || (type === 'profile' && first))) {
            setStatusFeed('Это самое первое видео', 'ok');
            return;
        }

        if (currentIndex > 0) {
            const nextIdx = currentIndex - 1;
            setCurrentIndex(nextIdx);

            const nextVideo = videos[nextIdx];
            if (!nextVideo) return;

            const { snapshot, localIndex } = nextVideo;

            if (nextIdx <= PREFETCH_DELTA) {
                prefetch('prev', snapshot, nextIdx < localIndex);
            }
        }
    }

    useEffect(() => {
        exposeControls?.({
            reloadFeed,
            switchToProfile: (userId, sort, uuid) => {
                feedModeRef.current = { type: 'profile', userId, sort };
                targetUuidRef.current = uuid || null;
                startFeed();
            },
            getGlobalIndexByUuid: (uuid) => videos.findIndex((v) => v.uuid === uuid),
            playByIndex: (idx) => setCurrentIndex(idx),
            goNext,
            goPrev,
        });
    }, [exposeControls, window]);

    /* Старт ленты при первом входе на страницу либо если нет видео */
    useEffect(() => {
        if (!active || !authReady || videos.length > 0) return;
        startFeed();
    }, [active, authReady]);

    /* Перезагрузка ленты, если сменился токен */
    useEffect(() => {
        if (!authReady) return;
        reloadFeed();
    }, [token]);

    /* Изменение состояния плеера */
    useEffect(() => {
        const player = playerRef.current;
        if (!active || !uuid || !player) return;

        const url = selectedQuality
            ? `${apiBase}/api/streaming/video/${uuid}?quality=${selectedQuality}`
            : `${apiBase}/api/streaming/video/${uuid}`;

        if (player.src === url) return;

        const saved = playerStateRef.current;
        const isSameVideo = saved?.uuid === uuid;
        const shouldPlay = isSameVideo ? saved.shouldPlay : true;
        const currentTime = isSameVideo ? saved.currentTime : 0;

        playerStateRef.current = null;

        try {
            player.pause();
            player.src = url;
            player.currentTime = currentTime;

            if (shouldPlay) {
                player.play().catch(() => {
                    /* ignore */
                });
            }
        } catch {
            /* ignore */
        }

        if (!isSameVideo) {
            preloadAround(currentIndex);
            registerView(uuid);
        }
    }, [active, uuid, selectedQuality]);

    /* Назначение / удаление слушателей плеера + сохранение его состояния при уходе со страницы */
    useEffect(() => {
        const player = playerRef.current;
        if (!active || !player) return;

        const onEnded = () => {
            try {
                player.currentTime = 0;
                player.play().catch(() => {
                    /* ignore */
                });
            } catch {
                /* ignore */
            }
        };
        const onError = () => {
            setStatusFeed('Ошибка воспроизведения видео', 'error');
        };

        player.addEventListener('ended', onEnded);
        player.addEventListener('error', onError);

        return () => {
            playerStateRef.current = {
                uuid,
                shouldPlay: !player.paused,
                currentTime: player.currentTime || 0,
            };
            player.removeEventListener('ended', onEnded);
            player.removeEventListener('error', onError);
        };
    }, [active, uuid]);

    const handleQualitySelect = (quality) => {
        const player = playerRef.current;
        if (!currentVideo || !player) return;

        playerStateRef.current = { uuid, shouldPlay: !player.paused, currentTime: player.currentTime };

        setVideos((prev) => {
            const copy = [...prev];
            const idx = copy.findIndex(v => v.uuid === uuid);
            if (idx >= 0) {
                copy[idx] = { ...copy[idx], selectedQuality: quality };
            }
            return copy;
        });
    };

    if (!active) return null;

    return (
        <Stack spacing={1} flex={1}>
            <Paper elevation={0}>
                <Stack direction="row" p={1} alignItems="center" justifyContent="space-between">
                    <Typography variant="h6" px={1}>Рекомендации</Typography>
                    <IconButton onClick={() => onPage("search")} size="small">
                        <SearchRoundedIcon/>
                    </IconButton>
                </Stack>
            </Paper>
            <Box sx={{
                position: 'relative',
                height: 1,
                borderRadius: 4,
                overflow: 'hidden',
            }}>
                <Stack
                    spacing={1}
                    sx={{
                        width: 1,
                        position: 'absolute',
                        left: 0,
                        top: 0,
                        zIndex: 1,
                        filter: 'drop-shadow(1px 1px 1px rgb(0,0,0))',
                        p: 1,
                        pt: 1 / 2,
                        alignItems: 'flex-start',
                    }}
                >
                    <Typography
                        noWrap
                        textOverflow="ellipsis"
                        width={1}
                        sx={{ userSelect: "none", cursor: "pointer" }}
                        onClick={() => setVideoInfoDialogOpen(true)}
                    >
                        {name}
                    </Typography>
                    <Stack
                        direction="row"
                        spacing={1}
                        alignItems="center"
                        sx={{ cursor: 'pointer' }}
                        onClick={() => uploaderId && onPage("profile", { profileUserId: uploaderId })}
                    >
                        <Avatar
                            src={uploaderAvatarUrl ? `${apiBase}${uploaderAvatarUrl}` : null}
                            sx={{ width: 32, height: 32 }}
                        >
                            {uploaderName?.[0]?.toUpperCase()}
                        </Avatar>
                        <Typography sx={{ userSelect: "none" }} fontWeight="medium">@{uploaderName}</Typography>
                    </Stack>
                </Stack>
                <Stack
                    spacing={2}
                    sx={(theme) => ({
                        position: 'absolute',
                        right: theme.spacing(1),
                        top: '50%',
                        transform: 'translateY(-50%)',
                        zIndex: 1,
                        alignItems: 'center',
                        filter: 'drop-shadow(2px 2px 2px rgb(0,0,0))',
                    })}
                >
                    <Stack alignItems="center" spacing={0.5}>
                        <VisibilityOutlinedIcon sx={{ fontSize: 32 }}/>
                        <Typography variant="caption">
                            {formatCompactNumber(viewsCount)}
                        </Typography>
                    </Stack>
                    <Stack alignItems="center">
                        <Button
                            size="uniSmall"
                            color={likedByMe ? "error" : "inherit"}
                            onClick={toggleLike}
                        >
                            {likedByMe ? (
                                <FavoriteRoundedIcon sx={{ fontSize: 32 }}/>
                            ) : (
                                <FavoriteBorderRoundedIcon sx={{ fontSize: 32 }}/>
                            )}
                        </Button>
                        <Typography variant="caption">
                            {formatCompactNumber(likesCount)}
                        </Typography>
                    </Stack>
                    <Stack alignItems="center">
                        <Button
                            size="uniSmall"
                            onClick={() => setCommentsDialogOpen(true)}
                        >
                            <CommentOutlined sx={{ fontSize: 32 }}/>
                        </Button>
                        <Typography variant="caption">
                            {formatCompactNumber(commentsCount)}
                        </Typography>
                    </Stack>
                    <Button
                        size="uniSmall"
                        onClick={() => setQualityDialogOpen(true)}
                    >
                        <SettingsOutlinedIcon sx={{ fontSize: 32 }}/>
                    </Button>
                    {user && uploaderId !== user.userId && (
                        <Button
                            size="uniSmall"
                            onClick={() => setComplaintDialogOpen(true)}
                        >
                            <OutlinedFlagRoundedIcon sx={{ fontSize: 32 }}/>
                        </Button>
                    )}
                </Stack>
                <video ref={playerRef} id="player" controls playsInline muted/>
                <video ref={preload1Ref} id="preloadNext1" style={{ display: 'none' }} muted/>
                <video ref={preload2Ref} id="preloadNext2" style={{ display: 'none' }} muted/>
            </Box>
            <VideoInfoDialog
                open={videoInfoDialogOpen}
                setOpen={setVideoInfoDialogOpen}
                video={currentVideo}
            />
            <CommentsDialog
                open={commentsDialogOpen}
                setOpen={setCommentsDialogOpen}
                videoUuid={uuid}
                onPage={onPage}
            />
            <QualityDialog
                open={qualityDialogOpen}
                setOpen={setQualityDialogOpen}
                video={currentVideo}
                onSelect={handleQualitySelect}
            />
            <ComplaintDialog
                open={complaintDialogOpen}
                setOpen={setComplaintDialogOpen}
                mode="video"
                videoUuid={uuid}
            />
        </Stack>
    );
}