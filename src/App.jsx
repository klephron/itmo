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

    const [activePage, setActivePage] = useState('feed'); // Изначально активен Feed
    const [profileUserId, setProfileUserId] = useState(null); // Идентификатор профиля
    const [activeAccountTab, setActiveAccountTab] = useState("videos");

    const feedControlsRef = useRef(null);

    const onPage = useCallback((page, { activeAccountTab, profileUserId } = {}) => {
        if (page === 'upload' && !token) {
            setActivePage('auth');
            return;
        }

        if (page === 'account' && activeAccountTab) setActiveAccountTab(activeAccountTab);
        if (page === 'profile' && profileUserId) setProfileUserId(profileUserId);

        setActivePage(page);
    }, [token]);

    const onGlobalPlayByUuid = useCallback((payload) => {
        const ctl = feedControlsRef.current;
        setActivePage('feed');
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
                active={activePage === 'feed'}
                exposeControls={(ctl) => {
                    feedControlsRef.current = ctl;
                }}
                onPage={onPage}
            />
            <UploadPage
                active={activePage === 'upload'}
                onPage={onPage}
            />
            <ProfilePage
                active={activePage === 'profile'}
                userId={profileUserId}
                onPage={onPage}
                onGlobalPlayByUuid={onGlobalPlayByUuid}
            />
            <AccountPage
                active={activePage === 'account'}
                activeTab={activeAccountTab}
                setActiveTab={setActiveAccountTab}
                onPage={onPage}
                onGlobalPlayByUuid={onGlobalPlayByUuid}
            />
            <AuthPage active={activePage === 'auth'} onPage={onPage}/>
            <ModerationPage active={activePage === 'moderation'} onPage={onPage}/>
            <SearchPage active={activePage === 'search'} onGlobalPlayByUuid={onGlobalPlayByUuid}/>
            <Tabs
                activeTab={activePage}  // Передаем активное состояние
                onTab={onPage}
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
