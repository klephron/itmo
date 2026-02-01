import { useEffect, useState } from "react";
import { Outlet, useNavigate, useParams } from "react-router";
import { Avatar, Box, Button, IconButton, Paper, Stack, Typography } from "@mui/material";
import ArrowBackRoundedIcon from "@mui/icons-material/ArrowBackRounded";
import { useSnackbar } from "notistack";

import { apiBase, authHeaders, getAvatarUrl, isUnauthorized } from "../api/api";
import { useAuth } from "../context/AuthContext";
import { formatFollowers, formatFollowing } from "../utils/format.js";

export default function ProfilePage() {
    const { token, user, setAuth } = useAuth();
    const { enqueueSnackbar } = useSnackbar();
    const { userId } = useParams();
    const navigate = useNavigate();

    const [profile, setProfile] = useState(null);
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
            enqueueSnackbar("Ошибка загрузки профиля: " + e.message, { variant: "error" });
        }
    }

    async function toggleFollow() {
        if (!token) {
            enqueueSnackbar("Войдите, чтобы подписываться", { variant: "info" });
            navigate("/auth");
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
                enqueueSnackbar("Сессия истекла, войдите снова", { variant: "error" });
                navigate("/auth");
                setAuth(null, null);
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            setProfile((prevProfile) => ({
                ...prevProfile,
                following: !prevProfile.following,  // Меняем статус подписки
            }));
        } catch (e) {
            enqueueSnackbar("Ошибка изменения подписки: " + e.message, { variant: "error" });
        }
    }

    useEffect(() => {
        if (!userId) return;
        loadProfile();
    }, [userId, token]);

    const followers = profile?.followersCount ?? 0;
    const followingCount = profile?.followingCount ?? 0;

    return (
        <>
            <Stack component={Paper} direction="row" p={1} alignItems="center">
                <IconButton onClick={() => navigate(-1)} size="small">
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
                <Outlet context={{ userId }}/>
            </Stack>
        </>
    );
}
