import { useCallback, useRef, useState } from 'react';
import { CssBaseline, Stack, ThemeProvider } from "@mui/material";

import Tabs from './components/Tabs';
import FeedPage from './pages/FeedPage';
import UploadPage from './pages/UploadPage';
import AccountPage from './pages/AccountPage.jsx';
import ProfilePage from './pages/ProfilePage';
import ModerationPage from './pages/ModerationPage';
import AuthPage from './pages/AuthPage.jsx';
import { AuthProvider, useAuth } from './context/AuthContext';
import { theme } from "./theme.js";

function AppInner() {
    const { token, user } = useAuth();

    const [activePage, setActivePage] = useState('feed'); // Изначально активен Feed
    const [profileUserId, setProfileUserId] = useState(null); // Идентификатор профиля
    const [activeAccountTab, setActiveAccountTab] = useState("videos");
    const [feedMode, setFeedMode] = useState({ type: "global" });
    const [searchDialogOpen, setSearchDialogOpen] = useState(false);

    const feedControlsRef = useRef(null);

    const onPage = useCallback((page, { activeAccountTab, profileUserId, feedMode } = {}) => {
        if (page === 'upload' && !token) {
            setActivePage('auth');
            return;
        }

        if (page === 'feed' && feedMode) setFeedMode(feedMode);
        if (page === 'account' && activeAccountTab) setActiveAccountTab(activeAccountTab);
        if (page === 'profile' && profileUserId) setProfileUserId(profileUserId);

        setActivePage(page);
    }, [token]);

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
                onPage={onPage}
                feedMode={feedMode}
                setFeedMode={setFeedMode}
                exposeControls={(ctl) => {
                    feedControlsRef.current = ctl;
                }}
                searchDialogOpen={searchDialogOpen}
                setSearchDialogOpen={setSearchDialogOpen}
            />
            <UploadPage
                active={activePage === 'upload'}
                onPage={onPage}
            />
            <ProfilePage
                active={activePage === 'profile'}
                userId={profileUserId}
                onPage={onPage}
            />
            <AccountPage
                active={activePage === 'account'}
                activeTab={activeAccountTab}
                setActiveTab={setActiveAccountTab}
                onPage={onPage}
            />
            <AuthPage active={activePage === 'auth'} onPage={onPage}/>
            <ModerationPage active={activePage === 'moderation'} onPage={onPage}/>
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
