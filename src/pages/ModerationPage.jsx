import React, { useEffect, useState } from 'react';
import { apiBase, authHeaders, isUnauthorized } from '../api/api';
import { useAuth } from '../context/AuthContext';
import { Box, Button, Grid, IconButton, Paper, Stack, Typography } from "@mui/material";
import ArrowBackRoundedIcon from "@mui/icons-material/ArrowBackRounded";
import { UsersList } from "../components/UsersList.jsx";
import { ComplaintsList } from "../components/ComplaintsList.jsx";
import { Service } from "../components/Service.jsx";

export default function ModerationPage({ active, activeTab, setActiveTab, onPage }) {
    const { token, isModerator, isAdmin } = useAuth();

    const [status, setStatus] = useState({ msg: '', type: '' });
    const [users, setUsers] = useState([]);
    const [selectedUserId, setSelectedUserId] = useState(null);

    const [userDetails, setUserDetails] = useState(null);
    const [userVideos, setUserVideos] = useState([]);
    const [complaints, setComplaints] = useState([]);

    function setStatusModeration(msg = '', type = '') {
        setStatus({ msg, type });
    }

    async function loadUsers() {
        if (!token || !isModerator) {
            setUsers([]);
            return;
        }

        try {
            const res = await fetch(apiBase + '/api/moderation/users', { headers: authHeaders(token) });

            if (isUnauthorized(res)) {
                setStatusModeration('Сессия истекла, войдите снова', 'error');
                onPage("auth");
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            const list = await res.json();
            setUsers(list || []);
        } catch (e) {
            setStatusModeration('Ошибка загрузки пользователей: ' + e.message, 'error');
        }
    }

    async function loadUserDetails(id) {
        if (!token || !isModerator) return;

        try {
            const res = await fetch(apiBase + '/api/moderation/users/' + id, { headers: authHeaders(token) });

            if (res.status === 404) {
                setUserDetails(null);
                return;
            }

            if (isUnauthorized(res)) {
                setStatusModeration('Сессия истекла, войдите снова', 'error');
                onPage("auth");
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            const u = await res.json();
            setSelectedUserId(u.id);
            setUserDetails(u);
        } catch (e) {
            setStatusModeration('Ошибка загрузки деталей: ' + e.message, 'error');
        }
    }

    async function loadUserVideos(id) {
        if (!token || !isModerator) return;

        try {
            const res = await fetch(apiBase + '/api/moderation/users/' + id + '/videos', { headers: authHeaders(token) });

            if (res.status === 404) {
                setUserVideos([]);
                return;
            }

            if (isUnauthorized(res)) {
                setStatusModeration('Сессия истекла, войдите снова', 'error');
                onPage("auth");
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            const list = await res.json();
            setUserVideos(list || []);
        } catch (e) {
            setStatusModeration('Ошибка загрузки видео пользователя: ' + e.message, 'error');
        }
    }

    async function loadComplaints() {
        if (!token || !isModerator) return;

        try {
            const res = await fetch(apiBase + '/api/moderation/complaints/admin', {
                headers: authHeaders(token),
            });

            if (isUnauthorized(res)) {
                setStatusModeration('Сессия истекла, войдите снова', 'error');
                onPage("auth");
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            const list = await res.json();
            setComplaints(Array.isArray(list) ? list : []);
        } catch (e) {
            setStatusModeration('Ошибка загрузки жалоб: ' + e.message, 'error');
        }
    }

    async function toggleBan(u) {
        if (!token || !isModerator) return;

        const action = u.enabled ? 'ban' : 'unban';
        const url = apiBase + `/api/moderation/users/${u.id}/${action}`;

        try {
            const res = await fetch(url, { method: 'POST', headers: authHeaders(token) });

            if (isUnauthorized(res)) {
                setStatusModeration('Сессия истекла, войдите снова', 'error');
                onPage("auth");
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            setStatusModeration(u.enabled ? 'Пользователь забанен' : 'Пользователь разбанен', 'ok');
            await loadUsers();
            if (selectedUserId === u.id) loadUserDetails(u.id);
        } catch (e) {
            setStatusModeration('Ошибка изменения статуса: ' + e.message, 'error');
        }
    }

    async function saveRoles(userId, roles) {
        if (!token || !isAdmin) return;

        try {
            const res = await fetch(apiBase + '/api/admin/users/' + userId + '/roles', {
                method: 'POST',
                headers: { ...authHeaders(token), 'Content-Type': 'application/json' },
                body: JSON.stringify({ roles }),
            });

            if (isUnauthorized(res)) {
                setStatusModeration('Сессия истекла, войдите снова', 'error');
                onPage("auth");
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            setStatusModeration('Роли обновлены', 'ok');
            await loadUsers();
            if (selectedUserId === userId) loadUserDetails(userId);
        } catch (e) {
            setStatusModeration('Ошибка сохранения ролей: ' + e.message, 'error');
        }
    }

    async function resolveComplaint(id, deleteTarget, reject) {
        if (!token || !isModerator) return;

        try {
            const res = await fetch(
                apiBase + `/api/moderation/complaints/admin/${id}/resolve` +
                `?deleteTarget=${deleteTarget}&reject=${reject}`,
                {
                    method: 'POST',
                    headers: authHeaders(token),
                }
            );

            if (isUnauthorized(res)) {
                setStatusModeration('Сессия истекла, войдите снова', 'error');
                onPage("auth");
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            setStatusModeration('Жалоба обработана', 'ok');
            loadComplaints();
        } catch (e) {
            setStatusModeration('Ошибка закрытия жалобы: ' + e.message, 'error');
        }
    }

    useEffect(() => {
        if (!active) return;
        setStatusModeration('');
        if (!token || !isModerator) return;
        loadUsers();
        loadComplaints();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [active, token, isModerator]);

    if (!active) return null;

    if (!token || !isModerator) {
        return (
            <Paper sx={{ p: 2 }}>
                <Typography variant="body2" color="text.secondary">
                    Нет прав для модерации
                </Typography>
            </Paper>
        );
    }

    return (
        <>
            <Stack component={Paper} direction="row" p={1} alignItems="center">
                <IconButton onClick={() => onPage("account")} size="small">
                    <ArrowBackRoundedIcon/>
                </IconButton>
                <Typography variant="h6" noWrap px={1}>Модерация</Typography>
            </Stack>
            <Stack component={Paper} pt={2} flex={1} overflow="hidden">
                <Grid container spacing={1} mb={2} px={2}>
                    <Button
                        variant="contained"
                        color={activeTab === "users" ? "primary" : "inherit"}
                        size="small"
                        onClick={() => setActiveTab("users")}
                    >
                        Пользователи
                    </Button>
                    <Button
                        variant="contained"
                        color={activeTab === "complaints" ? "primary" : "inherit"}
                        size="small"
                        onClick={() => setActiveTab("complaints")}
                    >
                        Жалобы
                    </Button>
                    {isAdmin && (
                        <Button
                            variant="contained"
                            color={activeTab === "service" ? "primary" : "inherit"}
                            size="small"
                            onClick={() => setActiveTab("service")}
                        >
                            Сервис
                        </Button>
                    )}
                </Grid>
                {activeTab === 'users' ? (
                    <UsersList
                        users={users}
                        onDetails={loadUserDetails}
                        onVideos={loadUserVideos}
                        onToggleBan={toggleBan}
                        onPage={onPage}
                    />
                ) : activeTab === 'complaints' ? (
                    <ComplaintsList
                        complaints={complaints}
                        onResolve={resolveComplaint}
                    />
                ) : activeTab === 'service' && isAdmin && (
                    <Service onPage={onPage}/>
                )}
            </Stack>
        </>
    );
}