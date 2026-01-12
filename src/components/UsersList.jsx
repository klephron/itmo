import { Avatar, Box, Button, Card, CardActionArea, Chip, Grid, Stack, Typography } from "@mui/material";
import RemoveCircleRoundedIcon from "@mui/icons-material/RemoveCircleRounded";
import AddCircleRoundedIcon from "@mui/icons-material/AddCircleRounded";
import ScheduleRoundedIcon from "@mui/icons-material/ScheduleRounded";
import MailOutlineRoundedIcon from "@mui/icons-material/MailOutlineRounded";

import { apiBase, authHeaders, getAvatarUrl, isUnauthorized } from "../api/api.js";
import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext.jsx";
import { formatDate } from "../utils/format.js";

const roleMap = {
    "USER": { label: "Пользователь", color: "default" },
    "MODERATOR": { label: "Модератор", color: "info" },
    "ADMIN": { label: "Администратор", color: "success" }
}

export function UsersList({ onPage }) {
    const { token, isModerator, isAdmin } = useAuth();

    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    async function loadUsers() {
        if (!token || !isModerator) {
            setUsers([]);
            return;
        }

        if (loading) return;

        setLoading(true);
        setError("");

        try {
            const res = await fetch(
                `${apiBase}/api/moderation/users`,
                { headers: authHeaders(token) }
            );

            if (isUnauthorized(res)) {
                throw new Error("Сессия истекла");
            }
            if (!res.ok) {
                throw new Error("Ошибка загрузки пользователей");
            }

            const list = await res.json();
            setUsers(Array.isArray(list) ? list : []);
        } catch (e) {
            console.error(e);
            setError(e.message || "Ошибка загрузки");
            setUsers([]);
        } finally {
            setLoading(false);
        }
    }

    async function toggleBan(user) {
        if (!token || !isModerator) return;

        const action = user.enabled ? "ban" : "unban";
        const url = `${apiBase}/api/moderation/users/${user.id}/${action}`;

        // optimistic update
        setUsers(prev =>
            prev.map(u =>
                u.id === user.id ? { ...u, enabled: !u.enabled } : u
            )
        );

        try {
            const res = await fetch(url, {
                method: "POST",
                headers: authHeaders(token),
            });

            if (isUnauthorized(res) || !res.ok) {
                throw new Error("Ошибка изменения статуса");
            }
        } catch (e) {
            console.error(e);
            // rollback
            setUsers(prev =>
                prev.map(u =>
                    u.id === user.id ? { ...u, enabled: user.enabled } : u
                )
            );
        }
    }

    async function toggleUserRole(userId, role, hasRole) {
        if (!token || !isAdmin) return;

        const user = users.find(u => u.id === userId);
        if (!user) return;

        const nextRoles = hasRole
            ? user.roles.filter(r => r !== role)
            : [...user.roles, role];

        // optimistic update
        setUsers(prev =>
            prev.map(u =>
                u.id === userId ? { ...u, roles: nextRoles } : u
            )
        );

        try {
            const res = await fetch(
                `${apiBase}/api/admin/users/${userId}/roles`,
                {
                    method: "POST",
                    headers: {
                        ...authHeaders(token),
                        "Content-Type": "application/json",
                    },
                    body: JSON.stringify({ roles: nextRoles }),
                }
            );

            if (isUnauthorized(res) || !res.ok) {
                throw new Error("Ошибка сохранения ролей");
            }
        } catch (e) {
            console.error(e);
            // rollback
            setUsers(prev =>
                prev.map(u =>
                    u.id === userId ? { ...u, roles: user.roles } : u
                )
            );
        }
    }

    useEffect(() => {
        loadUsers();
    }, [token, isModerator]);

    const handleBanClick = (user) => (e) => {
        e.stopPropagation();
        toggleBan(user);
    }

    const handleRoleClick = (userId, role, hasRole) => (e) => {
        e.stopPropagation();
        toggleUserRole(userId, role, hasRole);
    }

    const handleMouseDown = (e) => e.stopPropagation();
    const handleAreaClick = (userId) => () => onPage("profile", { profileUserId: userId });

    return (
        <>
            <Stack direction="row" spacing={1} py={0.5} px={2}>
                <Typography variant="h6" fontWeight="bold">
                    Список пользователей
                </Typography>
                <Typography variant="body2" color="text.secondary" pt={1}>
                    {users.length}
                </Typography>
            </Stack>
            {!users.length ? (
                <Typography mt={1.5} px={2}>
                    Пользователи не найдены
                </Typography>
            ) : (
                <Box sx={{ borderRadius: 4, flex: 1, overflow: "hidden", position: "relative", m: 2 }}>
                    <Stack spacing={1.5} sx={{ height: 1, overflowX: "hidden", overflowY: "auto" }}>
                        {users.map(u => (
                            <Card key={u.id} elevation={1} sx={{ flexShrink: 0 }}>
                                <CardActionArea component="div" onClick={handleAreaClick(u.id)}>
                                    <Stack spacing={2} p={2}>
                                        <Stack direction="row" spacing={2} alignItems="flex-start">
                                            <Avatar
                                                src={getAvatarUrl(u?.id)}
                                                sx={{ width: 32, height: 32 }}
                                            >
                                                {u?.username?.[0]?.toUpperCase()}
                                            </Avatar>
                                            <Stack spacing={0.5} flex={1} pt={0.25}>
                                                <Typography variant="caption" fontWeight="medium" lineHeight={1}>
                                                    {u.displayName || u.username}
                                                </Typography>
                                                <Typography variant="caption" color="text.secondary" lineHeight={1}>
                                                    @{u.username} · #{u.id}
                                                </Typography>
                                            </Stack>
                                            <Button
                                                size="small"
                                                variant="contained"
                                                color={u.enabled ? "error" : "primary"}
                                                onClick={handleBanClick(u)}
                                                onMouseDown={handleMouseDown}
                                            >
                                                {u.enabled ? 'Забанить' : 'Разбанить'}
                                            </Button>
                                        </Stack>
                                        <Grid container spacing={1}>
                                            {isAdmin ? Object.entries(roleMap).map(([role, { label, color }]) => {
                                                const hasRole = u.roles.includes(role);
                                                return (
                                                    <Chip
                                                        key={role}
                                                        size="small"
                                                        label={label}
                                                        color={hasRole ? color : "default"}
                                                        variant={hasRole ? "filled" : "outlined"}
                                                        onDelete={handleRoleClick(u.id, role, hasRole)}
                                                        deleteIcon={hasRole ? (
                                                            <RemoveCircleRoundedIcon onMouseDown={handleMouseDown}/>
                                                        ) : (
                                                            <AddCircleRoundedIcon onMouseDown={handleMouseDown}/>
                                                        )}
                                                        sx={!hasRole && {
                                                            borderStyle: "dashed",
                                                            color: "text.disabled"
                                                        }}
                                                    />
                                                );
                                            }) : u.roles.map((role) => (
                                                <Chip key={role} size="small" {...roleMap[role]}/>
                                            ))}
                                        </Grid>
                                        <Grid container columnSpacing={2} rowSpacing={1}>
                                            <Stack direction="row" spacing={0.5} alignItems="center">
                                                <ScheduleRoundedIcon
                                                    sx={{ fontSize: "inherit", color: "text.secondary" }}
                                                />
                                                <Typography variant="caption" lineHeight={1}>
                                                    {formatDate(u?.createdAt)}
                                                </Typography>
                                            </Stack>
                                            <Stack direction="row" spacing={0.5} alignItems="center">
                                                <MailOutlineRoundedIcon
                                                    sx={{ fontSize: "inherit", color: "text.secondary" }}
                                                />
                                                <Typography variant="caption" lineHeight={1}>
                                                    {u?.email}
                                                </Typography>
                                            </Stack>
                                        </Grid>
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
