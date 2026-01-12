import { useEffect, useState } from 'react';
import { apiBase, authHeaders } from '../api/api';
import { useAuth } from '../context/AuthContext';
import { ThumbGrid } from "../components/ThumbGrid.jsx";
import { Avatar, Box, Button, Grid, IconButton, Paper, Stack, Typography } from "@mui/material";
import { formatFollowers, formatFollowing } from "../utils/format.js";
import SettingsRoundedIcon from "@mui/icons-material/SettingsRounded";
import LogoutRoundedIcon from "@mui/icons-material/LogoutRounded";
import SecurityRoundedIcon from "@mui/icons-material/SecurityRounded";

import { AvatarUploadDialog } from "../components/AvatarUploadDialog.jsx";
import { FollowingList } from "../components/FollowingList.jsx";
import { UploadsList } from "../components/UploadsList.jsx";

export default function AccountPage({ active, activeTab, setActiveTab, onPage }) {
    const { token, user, setAuth, isModerator } = useAuth();

    const [avatarDialogOpen, setAvatarDialogOpen] = useState(false);
    const [status, setStatus] = useState({ msg: '', type: '' });
    const [profile, setProfile] = useState(null);

    function setStatusAuth(msg = '', type = '') {
        setStatus({ msg, type });
    }

    function logout() {
        setAuth(null, null);
        setStatusAuth('Вы вышли из аккаунта', 'ok');
        setProfile(null);
        onPage("auth")
    }

    async function loadAccountProfile(u = user, tokenOverride = token) {
        if (!u || !tokenOverride) {
            setProfile(null);
            return;
        }

        try {
            const res = await fetch(
                apiBase + '/api/users/' + u.userId + '/profile',
                { headers: authHeaders(tokenOverride) }
            );

            if (!res.ok) throw new Error('HTTP ' + res.status);

            const prof = await res.json();
            setProfile(prof);
        } catch {
            setProfile(null);
        }
    }

    useEffect(() => {
        if (!active) return;

        if (user && token) {
            loadAccountProfile();
        } else {
            setProfile(null);
        }
    }, [active, user?.userId, token]);

    if (!active) return null;

    const followers = profile?.followersCount ?? 0;
    const followingCount = profile?.followingCount ?? 0;

    return (
        <>
            <Stack component={Paper} direction="row" p={1} spacing={1} alignItems="center">
                <Typography variant="h6" px={1} flex={1}>Ваш профиль</Typography>
                {isModerator && (
                    <IconButton onClick={() => onPage("moderation")} size="small">
                        <SecurityRoundedIcon/>
                    </IconButton>
                )}
                <IconButton onClick={() => setAvatarDialogOpen(true)} size="small">
                    <SettingsRoundedIcon/>
                </IconButton>
                <IconButton onClick={logout} size="small">
                    <LogoutRoundedIcon/>
                </IconButton>
            </Stack>
            <Stack component={Paper} pt={2} flex={1} overflow="hidden">
                <Stack direction="row" px={2} spacing={1} alignItems="flex-start" position="relative">
                    <Avatar
                        src={user?.avatarUrl}
                        sx={{ width: 42, height: 42, fontSize: 24, fontWeight: "medium" }}
                    >
                        {user?.username?.[0]?.toUpperCase()}
                    </Avatar>
                    <Box flex={1}>
                        <Typography variant="body1" fontWeight="bold">
                            {profile?.displayName || user?.displayName || user?.username}
                        </Typography>
                        <Typography component="p" variant="caption" fontWeight="medium">
                            @{profile?.username || user?.username}
                        </Typography>
                    </Box>
                </Stack>

                <Typography variant="caption" color="text.secondary" mt={1} px={2}>
                    {followers.toLocaleString()} {formatFollowers(followers)} ·{" "}
                    {followingCount.toLocaleString()} {formatFollowing(followingCount)}
                </Typography>

                <Grid container spacing={1} mt={1.5} mb={2} px={2}>
                    <Button
                        variant="contained"
                        color={activeTab === "videos" ? "primary" : "inherit"}
                        size="small"
                        onClick={() => setActiveTab("videos")}
                    >
                        Видео
                    </Button>
                    <Button
                        variant="contained"
                        color={activeTab === "following" ? "primary" : "inherit"}
                        size="small"
                        onClick={() => setActiveTab("following")}
                    >
                        Подписки
                    </Button>
                    <Button
                        variant="contained"
                        color={activeTab === "uploads" ? "primary" : "inherit"}
                        size="small"
                        onClick={() => setActiveTab("uploads")}
                    >
                        Загрузки
                    </Button>
                </Grid>

                {activeTab === "videos" ? (
                    <ThumbGrid
                        label="Ваши видео"
                        userId={user?.userId}
                        showDelete
                        onPage={onPage}
                    />
                ) : activeTab === "following" ? (
                    <FollowingList
                        active={activeTab === "following"}
                        onPage={onPage}
                    />
                ) : (
                    <UploadsList
                        active={activeTab === "uploads"}
                        onPage={onPage}
                    />
                )}

                <AvatarUploadDialog
                    open={avatarDialogOpen}
                    setOpen={setAvatarDialogOpen}
                    onSuccess={(avatarUrl) => {
                        setProfile((p) => p ? { ...p, avatarUrl } : p);
                    }}
                />
            </Stack>
        </>
    );
}