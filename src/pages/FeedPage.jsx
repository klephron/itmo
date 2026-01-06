import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
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
    const { token } = useAuth();

    const [videoInfoDialogOpen, setVideoInfoDialogOpen] = useState(false);

    const playerRef = useRef(null);
    const preload1Ref = useRef(null);
    const preload2Ref = useRef(null);

    const feedModeRef = useRef({ type: 'global' }); // { type: 'global' } | { type: 'profile', userId, sort }
    const targetUuidRef = useRef(null);

    const feedSessionRef = useRef({
        // global
        feedId: null,
        prevPos: null,
        nextPos: null,

        // profile
        page: 0,
        first: true,
        last: true,
    });

    const deviceIdRef = useRef(getOrCreateDeviceId());

    const [videos, setVideos] = useState([]);
    const [currentIndex, setCurrentIndex] = useState(-1);

    const [loading, setLoading] = useState(false);

    const currentVideo = useMemo(() => {
        if (currentIndex < 0 || currentIndex >= videos.length) return null;
        return videos[currentIndex];
    }, [videos, currentIndex]);

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
    } = currentVideo ?? {};

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
            const items = data.items ?? data.content;

            setVideos(items);
            feedSessionRef.current = {
                feedId: data.feedId ?? null,
                prevPos: data.prevPos ?? null,
                nextPos: data.nextPos ?? null,
                page: data.number ?? 0,
                first: data.first ?? true,
                last: data.last ?? true,
            };

            const targetUuid = targetUuidRef.current;
            if (targetUuid) {
                const idx = items.findIndex(v => v.uuid === targetUuid);
                if (idx !== -1) {
                    setCurrentIndex(idx);
                    targetUuidRef.current = null; // цель достигнута
                    return;
                }
            }

            setCurrentIndex(0);
        } finally {
            setLoading(false);
        }
    }

    console.log(videos);

    async function prefetch(direction) {
        if (loading) return;
        setLoading(true);

        try {
            let url, res;

            if (feedModeRef.current.type === 'global') {
                const { feedId, nextPos, prevPos } = feedSessionRef.current;
                const pos = direction === 'next' ? (nextPos ?? 0) : prevPos; // если nextPos null, то берем 0

                if ((prevPos === null && nextPos === null) || pos === null) {
                    return; // если страница одна либо pos null - выходим
                }

                const url =
                    `${apiBase}/api/streaming/video/feed/session/${feedId}/${direction}` +
                    `?size=${PAGE_SIZE}&pos=${pos}`;

                res = await fetch(url, { headers: { ...authHeaders(token), ...deviceIdHeaders(deviceIdRef.current) } });
            } else {
                const { userId, sort } = feedModeRef.current;
                const { page, first, last } = feedSessionRef.current;

                if (first && last) return; // если страница одна - выходим

                let nextPage;
                if (direction === 'next') {
                    nextPage = last ? 0 : page + 1;
                } else {
                    nextPage = page - 1;
                }

                url = apiBase +
                    `/api/users/${userId}/videos?page=${nextPage}&size=${PAGE_SIZE}&sort=${encodeURIComponent(sort)}`;

                res = await fetch(url, { headers: authHeaders(token) });
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);
            const data = await res.json();
            const items = data.items ?? data.content ?? [];

            feedSessionRef.current = {
                feedId: data.feedId ?? null,
                prevPos: data.prevPos ?? null,
                nextPos: data.nextPos ?? null,
                page: data.number ?? 0,
                first: data.first ?? true,
                last: data.last ?? true,
            };

            setVideos((prev) => {
                let next;
                if (direction === 'next') {
                    next = prev.concat(items);
                } else {
                    next = items.concat(prev);
                    setCurrentIndex((i) => i + items.length);
                }

                if (next.length > MAX_WINDOW) {
                    const overflow = next.length - MAX_WINDOW;
                    if (direction === 'next') {
                        next = next.slice(overflow);
                        setCurrentIndex((i) => Math.max(i - overflow, 0));
                    } else {
                        next = next.slice(0, MAX_WINDOW);
                    }
                }
                return next;
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
        const url = apiBase + '/api/streaming/video/' + meta.uuid
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

    const goNext = useCallback(async () => {
        if (!videos.length) return;

        if (currentIndex < videos.length - 1) {
            const nextIdx = currentIndex + 1;
            setCurrentIndex(nextIdx);

            if (videos.length - 1 - nextIdx <= PREFETCH_DELTA) {
                prefetch('next');
            }
        } else {
            const type = feedModeRef.current.type;
            const { prevPos, nextPos, first, last } = feedSessionRef.current;

            // Конец окна и одна страница - перемещаемся в начало окна
            if ((type === 'global' && (prevPos === nextPos)) || (type === 'profile' && (first === last))) {
                setCurrentIndex(0);
            }
        }
    }, [currentIndex, videos.length]);

    const goPrev = useCallback(() => {
        if (!videos.length) return;

        if (currentIndex > 0) {
            const nextIdx = currentIndex - 1;
            setCurrentIndex(nextIdx);

            if (nextIdx <= PREFETCH_DELTA) {
                prefetch('prev');
            }
        } else {
            setStatusFeed('Это самое первое видео', 'ok');
        }
    }, [currentIndex, videos.length]);

    useEffect(() => {
        exposeControls?.({
            reloadFeed: () => {
                feedModeRef.current = { type: 'global' };
                targetUuidRef.current = null;

                feedSessionRef.current = {
                    feedId: null,
                    prevPos: null,
                    nextPos: null,
                    page: 0,
                    first: true,
                    last: true,
                };

                setVideos([]);
                setCurrentIndex(-1);

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

                // 3. ЖЁСТКО сбрасываем состояние
                feedSessionRef.current = {
                    feedId: null,
                    prevPos: null,
                    nextPos: null,
                    page: 0,
                    first: true,
                    last: true,
                };

                setVideos([]);
                setCurrentIndex(-1);

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
    }, [exposeControls, videos, goNext, goPrev]);

    useEffect(() => {
        if (!active) return;
        if (videos.length === 0) startFeed();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [active]);

    useEffect(() => {
        const player = playerRef.current;
        if (!active || !uuid || !player) return;

        try {
            player.pause();
            player.removeAttribute('src');
            player.load();

            player.src = apiBase + '/api/streaming/video/' + uuid;
            player.load();

            player.play().catch(() => {
                /* autoplay может быть заблокирован */
            });
        } catch {
            return;
        }

        preloadAround(currentIndex);
        registerView(uuid);
    }, [active, currentIndex, uuid]);

    useEffect(() => {
        const player = playerRef.current;
        if (!player) return;

        const onEnded = () => {
            goNext();
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
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentIndex, videos, loading]);

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
                            sx={{ width: 24, height: 24 }}
                        >
                            {uploaderName?.[0]?.toUpperCase()}
                        </Avatar>
                        <Typography sx={{ userSelect: "none" }}>{uploaderName}</Typography>
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
                            onClick={() => {
                                /* действие добавишь позже */
                            }}
                        >
                            <CommentOutlined sx={{ fontSize: 32 }}/>
                        </Button>
                        <Typography variant="caption">
                            {formatCompactNumber(commentsCount)}
                        </Typography>
                    </Stack>
                    <Button
                        size="uniSmall"
                        onClick={() => {
                            /* действие добавишь позже */
                        }}
                    >
                        <SettingsOutlinedIcon sx={{ fontSize: 32 }}/>
                    </Button>
                    <Button
                        size="uniSmall"
                        onClick={() => {
                            /* действие добавишь позже */
                        }}
                    >
                        <OutlinedFlagRoundedIcon sx={{ fontSize: 32 }}/>
                    </Button>
                </Stack>
                <video
                    ref={playerRef}
                    id="player"
                    controls
                    playsInline
                    muted
                    autoPlay
                />
                <video ref={preload1Ref} id="preloadNext1" style={{ display: 'none' }} muted preload="auto"/>
                <video ref={preload2Ref} id="preloadNext2" style={{ display: 'none' }} muted preload="metadata"/>
            </Box>
            {/*
            <Comments videoUuid={currentUuid} onOpenProfile={onOpenProfile} onRequireAuth={onRequireAuth}/>
            */}
            <VideoInfoDialog
                open={videoInfoDialogOpen}
                setOpen={setVideoInfoDialogOpen}
                video={currentVideo}
            />
        </Stack>
    );
}
