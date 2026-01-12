import { useEffect, useState } from "react";
import { Avatar, Box, Button, IconButton, Paper, Stack, Typography } from "@mui/material";

import { apiBase, authHeaders, getAvatarUrl, isUnauthorized } from "../api/api";
import { useAuth } from "../context/AuthContext";
import { formatFollowers, formatFollowing } from "../utils/format.js";
import { ThumbGrid } from "../components/ThumbGrid.jsx";
import ArrowBackRoundedIcon from "@mui/icons-material/ArrowBackRounded";

export default function ProfilePage({ active, userId, onPage }) {
    const { token, user } = useAuth();

    const [status, setStatus] = useState({ msg: '', type: '' });
    const [profile, setProfile] = useState(null);

    function setStatusProfile(msg = '', type = '') {
        setStatus({ msg, type });
    }

    const isSelf = user && Number(user.userId) === Number(userId);

    async function loadProfile() {
        try {
            const res = await fetch(
                `${apiBase}/api/users/${userId}/profile`,
                { headers: authHeaders(token) }
            );

            if (!res.ok) throw new Error('HTTP ' + res.status);
            const prof = await res.json();
            setProfile(prof);
        } catch (e) {
            setStatusProfile('Ошибка загрузки профиля: ' + e.message, 'error');
        }
    }

    async function toggleFollow() {
        if (!token) {
            setStatusProfile('Войдите, чтобы подписываться', 'error');
            onPage("auth");
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
                onPage("auth");
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

    useEffect(() => {
        if (!active || !userId) return;
        loadProfile();
    }, [active, userId, token]);

    if (!active) return null;

    const followers = profile?.followersCount ?? 0;
    const followingCount = profile?.followingCount ?? 0;

    const handleBack = () => onPage("feed");

    return (
        <>
            <Stack component={Paper} direction="row" p={1} alignItems="center">
                <IconButton onClick={handleBack} size="small">
                    <ArrowBackRoundedIcon/>
                </IconButton>
                <Typography variant="h6" noWrap px={1}>Профиль пользователя</Typography>
            </Stack>
            <Stack component={Paper} pt={2} flex={1} overflow="hidden">
                <Stack direction="row" spacing={1} px={2} alignItems="flex-start">
                    <Avatar
                        src={getAvatarUrl(userId)}
                        sx={{ width: 42, height: 42, fontSize: 24, fontWeight: "medium" }}
                    >
                        {profile?.username?.[0]?.toUpperCase()}
                    </Avatar>
                    <Box flex={1}>
                        <Typography variant="body1" fontWeight="bold">
                            {profile?.displayName}
                        </Typography>
                        <Typography component="p" variant="caption" fontWeight="medium">
                            @{profile?.username}
                        </Typography>
                    </Box>
                    {!isSelf && !!userId && (
                        <Button
                            size="large"
                            variant="contained"
                            color={profile?.following ? "inherit" : "primary"}
                            onClick={toggleFollow}
                        >
                            {profile?.following ? "Отписаться" : "Подписаться"}
                        </Button>
                    )}
                </Stack>
                <Typography variant="caption" color="text.secondary" mt={1} mb={1.5} px={2}>
                    {followers.toLocaleString()} {formatFollowers(followers)} · {followingCount.toLocaleString()} {formatFollowing(followingCount)}
                </Typography>
                <ThumbGrid userId={userId} onPage={onPage}/>
            </Stack>
        </>
    );
}
