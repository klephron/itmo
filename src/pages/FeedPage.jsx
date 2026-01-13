import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from "react-router";

import { Avatar, Box, Button, CircularProgress, IconButton, Paper, Stack, Typography } from "@mui/material";
import FavoriteRoundedIcon from "@mui/icons-material/FavoriteRounded";
import FavoriteBorderRoundedIcon from "@mui/icons-material/FavoriteBorderRounded";
import CommentOutlined from "@mui/icons-material/CommentOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import OutlinedFlagRoundedIcon from "@mui/icons-material/OutlinedFlagRounded";
import SettingsOutlinedIcon from "@mui/icons-material/SettingsOutlined";
import SearchRoundedIcon from "@mui/icons-material/SearchRounded";
import ArrowBackRoundedIcon from "@mui/icons-material/ArrowBackRounded";
import RefreshRoundedIcon from "@mui/icons-material/RefreshRounded";

import { apiBase, authHeaders, deviceIdHeaders, getAvatarUrl, getOrCreateDeviceId, isUnauthorized } from '../api/api';
import { formatCompactNumber } from '../utils/format';
import { useAuth } from '../context/AuthContext';
import { VideoInfoDialog } from "../components/VideoInfoDialog.jsx";
import { CommentsDialog } from "../components/CommentsDialog.jsx";
import { QualityDialog } from "../components/QualityDialog.jsx";
import { ComplaintDialog } from "../components/ComplaintDialog.jsx";
import { SearchDialog } from "../components/SearchDialog.jsx";
import { objectsAreEqual } from "../utils/compare.js";

const PAGE_SIZE = 10;
const MAX_WINDOW = 2 * PAGE_SIZE;
const PREFETCH_DELTA = 2;
const WATCH_MILESTONES = [25, 50, 75, 100];
const MIN_LOADING_MS = 400;

export default function FeedPage({
                                     feedMode,
                                     setFeedMode,
                                     exposeControls,
                                     searchDialogOpen,
                                     setSearchDialogOpen
                                 }) {
    const { token, user, authReady } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();
    const active = location.pathname === "/feed";
    const feedModeFromLocation = location.state?.feedMode;

    const [videoInfoDialogOpen, setVideoInfoDialogOpen] = useState(false);
    const [commentsDialogOpen, setCommentsDialogOpen] = useState(false);
    const [qualityDialogOpen, setQualityDialogOpen] = useState(false);
    const [complaintDialogOpen, setComplaintDialogOpen] = useState(false);

    const [trigger, setTrigger] = useState(0);

    const playerRef = useRef(null);
    const preload1Ref = useRef(null);
    const preload2Ref = useRef(null);
    const playerStateRef = useRef({
        uuid: null,
        shouldPlay: true,
        currentTime: 0,
        viewSent: false,
        watchSent: new Set(),
    });

    const feedModeRef = useRef(feedMode);
    const deviceIdRef = useRef(getOrCreateDeviceId());

    const [window, setWindow] = useState({ videos: [], currentIndex: -1 });
    const { videos, currentIndex } = window;
    const currentVideo = videos[currentIndex] ?? null;

    const {
        uuid,
        name,
        uploaderId,
        uploaderName,
        likesCount = 0,
        likedByMe = false,
        viewsCount = 0,
        commentsCount = 0,
        snapshot = {},
        localIndex = -1,
        selectedQuality = null,
    } = currentVideo ?? {};

    const [loading, setLoading] = useState(false);
    const [showLoadingAnimation, setShowLoadingAnimation] = useState(false);

    const setVideos = (callback) => setWindow((prev) => ({ ...prev, videos: callback(prev.videos) }));
    const setCurrentIndex = (currentIndex) => setWindow((prev) => ({ ...prev, currentIndex }));

    async function startFeed(jumpToTarget = false, showAnimation = false) {
        if (loading) return;

        setLoading(true);
        if (showAnimation) setShowLoadingAnimation(true);
        const startTs = performance.now();

        try {
            let url, res;
            const { type, userId, sort, query, targetUuid, targetIndex } = feedModeRef.current;

            if (type === "moderation") {
                const res = await fetch(
                    `${apiBase}/api/streaming/video/metadata/${targetUuid}`,
                    { headers: authHeaders(token) }
                );
                if (!res.ok) throw new Error("HTTP " + res.status);

                const video = await res.json();
                setWindow({ videos: [video], currentIndex: 0 });

                return;
            }

            if (type === "global") {
                url = `${apiBase}/api/streaming/video/feed/session/start?size=${PAGE_SIZE}`;
                res = await fetch(url, {
                    method: 'POST',
                    headers: { ...authHeaders(token), ...deviceIdHeaders(deviceIdRef.current) }
                });
            } else {
                const page = jumpToTarget ? Math.floor(targetIndex / PAGE_SIZE) : 0;
                if (type === "user") {
                    url = `${apiBase}/api/users/${userId}/videos?page=${page}&size=${PAGE_SIZE}&sort=${encodeURIComponent(sort)}`;
                    res = await fetch(url, { headers: authHeaders(token) });
                } else {
                    url = `${apiBase}/api/search/videos?q=${encodeURIComponent(query)}&page=${page}&size=${PAGE_SIZE}`;
                    res = await fetch(url, { headers: authHeaders(token) });
                }
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
            if (jumpToTarget && targetUuid) {
                const idx = newVideos.findIndex(v => v.uuid === targetUuid);
                if (idx !== -1) newIndex = idx;
            }

            setWindow({ videos: newVideos, currentIndex: newIndex });
        } finally {
            setLoading(false);

            if (showAnimation) {
                const elapsed = performance.now() - startTs;
                const delay = Math.max(0, MIN_LOADING_MS - elapsed);

                if (delay > 0) {
                    setTimeout(() => setShowLoadingAnimation(false), delay);
                } else {
                    setShowLoadingAnimation(false);
                }
            }
        }
    }

    async function prefetch(direction, snapshot, restoreCurPage = false) {
        if (loading) return;
        setLoading(true);

        try {
            let pos, page, url, res;
            const { type, userId, sort, query } = feedModeRef.current;

            if (type === "global") {
                const { feedId, nextPos, curPos, prevPos } = snapshot;
                if (direction === "next") {
                    pos = nextPos;
                    if (pos == null) {
                        url = `${apiBase}/api/streaming/video/feed/session/start?size=${PAGE_SIZE}`;
                        res = await fetch(url, {
                            method: 'POST',
                            headers: { ...authHeaders(token), ...deviceIdHeaders(deviceIdRef.current) }
                        });
                    } else {
                        url = `${apiBase}/api/streaming/video/feed/session/${feedId}/next?count=${PAGE_SIZE}&pos=${pos}`;
                        res = await fetch(url, {
                            headers: { ...authHeaders(token), ...deviceIdHeaders(deviceIdRef.current) }
                        });
                    }
                } else {
                    pos = restoreCurPage ? curPos : prevPos;
                    if (pos == null) return;

                    url = `${apiBase}/api/streaming/video/feed/session/${feedId}/prev?count=${PAGE_SIZE}&pos=${pos}`;
                    res = await fetch(url, {
                        headers: { ...authHeaders(token), ...deviceIdHeaders(deviceIdRef.current) }
                    });
                }
            } else {
                const { number, last } = snapshot;
                if (direction === 'next') {
                    page = last ? 0 : number + 1;
                } else {
                    page = restoreCurPage ? number : number - 1;
                }
                if (page < 0) return;

                if (type === "user") {
                    url = `${apiBase}/api/users/${userId}/videos?page=${page}&size=${PAGE_SIZE}&sort=${encodeURIComponent(sort)}`;
                    res = await fetch(url, { headers: authHeaders(token) });
                } else {
                    url = `${apiBase}/api/search/videos?q=${encodeURIComponent(query)}&page=${page}&size=${PAGE_SIZE}`;
                    res = await fetch(url, { headers: authHeaders(token) });
                }
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
        if (!token || !uuid || playerStateRef.current.viewSent) return;

        try {
            const res = await fetch(`${apiBase}/api/videos/${uuid}/view`, {
                method: 'POST',
                headers: authHeaders(token)
            });

            if (!res.ok) return;

            const data = await res.json();
            const viewsCount = data?.viewsCount;

            playerStateRef.current.viewSent = true;

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

    async function registerWatch(percent, positionSeconds, durationSeconds) {
        const state = playerStateRef.current;
        if (!token || !uuid || state.uuid !== uuid || state.watchSent.has(percent)) return;

        try {
            const res = await fetch(
                `${apiBase}/api/videos/${uuid}/watch`,
                {
                    method: 'POST',
                    headers: {
                        ...authHeaders(token),
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        percent,
                        positionSeconds: Math.floor(positionSeconds),
                        durationSeconds: Math.floor(durationSeconds),
                    }),
                }
            );

            if (!res.ok) return;

            // ТОЛЬКО тут фиксируем успех
            state.watchSent.add(percent);
        } catch {
            // ничего не делаем — будет повтор при следующем timeupdate
        }
    }

    const toggleLike = async () => {
        if (!currentVideo) return;
        if (!token) {
            navigate("/auth");
            return;
        }

        try {
            const res = await fetch(`${apiBase}/api/videos/${uuid}/like`, {
                method: likedByMe ? 'DELETE' : 'POST',
                headers: authHeaders(token),
            });

            if (isUnauthorized(res)) {
                navigate("/auth");
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

    async function loadVideoMeta(uuid) {
        if (!uuid) return;

        try {
            const headers = authHeaders(token)
            const [viewsRes, commentsRes, likeRes] = await Promise.all([
                fetch(`${apiBase}/api/videos/${uuid}/view`, { headers }),
                fetch(`${apiBase}/api/videos/${uuid}/comments?page=0&size=1`, { headers }),
                fetch(`${apiBase}/api/videos/${uuid}/like`, { headers }),
            ]);

            let viewsCount;
            if (viewsRes.ok) {
                const data = await viewsRes.json();
                viewsCount = data?.viewsCount;
            }

            let commentsCount;
            if (commentsRes.ok) {
                const data = await commentsRes.json();
                commentsCount = data?.totalElements;
            }

            let likesCount, likedByMe;
            if (likeRes.ok) {
                const data = await likeRes.json();
                likesCount = data?.likesCount;
                likedByMe = data?.liked;
            }

            setVideos((prev) => {
                const copy = [...prev];
                const idx = copy.findIndex(v => v.uuid === uuid);
                if (idx === -1) return prev;

                copy[idx] = {
                    ...copy[idx],
                    ...(typeof viewsCount === 'number' && { viewsCount }),
                    ...(typeof commentsCount === 'number' && { commentsCount }),
                    ...(typeof likesCount === 'number' && { likesCount }),
                    ...(typeof likedByMe === 'boolean' && { likedByMe }),
                };

                return copy;
            });
        } catch {
            // мета — не критичная
        }
    }

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
        const type = feedModeRef.current.type;
        if (type === "moderation") return;

        // Если видео еще нет либо мы как-то оказались на последнем видео в окне - перезапускаем ленту
        if (!videos.length || currentIndex === videos.length - 1) {
            startFeed(false, true);
            return;
        }

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
    }

    const goPrev = () => {
        const type = feedModeRef.current.type;
        if (!videos.length || type === "moderation") return;

        const { prevPos, first } = snapshot;

        // Если нулевой элемент первой страницы - назад идти нельзя!
        if (
            localIndex === 0 &&
            ((type === "global" && prevPos === null) || ((type === "user" || type === "search") && first))
        ) {
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

    const savePlayerState = () => {
        const player = playerRef.current;
        if (!player) return;

        playerStateRef.current = {
            ...playerStateRef.current,
            uuid,
            shouldPlay: !player.paused,
            currentTime: player.currentTime
        };
    }

    useEffect(() => {
        exposeControls?.({ goNext, goPrev, savePlayerState });
    }, [exposeControls, window, loading]);

    /* Старт ленты при первом входе на страницу, смене режима или если нет видео */
    useEffect(() => {
        if (!active || !authReady) return;

        if (!objectsAreEqual(feedMode, feedModeFromLocation)) {
            feedModeRef.current = feedModeFromLocation;
            setFeedMode(feedModeFromLocation);
            startFeed(true, false);
            return;
        }

        if (videos.length === 0) {
            startFeed(false, false);
        }
    }, [location, authReady]);

    /* Сброс состояния ленты, если сменился токен */
    useEffect(() => {
        if (!authReady) return;
        setFeedMode({ type: "global" });
        setWindow({ videos: [], currentIndex: -1 });
    }, [token]);

    /* Изменение состояния плеера */
    useEffect(() => {
        const player = playerRef.current;
        if (!player) return;

        /* Ушли со страницы - сохраняем состояние и останавливаем плеер */
        if (!active) {
            playerStateRef.current = {
                ...playerStateRef.current,
                shouldPlay: !player.paused,
                currentTime: player.currentTime,
            };
            player.pause();
            return;
        }

        /* Нет видео - на всякий случай сбрасываем плеер */
        if (!uuid) {
            player.pause();
            player.removeAttribute("src");
            player.load();
            return;
        }

        const url = selectedQuality
            ? `${apiBase}/api/streaming/video/${uuid}?quality=${selectedQuality}`
            : `${apiBase}/api/streaming/video/${uuid}`;

        const isNewVideo = playerStateRef.current.uuid !== uuid;
        const isNewUrl = player.src !== url;

        /* Сброс сохраненного состояния плеера, если видео изменилось */
        if (isNewVideo) {
            playerStateRef.current = {
                uuid,
                shouldPlay: true,
                currentTime: 0,
                viewSent: false,
                watchSent: new Set(),
            };
        }

        /* Меняем src ТОЛЬКО если он реально изменился */
        if (isNewUrl) {
            player.pause();
            player.src = url;
            player.load();
            return;
        }

        /* Если видео осталось старое, но мы вернулись на страницу - восстанавливаем состояние здесь */
        if (playerStateRef.current.shouldPlay && player.paused) {
            player.play().catch(() => {
                /* ignore */
            });
        }
    }, [active, uuid, selectedQuality]);

    /* Загрузка актуальной мета-информации видео. Не учитываем токен, т.к. выше есть очистка (которая сменит uuid) */
    useEffect(() => {
        if (!uuid) return;
        loadVideoMeta(uuid);
    }, [uuid, trigger]);

    /* Загрузка следующих видео при любых изменениях в окне либо при открытии страницы */
    useEffect(() => {
        if (!active) return;
        preloadAround(currentIndex);
    }, [active, window]);

    /* Сбрасываем ВСЕ диалоги FeedPage */
    useEffect(() => {
        if (active) return;

        setVideoInfoDialogOpen(false);
        setCommentsDialogOpen(false);
        setQualityDialogOpen(false);
        setComplaintDialogOpen(false);
        setSearchDialogOpen(false);
    }, [active]);

    const handleVideoError = () => {
        setStatusFeed('Ошибка воспроизведения видео', 'error');
    }

    const handleVideoEnded = () => {
        const player = playerRef.current;
        if (!player) return;

        player.currentTime = 0;
        player.play().catch(() => {
            /* ignore */
        });
    }

    const handleVideoPlay = () => registerView();
    const handleVideoCanPlay = () => {
        const player = playerRef.current;
        const state = playerStateRef.current;
        if (!player || !state.shouldPlay || searchDialogOpen) return;

        player.play().catch(() => {
            /* ignore */
        });
    };

    const handleVideoLoadedMetadata = () => {
        const player = playerRef.current;
        const state = playerStateRef.current;
        if (!player) return;

        player.currentTime = state.currentTime;
    };

    const handleVideoTimeUpdate = () => {
        const player = playerRef.current;
        if (!player) return;

        const currentTime = player.currentTime;
        const duration = player.duration;
        if (!duration || !isFinite(duration)) return;

        const percent = Math.floor((currentTime / duration) * 100);
        for (const m of WATCH_MILESTONES) {
            if (percent >= m) {
                registerWatch(m, currentTime, duration);
            }
        }
    };

    const handleQualitySelect = (quality) => {
        const player = playerRef.current;
        if (!uuid || !player) return;

        savePlayerState();
        setVideos((prev) => {
            const copy = [...prev];
            const idx = copy.findIndex(v => v.uuid === uuid);
            if (idx >= 0) {
                copy[idx] = { ...copy[idx], selectedQuality: quality };
            }
            return copy;
        });
    };

    return (
        <>
            {active && (
                <Stack component={Paper} direction="row" p={1} alignItems="center">
                    {feedMode.type === "global" && (
                        <>
                            <Typography variant="h6" noWrap px={1} flex={1}>Рекомендации</Typography>
                            <IconButton onClick={() => setSearchDialogOpen(true)} size="small">
                                <SearchRoundedIcon/>
                            </IconButton>
                        </>
                    )}
                    {feedMode.type === "user" && (
                        <>
                            <IconButton onClick={() => navigate(-1)} size="small">
                                <ArrowBackRoundedIcon/>
                            </IconButton>
                            <Typography variant="h6" noWrap px={1}>Видео пользователя</Typography>
                        </>
                    )}
                    {feedMode.type === "search" && (
                        <>
                            <IconButton onClick={() => navigate(-1)} size="small">
                                <ArrowBackRoundedIcon/>
                            </IconButton>
                            <Typography variant="h6" noWrap px={1}>Найденные видео</Typography>
                        </>
                    )}
                    {feedMode.type === "moderation" && (
                        <>
                            <IconButton onClick={() => navigate(-1)} size="small">
                                <ArrowBackRoundedIcon/>
                            </IconButton>
                            <Typography variant="h6" noWrap px={1}>Видео из жалобы</Typography>
                        </>
                    )}
                </Stack>
            )}
            {uuid ? (
                <Box sx={{
                    flex: 1,
                    overflow: 'hidden',
                    position: 'relative',
                    display: active ? "block" : "none",
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
                            onClick={() => navigate(`/profile/${uploaderId}`)}
                        >
                            <Avatar
                                src={getAvatarUrl(uploaderId)}
                                sx={{ width: 32, height: 32 }}
                            >
                                {uploaderName?.[0]?.toUpperCase()}
                            </Avatar>
                            <Typography sx={{ userSelect: "none" }} fontWeight="medium">{uploaderName}</Typography>
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
                            <Button size="uniSmall" onClick={() => setCommentsDialogOpen(true)}>
                                <CommentOutlined sx={{ fontSize: 32 }}/>
                            </Button>
                            <Typography variant="caption">
                                {formatCompactNumber(commentsCount)}
                            </Typography>
                        </Stack>
                        <Button size="uniSmall" onClick={() => setQualityDialogOpen(true)}>
                            <SettingsOutlinedIcon sx={{ fontSize: 32 }}/>
                        </Button>
                        {user && uploaderId !== user.userId && (
                            <Button size="uniSmall" onClick={() => setComplaintDialogOpen(true)}>
                                <OutlinedFlagRoundedIcon sx={{ fontSize: 32 }}/>
                            </Button>
                        )}
                    </Stack>
                    <Box
                        component="video"
                        ref={playerRef}
                        controls
                        playsInline
                        muted
                        sx={{
                            width: 1,
                            height: 1,
                            objectFit: "contain",
                            bgcolor: "black",
                            display: "block",
                            borderRadius: 4,
                        }}
                        onEnded={handleVideoEnded}
                        onError={handleVideoError}
                        onPlay={handleVideoPlay}
                        onCanPlay={handleVideoCanPlay}
                        onLoadedMetadata={handleVideoLoadedMetadata}
                        onTimeUpdate={handleVideoTimeUpdate}
                    />
                    <Box component="video" display="none" ref={preload1Ref} muted/>
                    <Box component="video" display="none" ref={preload2Ref} muted/>
                </Box>
            ) : active && (
                <Stack
                    component={Paper}
                    spacing={1}
                    alignItems="center"
                    justifyContent="center"
                    flex={1}
                    p={2}
                >
                    <Typography variant="h6">
                        Лента закончилась
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Новых видео пока нет
                    </Typography>
                    <Button
                        size="uniSmall"
                        onClick={() => startFeed(false, true)}
                        loading={showLoadingAnimation}
                        loadingIndicator={<CircularProgress size={23} color="primary"/>}
                    >
                        <RefreshRoundedIcon fontSize="large"/>
                    </Button>
                </Stack>
            )}
            <VideoInfoDialog
                open={active && videoInfoDialogOpen}
                setOpen={setVideoInfoDialogOpen}
                video={currentVideo}
            />
            <CommentsDialog
                open={active && commentsDialogOpen}
                setOpen={setCommentsDialogOpen}
                videoUuid={uuid}
                setTrigger={setTrigger}
            />
            <QualityDialog
                open={active && qualityDialogOpen}
                setOpen={setQualityDialogOpen}
                video={currentVideo}
                onSelect={handleQualitySelect}
            />
            <ComplaintDialog
                open={active && complaintDialogOpen}
                setOpen={setComplaintDialogOpen}
                mode="video"
                videoUuid={uuid}
            />
            <SearchDialog
                open={active && searchDialogOpen}
                setOpen={setSearchDialogOpen}
            />
        </>
    );
}