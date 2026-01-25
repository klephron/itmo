import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { Avatar, Box, Card, CardActionArea, Stack, Typography, } from "@mui/material";
import { useSnackbar } from "notistack";

import { apiBase, authHeaders, getAvatarUrl, isUnauthorized } from "../api/api";
import { useAuth } from "../context/AuthContext";

export function FollowingList() {
    const { user, token, setAuth } = useAuth();
    const { enqueueSnackbar } = useSnackbar();
    const navigate = useNavigate();

    const [following, setFollowing] = useState([]);

    useEffect(() => {
        if (!user || !token) return;

        let cancelled = false;
        fetch(apiBase + `/api/users/${user.userId}/following`, {
            headers: authHeaders(token),
        })
            .then((res) => {
                if (isUnauthorized(res)) {
                    enqueueSnackbar("Сессия истекла, войдите снова", { variant: "error" });
                    navigate("/auth");
                    setAuth(null, null);
                    return;
                }
                if (!res.ok) throw new Error("HTTP " + res.status);
                return res.json();
            })
            .then((list) => {
                if (cancelled) return;
                setFollowing(list || []);
            })
            .catch((e) => {
                if (cancelled) return;
                setFollowing([]);
                enqueueSnackbar("Ошибка загрузки подписок: " + e.message, { variant: "error" });
            });

        return () => {
            cancelled = true;
        };
    }, [user?.userId, token]);

    return (
        <>
            <Stack direction="row" spacing={1} py={0.5} px={2}>
                <Typography variant="h6" fontWeight="bold">
                    Ваши подписки
                </Typography>
                <Typography variant="body2" color="text.secondary" pt={1}>
                    {following.length}
                </Typography>
            </Stack>

            {!following.length ? (
                <Typography mt={1.5} px={2}>
                    Подписок пока нет
                </Typography>
            ) : (
                <Box sx={{ borderRadius: 3, flex: 1, overflow: "hidden", position: "relative", m: 1 }}>
                    <Stack sx={{ height: 1, overflowX: "hidden", overflowY: "auto" }}>
                        {following.map((u) => (
                            <Card key={u.id} sx={{ flexShrink: 0, borderRadius: 3 }}>
                                <CardActionArea onClick={() => navigate(`/profile/${u.id}`)}>
                                    <Stack direction="row" spacing={1} p={1} alignItems="center">
                                        <Avatar
                                            src={getAvatarUrl(u?.id)}
                                            sx={{ width: 32, height: 32 }}
                                        >
                                            {user?.username?.[0]?.toUpperCase()}
                                        </Avatar>
                                        <Stack spacing={0.5}>
                                            <Typography variant="caption" fontWeight="medium" lineHeight={1}>
                                                {u.displayName || u.username}
                                            </Typography>
                                            <Typography variant="caption" color="text.secondary" lineHeight={1}>
                                                @{u.username}
                                            </Typography>
                                        </Stack>
                                    </Stack>
                                </CardActionArea>
                            </Card>
                        ))}
                    </Stack>
                </Box>
            )}
        </>
    );
}