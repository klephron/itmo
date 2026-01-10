import { useCallback, useRef, useState } from 'react';
import { CssBaseline, Stack, ThemeProvider } from "@mui/material";

import Tabs from './components/Tabs';
import FeedPage from './pages/FeedPage';
import UploadPage from './pages/UploadPage';
import AccountPage from './pages/AccountPage.jsx';
import ProfilePage from './pages/ProfilePage';
import ModerationPage from './pages/ModerationPage';
import AuthPage from './pages/AuthPage.jsx';
import SearchPage from './pages/SearchPage.jsx';
import { AuthProvider, useAuth } from './context/AuthContext';
import { theme } from "./theme.js";

function AppInner() {
    const { token, user } = useAuth();

    const [activeTab, setActiveTab] = useState('feed'); // Изначально активен Feed
    const [profileUserId, setProfileUserId] = useState(null); // Идентификатор профиля

    const feedControlsRef = useRef(null);

    const onTab = useCallback((tab) => {
        if (tab === 'upload' && !token) {
            setActiveTab('auth');
            return;
        }
        setActiveTab(tab);
    }, [token]);

    const onRequireAuth = useCallback(() => setActiveTab('auth'), []);  // Переключение на вкладку авторизации

    const onOpenProfile = useCallback((userId) => {
        setProfileUserId(userId);  // Открытие профиля
        setActiveTab('profile');  // Переключаем вкладку на профайл
    }, []);

    const onGlobalPlayByUuid = useCallback((payload) => {
        const ctl = feedControlsRef.current;
        setActiveTab('feed');
        if (!ctl) return;

        if (typeof payload === 'string') {
            const idx = ctl.getGlobalIndexByUuid(payload);
            if (idx !== -1) ctl.playByIndex(idx);
            else ctl.reloadFeed();
            return;
        }

        const { userId, sort, uuid } = payload;
        ctl.switchToProfile(userId, sort, uuid);
    }, []);

    return (
        <Stack
            p={1}
            spacing={1}
            sx={{
                width: 1,
                maxWidth: 480,
                height: "100dvh",
                mx: "auto",
            }}
        >
            <FeedPage
                active={activeTab === 'feed'}
                onOpenProfile={onOpenProfile}
                onRequireAuth={onRequireAuth}
                exposeControls={(ctl) => {
                    feedControlsRef.current = ctl;
                }}
                onTab={onTab}
            />
            <UploadPage
                active={activeTab === 'upload'}
                onRequireAuth={onRequireAuth}
                goToPage={onTab}
            />
            <ProfilePage
                active={activeTab === 'profile'}
                userId={profileUserId}
                onRequireAuth={onRequireAuth}
                onGlobalPlayByUuid={onGlobalPlayByUuid}
            />
            <AccountPage
                active={activeTab === 'account'}
                onTab={onTab}
                onOpenProfile={onOpenProfile}
                onRequireAuth={onRequireAuth}
                onGlobalPlayByUuid={onGlobalPlayByUuid}
            />
            <AuthPage active={activeTab === 'auth'} onTab={onTab}/>
            <ModerationPage active={activeTab === 'moderation'} onRequireAuth={onRequireAuth}/>
            <SearchPage active={activeTab === 'search'} onGlobalPlayByUuid={onGlobalPlayByUuid}/>
            <Tabs
                activeTab={activeTab}  // Передаем активное состояние
                onTab={onTab}
                isAuthenticated={Boolean(user)}
                feedControlsRef={feedControlsRef}
            />
        </Stack>
    );
}

export default function App() {
    return (
        <ThemeProvider theme={theme} defaultMode="dark" noSsr>
            <CssBaseline/>
            <AuthProvider>
                <AppInner/>
            </AuthProvider>
        </ThemeProvider>
    );
}
