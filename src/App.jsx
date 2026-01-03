import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Tabs from './components/Tabs';
import FeedPage from './pages/FeedPage';
import UploadPage from './pages/UploadPage';
import AccountPage from './pages/AccountPage.jsx';
import ProfilePage from './pages/ProfilePage';
import ModerationPage from './pages/ModerationPage';
import AuthPage from './pages/AuthPage.jsx';
import SearchPage from './pages/SearchPage.jsx';
import { AuthProvider, useAuth } from './context/AuthContext';
import { apiBase, authHeaders, isUnauthorized } from './api/api';

function AppInner() {
    const { token, user, isModerator } = useAuth();

    const [activeTab, setActiveTab] = useState('feed'); // Изначально активен Feed
    const [profileUserId, setProfileUserId] = useState(null); // Идентификатор профиля

    const [activeJobs, setActiveJobs] = useState([]);  // Статус активных задач
    const jobsRef = useRef(new Map());  // Мапа для активных задач
    const [statusStudio, setStatusStudio] = useState(''); // Статус для студии

    const feedControlsRef = useRef(null);
    const profileLoadMoreRef = useRef(null);
    const authLoadMoreRef = useRef(null);

    const showModerationTab = !!(user && isModerator);  // Проверка на наличие модератора

    const onTab = useCallback((tab) => {
        if (tab === 'upload' && !token) {
            setActiveTab('auth');
            return;
        }
        if (tab === 'moderation' && (!token || !showModerationTab)) {
            setActiveTab('auth');
            return;
        }
        setActiveTab(tab); // Установка активной вкладки
    }, [token, showModerationTab]);

    const onRequireAuth = useCallback(() => setActiveTab('auth'), []);  // Переключение на вкладку авторизации

    const onOpenProfile = useCallback((userId) => {
        setProfileUserId(userId);  // Открытие профиля
        setActiveTab('profile');  // Переключаем вкладку на профайл
    }, []);

    const onGlobalPlayByUuid = useCallback((uuid) => {
        const ctl = feedControlsRef.current;
        setActiveTab('feed');
        if (!ctl) return;

        const idx = ctl.getGlobalIndexByUuid(uuid);
        if (idx !== -1) ctl.playByIndex(idx);
        else ctl.reloadFeed?.();
    }, []);

    const onReloadFeed = useCallback(async () => {
        const ctl = feedControlsRef.current;
        await ctl?.reloadFeed?.();
    }, []);

    const renderStudioJobs = useCallback(() => {
        setActiveJobs(Array.from(jobsRef.current.values()).map((j) => ({
            jobId: j.jobId,
            fileName: j.fileName,
            lastStatus: j.lastStatus,
        })));
    }, []);

    const trackJob = useCallback((jobId, fileName, initialStatus) => {
        if (!jobId) return;
        if (jobsRef.current.has(jobId)) return;

        const job = { jobId, fileName, lastStatus: initialStatus || null, intervalId: null };
        jobsRef.current.set(jobId, job);
        renderStudioJobs();

        const intervalId = setInterval(async () => {
            try {
                const res = await fetch(apiBase + '/api/streaming/video/upload/status/' + jobId, {
                    headers: authHeaders(token),
                });

                if (isUnauthorized(res)) {
                    setStatusStudio('Сессия истекла, войдите снова');
                    clearInterval(job.intervalId);
                    jobsRef.current.delete(jobId);
                    renderStudioJobs();
                    onRequireAuth();
                    return;
                }

                if (!res.ok) throw new Error('HTTP ' + res.status);

                const st = await res.json();
                job.lastStatus = st;
                renderStudioJobs();

                if (st.stage === 'DONE') {
                    clearInterval(job.intervalId);
                    jobsRef.current.delete(jobId);
                    renderStudioJobs();
                    setStatusStudio('');
                    await onReloadFeed();
                }

                if (st.stage === 'ERROR') {
                    clearInterval(job.intervalId);
                    jobsRef.current.delete(jobId);
                    renderStudioJobs();
                    setStatusStudio(st.message || 'Ошибка обработки');
                }
            } catch (e) {
                setStatusStudio('Ошибка запроса статуса: ' + e.message);
            }
        }, 1000);

        job.intervalId = intervalId;
    }, [token, onRequireAuth, onReloadFeed, renderStudioJobs]);

    const loadActiveJobsFromServer = useCallback(async (tokenOverride = token) => {
        if (!tokenOverride) {
            setStatusStudio('');
            jobsRef.current.forEach((job) => job.intervalId && clearInterval(job.intervalId));
            jobsRef.current.clear();
            renderStudioJobs();
            return;
        }

        try {
            const res = await fetch(apiBase + '/api/streaming/video/upload/active', {
                headers: authHeaders(tokenOverride),
            });

            if (isUnauthorized(res)) {
                setStatusStudio('Сессия истекла, войдите снова');
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);
            const jobs = await res.json();

            jobsRef.current.forEach((job) => job.intervalId && clearInterval(job.intervalId));
            jobsRef.current.clear();

            (jobs || []).forEach((j) => {
                const initial = { stage: j.stage, percent: j.percent, message: j.message };
                trackJob(j.jobId, j.fileName || '(без названия)', initial);
            });

            renderStudioJobs();
        } catch (e) {
            setStatusStudio('Ошибка загрузки активных задач: ' + e.message);
        }
    }, [token, trackJob, renderStudioJobs]);

    useEffect(() => {
        loadActiveJobsFromServer(token);
    }, [token]); // eslint-disable-line react-hooks/exhaustive-deps

    useEffect(() => {
        function onScroll() {
            const nearBottom = window.innerHeight + window.scrollY >= document.body.offsetHeight - 200;
            if (!nearBottom) return;

            if (activeTab === 'profile') profileLoadMoreRef.current?.();
            if (activeTab === 'auth') authLoadMoreRef.current?.();
        }

        window.addEventListener('scroll', onScroll);
        return () => window.removeEventListener('scroll', onScroll);
    }, [activeTab]);

    const jobsState = useMemo(() => ({ activeJobs, statusStudio }), [activeJobs, statusStudio]);

    return (
        <div className="app">
            <Tabs
                activeTab={activeTab}  // Передаем активное состояние
                onTab={onTab}
                showModeration={showModerationTab}
                isAuthenticated={Boolean(user)}
            />
            <FeedPage
                active={activeTab === 'feed'}
                onOpenProfile={onOpenProfile}
                onRequireAuth={onRequireAuth}
                exposeControls={(ctl) => {
                    feedControlsRef.current = ctl;
                }}
            />
            <UploadPage
                active={activeTab === 'upload'}
                onRequireAuth={onRequireAuth}
                onJobTracked={(jobId, label, initialStatus) => {
                    trackJob(jobId, label, initialStatus);
                    setActiveTab('account'); // как в оригинале: прыжок в "Аккаунт"
                }}
            />
            <ProfilePage
                active={activeTab === 'profile'}  // Передаем активное состояние для профиля
                userId={profileUserId}
                onRequireAuth={onRequireAuth}
                onGlobalPlayByUuid={onGlobalPlayByUuid}
                onLoadMoreRequest={(fn) => {
                    profileLoadMoreRef.current = fn;
                }}
            />
            <AccountPage
                active={activeTab === 'account'}  // Передаем активное состояние для страницы аккаунта
                onOpenProfile={onOpenProfile}
                onRequireAuth={onRequireAuth}
                onGlobalPlayByUuid={onGlobalPlayByUuid}
                onReloadFeed={onReloadFeed}
                jobsState={jobsState}
                onLoadActiveJobsFromServer={loadActiveJobsFromServer}
                onRegisterLoadMore={(fn) => {
                    authLoadMoreRef.current = fn;
                }}
                onTab={onTab}
            />
            <AuthPage active={activeTab === 'auth'} onTab={onTab}/>
            <ModerationPage active={activeTab === 'moderation'} onRequireAuth={onRequireAuth}/>
            <SearchPage active={activeTab === 'search'} onGlobalPlayByUuid={onGlobalPlayByUuid}/>
        </div>
    );
}

export default function App() {
    return (
        <AuthProvider>
            <AppInner/>
        </AuthProvider>
    );
}
