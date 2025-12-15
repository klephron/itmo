import React, { useEffect, useMemo, useState } from 'react';
import { apiBase, authHeaders, isUnauthorized } from '../api/api';  // Импорт apiBase
import { useAuth } from '../context/AuthContext';
import Status from '../components/Status';
import ThumbGrid, { ThumbCard } from '../components/ThumbGrid';

export default function ProfilePage({
                                        active,
                                        userId,
                                        onRequireAuth,
                                        onGlobalPlayByUuid,
                                        onLoadMoreRequest,
                                    }) {
    // Хуки всегда вызываются, даже если компонент не рендерится
    const { token, user } = useAuth();
    const [status, setStatus] = useState({ msg: '', type: '' });
    const [profile, setProfile] = useState(null);
    const [videos, setVideos] = useState([]);
    const [paging, setPaging] = useState({ page: 0, size: 12, last: false, loading: false });

    function setStatusProfile(msg = '', type = '') {
        setStatus({ msg, type });
    }

    const isSelf = useMemo(() => user && Number(user.userId) === Number(userId), [user, userId]);

    // Загружаем данные профиля при активации компонента
    useEffect(() => {
        if (!active || !userId) return;

        let cancelled = false;

        async function load() {
            setStatusProfile('Загрузка профиля…', 'ok');
            setVideos([]);
            setProfile(null);
            setPaging({ page: 0, size: 12, last: false, loading: false });

            try {
                const [userRes, videosRes] = await Promise.all([
                    fetch(`${apiBase}/api/users/${userId}/profile`, { headers: authHeaders(token) }),
                    fetch(`${apiBase}/api/users/${userId}/videos?page=0&size=12`, { headers: authHeaders(token) }),
                ]);

                if (!userRes.ok) throw new Error('user HTTP ' + userRes.status);
                if (!videosRes.ok) throw new Error('videos HTTP ' + videosRes.status);

                const prof = await userRes.json();
                const vidsPage = await videosRes.json();

                if (cancelled) return;

                const vids = Array.isArray(vidsPage?.content) ? vidsPage.content : [];
                const totalVideos = typeof vidsPage?.totalElements === 'number' ? vidsPage.totalElements : vids.length;

                setProfile({ ...prof, totalVideos });
                setVideos(vids);

                setPaging({
                    page: (typeof vidsPage?.number === 'number' ? vidsPage.number : 0) + 1,
                    size: 12,
                    last: !!vidsPage?.last,
                    loading: false,
                });

                setStatusProfile('', '');
            } catch (e) {
                if (cancelled) return;
                setStatusProfile('Ошибка загрузки профиля: ' + e.message, 'error');
            }
        }

        load();
        return () => {
            cancelled = true;
        };
    }, [active, userId, token]);

    // Загрузка дополнительных видео
    async function loadMore() {
        if (!userId) return;
        if (paging.loading || paging.last) return;

        setPaging((s) => ({ ...s, loading: true }));
        setStatusProfile('Загрузка видео…', 'ok');

        try {
            const res = await fetch(`${apiBase}/api/users/${userId}/videos?page=${paging.page}&size=${paging.size}`, {
                headers: authHeaders(token),
            });

            if (!res.ok) throw new Error('HTTP ' + res.status);
            const page = await res.json();
            const vids = page?.content || [];

            setVideos((prev) => prev.concat(vids));
            setPaging((s) => ({
                ...s,
                page: (typeof page?.number === 'number' ? page.number : s.page) + 1,
                last: page?.last === true,
                loading: false,
            }));
            setStatusProfile('', '');
        } catch (e) {
            setStatusProfile('Ошибка загрузки видео: ' + e.message, 'error');
            setPaging((s) => ({ ...s, loading: false }));
        }
    }

    useEffect(() => {
        if (!active) return;
        onLoadMoreRequest?.(() => loadMore());
    }, [active, paging, userId]);

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

    // Возвращаем null, если активная вкладка не "Profile"
    if (!active) return null;

    const totalVideos = profile?.totalVideos ?? videos.length;
    const followers = profile?.followersCount ?? 0;
    const followingCount = profile?.followingCount ?? 0;

    return (
        <div className={'card page ' + (active ? 'active' : '')} id="pageProfile">
            <div className="header">
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <div className="profile-avatar" id="profileAvatar">
                        {profile?.avatarUrl ? <img src={`${apiBase}${profile?.avatarUrl}`} alt=""/> : null}
                    </div>
                    <div>
                        <div className="title" id="profileDisplayName">
                            {profile?.displayName || profile?.username || 'Профиль'}
                        </div>
                        <div className="studio-row-meta" id="profileUsername">
                            {profile?.username ? '@' + profile.username : ''}
                        </div>
                        <div className="studio-row-meta" id="profileStats">
                            {totalVideos} видео · {followers} подписчиков · {followingCount} подписок
                        </div>
                    </div>
                </div>

                <button
                    className="small secondary"
                    id="btnProfileFollow"
                    style={{ display: (!isSelf && !!userId) ? '' : 'none' }}
                    onClick={toggleFollow}
                >
                    {profile?.following ? 'Отписаться' : 'Подписаться'}
                </button>
            </div>

            <div className="studio-section-title">Видео</div>

            {(!videos || videos.length === 0) ? (
                <div className="studio-row-meta">Видео пока нет</div>
            ) : (
                <ThumbGrid
                    videos={videos}
                    renderCard={(v) => (
                        <ThumbCard key={v.uuid} video={v} showDelete={false}
                                   onOpen={() => onGlobalPlayByUuid?.(v.uuid)}/>
                    )}
                />
            )}

            <Status id="statusProfile" message={status.msg} type={status.type}/>
        </div>
    );
}
