import React, { useEffect, useMemo, useRef, useState } from 'react';
import { apiBase, authHeaders, isUnauthorized, parseVideoListPayload } from '../api/api';
import { formatDate, formatViews, humanSize } from '../utils/format';
import { useAuth } from '../context/AuthContext';
import Avatar from '../components/Avatar';
import Status from '../components/Status';
import Comments from '../components/Comments';

const FEED_PAGE_SIZE = 10;

export default function FeedPage({ active, onOpenProfile, onRequireAuth, exposeControls }) {
    const { token } = useAuth();

    const playerRef = useRef(null);
    const preload1Ref = useRef(null);
    const preload2Ref = useRef(null);

    const feedModeRef = useRef({ type: 'global' }); // { type: 'global' } | { type: 'profile', userId, sort }
    const targetUuidRef = useRef(null);

    const [videos, setVideos] = useState([]);
    const [currentIndex, setCurrentIndex] = useState(-1);

    const [feedPage, setFeedPage] = useState(0);
    const [hasNext, setHasNext] = useState(false);
    const [loading, setLoading] = useState(false);

    const [status, setStatus] = useState({ msg: '', type: '' });
    const [like, setLike] = useState({ count: 0, liked: false });
    const [views, setViews] = useState('—');

    const currentVideo = useMemo(() => {
        if (currentIndex < 0 || currentIndex >= videos.length) return null;
        return videos[currentIndex];
    }, [videos, currentIndex]);

    const currentUuid = currentVideo?.uuid ?? null;

    function setStatusFeed(msg = '', type = '') {
        setStatus({ msg, type });
    }

    function getVideoUrlByUuid(uuid) {
        return apiBase + '/api/streaming/video/' + uuid;
    }

    function preloadVideo(videoIndex, el, mode) {
        if (!el) return;
        if (videoIndex < 0 || videoIndex >= videos.length) return;
        const meta = videos[videoIndex];
        const url = getVideoUrlByUuid(meta.uuid);
        if (el.src === url) return;

        try {
            el.pause();
            el.removeAttribute('src');
            el.load();
            el.preload = mode || 'auto';
            el.src = url;
            el.load();
        } catch {}
    }

    function preloadAround(index) {
        if (videos.length === 0 || index < 0) return;
        preloadVideo(index + 1, preload1Ref.current, 'auto');
        preloadVideo(index + 2, preload2Ref.current, 'metadata');
    }

    async function loadVideoPage(page = 0, append = false) {
        if (loading) return;
        setLoading(true);

        try {
            let url;

            if (feedModeRef.current.type === 'profile') {
                const { userId, sort } = feedModeRef.current;
                url =
                    apiBase +
                    `/api/users/${userId}/videos?page=${page}&size=${FEED_PAGE_SIZE}&sort=${encodeURIComponent(sort)}`;
            } else {
                url =
                    apiBase +
                    `/api/streaming/video/list?page=${page}&size=${FEED_PAGE_SIZE}`;
            }

            const res = await fetch(url, { headers: authHeaders(token) });
            if (!res.ok) throw new Error('HTTP ' + res.status);

            const data = await res.json();
            const parsed = parseVideoListPayload(data);

            const nextHasNext =
                typeof data?.hasNext === 'boolean'
                    ? data.hasNext
                    : typeof data?.last === 'boolean'
                        ? !data.last
                        : parsed.items.length === FEED_PAGE_SIZE;

            setHasNext(nextHasNext);
            setFeedPage(parsed.page ?? page);

            setVideos((prev) => (append ? prev.concat(parsed.items) : parsed.items));

            setCurrentIndex((prevIdx) => {
                // старт с targetUuid (важно!)
                if (targetUuidRef.current) {
                    const idx = parsed.items.findIndex(
                        (v) => v.uuid === targetUuidRef.current
                    );
                    targetUuidRef.current = null;
                    return idx >= 0 ? idx : 0;
                }

                if (prevIdx === -1 && parsed.items.length > 0) return 0;
                if (!append && parsed.items.length > 0) return Math.min(prevIdx, parsed.items.length - 1);
                return prevIdx;
            });

            if (!append && parsed.items.length === 0) {
                setStatusFeed('Загрузите видео на вкладке "Загрузка"', 'error');
            }
        } catch (e) {
            setStatusFeed('Ошибка загрузки списка видео: ' + e.message, 'error');
        } finally {
            setLoading(false);
        }
    }

    async function goNext() {
        if (!videos.length) return;

        if (currentIndex < videos.length - 1) {
            const nextIndex = currentIndex + 1;
            setCurrentIndex(nextIndex);

            if (hasNext && !loading && (videos.length - nextIndex) <= 2) {
                loadVideoPage(feedPage + 1, true);
            }
            return;
        }

        if (hasNext && !loading) {
            await loadVideoPage(feedPage + 1, true);
            if (currentIndex < videos.length - 1) {
                setCurrentIndex(currentIndex + 1);
            } else {
                setStatusFeed('Больше видео пока нет', 'ok');
            }
            return;
        }

        await loadVideoPage(0, false);
        setCurrentIndex(0);
        setStatusFeed('Начали ленту сначала', 'ok');
    }

    async function goPrev() {
        if (!videos.length) return;
        if (currentIndex > 0) setCurrentIndex(currentIndex - 1);
        else setStatusFeed('Это самое первое видео', 'ok');
    }

    useEffect(() => {
        exposeControls?.({
            reloadFeed: () => {
                feedModeRef.current = { type: 'global' };
                targetUuidRef.current = null;

                setVideos([]);
                setCurrentIndex(-1);
                setFeedPage(0);
                setHasNext(false);

                const player = playerRef.current;
                if (player) {
                    try {
                        player.pause();
                        player.removeAttribute('src');
                        player.load();
                    } catch {}
                }

                loadVideoPage(0, false);
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
                setVideos([]);
                setCurrentIndex(-1);
                setFeedPage(0);
                setHasNext(false);

                // 4. останавливаем плеер
                const player = playerRef.current;
                if (player) {
                    try {
                        player.pause();
                        player.removeAttribute('src');
                        player.load();
                    } catch {}
                }

                // 5. загружаем новую ленту
                loadVideoPage(0, false);
            },
            getGlobalIndexByUuid: (uuid) => videos.findIndex((v) => v.uuid === uuid),
            playByIndex: (idx) => setCurrentIndex(idx),
        });
    }, [exposeControls, videos]);

    useEffect(() => {
        if (!active) return;
        if (videos.length === 0) loadVideoPage(0, false);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [active]);

    useEffect(() => {
        if (!active) return;
        if (!currentVideo) return;

        const player = playerRef.current;
        if (!player) return;

        const v = currentVideo;
        const url = getVideoUrlByUuid(v.uuid);

        try {
            player.pause();
            player.removeAttribute('src');
            player.load();
            player.src = url;
            player.load();
            player.play().catch(() => {
                setStatusFeed('Нажмите Play, чтобы запустить видео', 'error');
            });
        } catch {}

        const baseLikes = (typeof v.likesCount === 'number') ? v.likesCount : 0;
        setLike({ count: baseLikes, liked: false });

        const baseViews = (typeof v.viewsCount === 'number') ? v.viewsCount : 0;
        setViews(formatViews(baseViews));

        setStatusFeed('Воспроизведение…', 'ok');
        preloadAround(currentIndex);

        let cancelled = false;

        (async () => {
            if (!token) return;
            try {
                const res = await fetch(apiBase + '/api/videos/' + v.uuid + '/like', { headers: authHeaders(token) });
                if (cancelled) return;
                if (res.ok) {
                    const data = await res.json();
                    const likesCount = (typeof data?.likesCount === 'number') ? data.likesCount : baseLikes;
                    const liked = !!data?.liked;
                    setVideos((prev) => {
                        const copy = [...prev];
                        const idx = copy.findIndex((x) => x.uuid === v.uuid);
                        if (idx >= 0) copy[idx] = { ...copy[idx], likesCount, likedByMe: liked };
                        return copy;
                    });
                    setLike({ count: likesCount, liked });
                } else if (isUnauthorized(res)) {
                    setLike({ count: baseLikes, liked: false });
                }
            } catch {}
        })();

        (async () => {
            try {
                const resViews = await fetch(apiBase + '/api/videos/' + v.uuid + '/view', {
                    method: 'POST',
                    headers: authHeaders(token),
                });
                if (cancelled) return;
                if (resViews.ok) {
                    const data = await resViews.json();
                    const cnt = (typeof data?.viewsCount === 'number') ? data.viewsCount :
                        (typeof data?.count === 'number') ? data.count : baseViews;
                    setVideos((prev) => {
                        const copy = [...prev];
                        const idx = copy.findIndex((x) => x.uuid === v.uuid);
                        if (idx >= 0) copy[idx] = { ...copy[idx], viewsCount: cnt };
                        return copy;
                    });
                    setViews(formatViews(cnt));
                }
            } catch {}
        })();

        return () => { cancelled = true; };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [active, currentIndex, token]);

    async function toggleLike() {
        if (!currentVideo) return;
        if (!token) {
            setStatusFeed('Войдите, чтобы ставить лайки', 'error');
            onRequireAuth?.();
            return;
        }

        const method = like.liked ? 'DELETE' : 'POST';

        try {
            const res = await fetch(apiBase + '/api/videos/' + currentVideo.uuid + '/like', {
                method,
                headers: { ...authHeaders(token), 'Content-Type': 'application/json' },
            });

            if (isUnauthorized(res)) {
                setStatusFeed('Сессия истекла, войдите снова', 'error');
                onRequireAuth?.();
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);
            const data = await res.json();
            const likesCount = (typeof data?.likesCount === 'number') ? data.likesCount : like.count;
            const liked = !!data?.liked;

            setVideos((prev) => {
                const copy = [...prev];
                const idx = copy.findIndex((x) => x.uuid === currentVideo.uuid);
                if (idx >= 0) copy[idx] = { ...copy[idx], likesCount, likedByMe: liked };
                return copy;
            });

            setLike({ count: likesCount, liked });
        } catch (e) {
            setStatusFeed('Ошибка лайка: ' + e.message, 'error');
        }
    }

    useEffect(() => {
        const player = playerRef.current;
        if (!player) return;

        const onEnded = () => { goNext(); };
        const onError = () => { setStatusFeed('Ошибка воспроизведения видео', 'error'); };

        player.addEventListener('ended', onEnded);
        player.addEventListener('error', onError);
        return () => {
            player.removeEventListener('ended', onEnded);
            player.removeEventListener('error', onError);
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentIndex, videos, hasNext, loading]);

    if (!active) return null;

    const title = currentVideo ? (currentVideo.name || '(без названия)') : 'Загрузка списка видео…';
    const authorName = currentVideo?.uploaderName || '';
    const displayTitle = authorName ? `${title} — ${authorName}` : title;

    return (
        <div className={'card page ' + (active ? 'active' : '')} id="pageFeed">
            <div className="header">
                <div>
                    <div className="title" id="videoTitle">{displayTitle}</div>
                    <div
                        className="studio-row-meta"
                        id="videoAuthorName"
                        style={{ cursor: currentVideo?.uploaderId ? 'pointer' : 'default' }}
                        onClick={() => currentVideo?.uploaderId && onOpenProfile?.(currentVideo.uploaderId)}
                        title={currentVideo?.uploaderId ? 'Открыть профиль' : undefined}
                    >
                        {authorName}
                    </div>
                </div>
                <div className="badge" id="videoCount">{videos.length} видео</div>
            </div>

            <div className="video-wrapper">
                <video ref={playerRef} id="player" controls playsInline muted autoPlay />
                <div className="swipe-hint">Свайп вверх/вниз или кнопки ниже</div>

                <video ref={preload1Ref} id="preloadNext1" style={{ display: 'none' }} muted preload="auto" />
                <video ref={preload2Ref} id="preloadNext2" style={{ display: 'none' }} muted preload="metadata" />
            </div>

            <div className="controls">
                <button className="secondary" id="btnPrev" onClick={goPrev}>⏮ Предыдущее</button>
                <button id="btnNext" onClick={goNext}>▶ Следующее</button>
                <button
                    className="secondary"
                    id="btnLike"
                    onClick={toggleLike}
                    data-liked={like.liked ? 'true' : 'false'}
                    data-count={String(like.count)}
                >
                    {(like.liked ? '♥ ' : '♡ ') + like.count}
                </button>
            </div>

            <div className="meta">
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <Avatar
                        className="avatar-mini"
                        url={currentVideo?.uploaderAvatarUrl
                            ? `${apiBase}${currentVideo?.uploaderAvatarUrl}`
                            : null
                        }
                        onClick={() => currentVideo?.uploaderId && onOpenProfile?.(currentVideo.uploaderId)}
                    />
                    <span id="videoInfoSize">{humanSize(currentVideo?.contentSize)}</span>
                </div>
                <div style={{ textAlign: 'right' }}>
                    <div id="videoInfoViews">{views || '—'}</div>
                    <div id="videoInfoDate">{formatDate(currentVideo?.uploadedAt)}</div>
                </div>
            </div>

            <Comments videoUuid={currentUuid} onOpenProfile={onOpenProfile} onRequireAuth={onRequireAuth} />

            <Status id="statusFeed" message={status.msg} type={status.type} />
        </div>
    );
}
