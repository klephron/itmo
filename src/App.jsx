import { useEffect, useRef, useState } from 'react';
import { MemoryRouter, Navigate, Route, Routes, useLocation } from "react-router";
import { CssBaseline, Stack, ThemeProvider } from "@mui/material";

import Tabs from './components/Tabs';
import FeedPage from './pages/FeedPage';
import UploadPage from './pages/UploadPage';
import AccountPage from './pages/AccountPage.jsx';
import ProfilePage from './pages/ProfilePage';
import ModerationPage from './pages/ModerationPage';
import AuthPage from './pages/AuthPage.jsx';
import { AuthProvider } from './context/AuthContext';
import { theme } from "./theme.js";
import { objectsAreEqual } from "./utils/compare.js";
import { ThumbGrid } from "./components/ThumbGrid.jsx";
import { FollowingList } from "./components/FollowingList.jsx";
import { UploadsList } from "./components/UploadsList.jsx";
import { UsersList } from "./components/UsersList.jsx";
import { ComplaintsList } from "./components/ComplaintsList.jsx";
import { Service } from "./components/Service.jsx";

function AppInner() {
    const location = useLocation();

    const [activeModerationTab, setActiveModerationTab] = useState("users");
    const [feedMode, setFeedMode] = useState({ type: "global" });
    const [searchDialogOpen, setSearchDialogOpen] = useState(false);

    const feedControlsRef = useRef(null);

    /* Синхронизируем состояние фида с роутером */
    useEffect(() => {
        const newFeedMode = location.state?.feedMode;
        //console.log(newFeedMode);
        if (location.pathname !== "/feed" || !newFeedMode || objectsAreEqual(feedMode, newFeedMode)) return;

        //setFeedMode(newFeedMode);
    }, [location.key]);

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
            <Routes>
                <Route path="/feed" element={null}/>
                <Route path="/upload" element={<UploadPage/>}/>
                <Route path="/profile/:userId" element={<ProfilePage/>}>
                    <Route index element={<ThumbGrid/>}/>
                </Route>
                <Route path="/account" element={<AccountPage/>}>
                    <Route index element={<Navigate to="videos" replace/>}/>
                    <Route path="videos" element={<ThumbGrid label="Ваши видео" showDelete/>}/>
                    <Route path="following" element={<FollowingList/>}/>
                    <Route path="uploads" element={<UploadsList/>}/>
                </Route>
                <Route path="/auth" element={<AuthPage/>}/>
                <Route path="/moderation" element={<ModerationPage/>}>
                    <Route index element={<Navigate to="users" replace/>}/>
                    <Route path="users" element={<UsersList/>}/>
                    <Route path="complaints" element={<ComplaintsList/>}/>
                    <Route path="service" element={<Service/>}/>
                </Route>
            </Routes>
            <FeedPage
                active={location.pathname === "/feed"}
                feedMode={feedMode}
                setFeedMode={setFeedMode}
                exposeControls={(ctl) => {
                    feedControlsRef.current = ctl;
                }}
                searchDialogOpen={searchDialogOpen}
                setSearchDialogOpen={setSearchDialogOpen}
            />
            <Tabs feedMode={feedMode} feedControlsRef={feedControlsRef}/>
        </Stack>
    );
}

export default function App() {
    return (
        <ThemeProvider theme={theme} defaultMode="dark" noSsr>
            <CssBaseline/>
            <AuthProvider>
                <MemoryRouter initialEntries={[{ pathname: "/feed", state: { feedMode: { type: "global" } } }]}>
                    <AppInner/>
                </MemoryRouter>
            </AuthProvider>
        </ThemeProvider>
    );
}
