import { useEffect, useRef, useState } from 'react';
import { Avatar, Box, Button, Paper, Stack, Typography } from "@mui/material";
import FavoriteRoundedIcon from "@mui/icons-material/FavoriteRounded";
import FavoriteBorderRoundedIcon from "@mui/icons-material/FavoriteBorderRounded";
import CommentOutlined from "@mui/icons-material/CommentOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import OutlinedFlagRoundedIcon from "@mui/icons-material/OutlinedFlagRounded";
import SettingsOutlinedIcon from "@mui/icons-material/SettingsOutlined";

import { apiBase, authHeaders, deviceIdHeaders, isUnauthorized } from '../api/api';
import { formatCompactNumber } from '../utils/format';
import { useAuth } from '../context/AuthContext';
import { VideoInfoDialog } from "../components/VideoInfoDialog.jsx";
import { CommentsDialog } from "../components/CommentsDialog.jsx";
import { QualityDialog } from "../components/QualityDialog.jsx";
import { ComplaintDialog } from "../components/ComplaintDialog.jsx";

const PAGE_SIZE = 3;
const MAX_WINDOW = 2 * PAGE_SIZE;
const PREFETCH_DELTA = 1;

function getOrCreateDeviceId() {
    const key = 'feed-device-id';
    let id = localStorage.getItem(key);
    if (!id) {
        id = crypto.randomUUID();
        localStorage.setItem(key, id);
    }
    return id;
}

export default function FeedPage({ active, onOpenProfile, onRequireAuth, exposeControls }) {
    const { token, user } = useAuth();

    const [videoInfoDialogOpen, setVideoInfoDialogOpen] = useState(false);
    const [commentsDialogOpen, setCommentsDialogOpen] = useState(false);
    const [qualityDialogOpen, setQualityDialogOpen] = useState(false);
    const [complaintDialogOpen, setComplaintDialogOpen] = useState(false);

    const playerRef = useRef(null);
    const preload1Ref = useRef(null);
    const preload2Ref = useRef(null);
    const qualitySwitchRef = useRef(null);

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

            const newSnapshot = {
                feedId: data.feedId ?? null,
                prevPos: data.prevPos ?? null,
                nextPos: data.nextPos ?? null,
                number: data.number ?? 0,
                first: data.first ?? true,
                last: data.last ?? true,
            };
            const newVideos = (data.items ?? data.content ?? []).map((v, index) => ({
                ...v,
                snapshot: newSnapshot,
                localIndex: index,
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

    async function prefetch(direction) {
        if (loading) return;
        setLoading(true);

        try {
            let url, res;

            if (feedModeRef.current.type === 'global') {
                const { feedId, nextPos, prevPos } = snapshot;
                const pos = direction === 'next' ? (nextPos ?? 0) : prevPos; // если nextPos null, то берем 0

                if ((prevPos === null && nextPos === null) || pos === null) {
                    return; // если страница одна либо pos null - выходим
                }

                url = `${apiBase}/api/streaming/video/feed/session/${feedId}/${direction}?count=${PAGE_SIZE}&pos=${pos}`;
                res = await fetch(url, { headers: { ...authHeaders(token), ...deviceIdHeaders(deviceIdRef.current) } });
            } else {
                const { number, first, last } = snapshot;
                let page;
                if (direction === 'next') {
                    page = last ? 0 : number + 1;
                } else {
                    page = number - 1;
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

            const newSnapshot = {
                feedId: data.feedId ?? null,
                prevPos: data.prevPos ?? null,
                nextPos: data.nextPos ?? null,
                number: data.number ?? 0,
                first: data.first ?? true,
                last: data.last ?? true,
            };
            const newVideos = (data.items ?? data.content ?? []).map((v, index) => ({
                ...v,
                snapshot: newSnapshot,
                localIndex: index,
            }));

            setWindow((prev) => {
                let nextVideos;
                let nextIndex = prev.currentIndex;

                if (direction === 'next') {
                    nextVideos = [...prev.videos, ...newVideos];
                } else {
                    nextVideos = [...newVideos, ...prev.videos];
                }

                if (nextVideos.length > MAX_WINDOW) {
                    const overflow = nextVideos.length - MAX_WINDOW;
                    if (direction === 'next') {
                        nextVideos = nextVideos.slice(overflow);
                        nextIndex = Math.max(prev.currentIndex - overflow, 0);
                    } else {
                        nextVideos = nextVideos.slice(0, MAX_WINDOW);
                        nextIndex = prev.currentIndex + newVideos.length;
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
            onRequireAuth?.();
            return;
        }

        try {
            const res = await fetch(`${apiBase}/api/videos/${uuid}/like`, {
                method: likedByMe ? 'DELETE' : 'POST',
                headers: authHeaders(token),
            });

            if (isUnauthorized(res)) {
                onRequireAuth?.();
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

    function preloadVideo(videoIndex, el, mode) {
        if (!el) return;
        if (videoIndex < 0 || videoIndex >= videos.length) return;
        const meta = videos[videoIndex];
        const url = meta.selectedQuality
            ? `${apiBase}/api/streaming/video/${meta.uuid}?quality=${meta.selectedQuality}`
            : `${apiBase}/api/streaming/video/${meta.uuid}`;
        if (el.src === url) return;

        try {
            el.pause();
            el.removeAttribute('src');
            el.load();
            el.preload = mode || 'auto';
            el.src = url;
            el.load();
        } catch {
        }
    }

    function preloadAround(index) {
        if (videos.length === 0 || index < 0) return;
        preloadVideo(index + 1, preload1Ref.current, 'auto');
        preloadVideo(index + 2, preload2Ref.current, 'metadata');
    }

    const goNext = () => {
        if (!videos.length) return;

        if (currentIndex < videos.length - 1) {
            const nextIdx = currentIndex + 1;
            setCurrentIndex(nextIdx);

            if (videos.length - 1 - nextIdx <= PREFETCH_DELTA) {
                prefetch('next');
            }
        } else {
            const type = feedModeRef.current.type;
            const { prevPos, nextPos, first, last } = snapshot;

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

            if (nextIdx <= PREFETCH_DELTA) {
                prefetch('prev');
            }
        }
    }

    useEffect(() => {
        exposeControls?.({
            reloadFeed: () => {
                feedModeRef.current = { type: 'global' };
                targetUuidRef.current = null;

                const player = playerRef.current;
                if (player) {
                    try {
                        player.pause();
                        player.removeAttribute('src');
                        player.load();
                    } catch {
                    }
                }

                startFeed();
            },
            switchToProfile: (userId, sort, uuid) => {
                // 1. переключаем режим
                feedModeRef.current = {
                    type: 'profile',
                    userId,
                    sort,
                };

                // 2. ставим цель
                targetUuidRef.current = uuid || null;

                const player = playerRef.current;
                if (player) {
                    try {
                        player.pause();
                        player.removeAttribute('src');
                        player.load();
                    } catch {
                    }
                }

                startFeed();
            },
            getGlobalIndexByUuid: (uuid) => videos.findIndex((v) => v.uuid === uuid),
            playByIndex: (idx) => setCurrentIndex(idx),
            goNext,
            goPrev,
        });
    }, [exposeControls, videos]);

    useEffect(() => {
        if (!active) return;
        if (videos.length === 0) startFeed();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [active]);

    useEffect(() => {
        const player = playerRef.current;
        if (!active || !uuid || !player) return;

        const url = selectedQuality
            ? `${apiBase}/api/streaming/video/${uuid}?quality=${selectedQuality}`
            : `${apiBase}/api/streaming/video/${uuid}`;

        if (player.src === url) return;

        const isQualitySwitch = qualitySwitchRef.current?.uuid === uuid &&
            qualitySwitchRef.current.toQuality === selectedQuality;
        const shouldPlay = isQualitySwitch ? !player.paused : true;
        const currentTime = isQualitySwitch ? player.currentTime : 0;

        qualitySwitchRef.current = null;

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

        if (!isQualitySwitch) {
            preloadAround(currentIndex);
            registerView(uuid);
        }
    }, [active, uuid, selectedQuality]);

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
            player.removeEventListener('ended', onEnded);
            player.removeEventListener('error', onError);
        };
    }, [active]);

    const handleQualitySelect = (quality) => {
        if (!currentVideo) return;

        qualitySwitchRef.current = { uuid, toQuality: quality };

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
                <Stack direction="row" p={1}>
                    <Typography variant="h6" px={1}>Рекомендации</Typography>
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
                        onClick={() => uploaderId && onOpenProfile?.(uploaderId)}
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
                <video ref={preload1Ref} id="preloadNext1" style={{ display: 'none' }} muted preload="auto"/>
                <video ref={preload2Ref} id="preloadNext2" style={{ display: 'none' }} muted preload="metadata"/>
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
                onOpenProfile={onOpenProfile}
                onRequireAuth={onRequireAuth}
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