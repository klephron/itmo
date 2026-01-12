import { useEffect, useState } from "react";
import { Avatar, Box, Button, Card, CardActionArea, Grid, Stack, Typography } from "@mui/material";
import ScheduleRoundedIcon from "@mui/icons-material/ScheduleRounded";
import PersonOutlinedIcon from "@mui/icons-material/PersonOutlined";

import { formatDate, formatLikes, formatViews } from "../utils/format.js";
import { apiBase, authHeaders, isUnauthorized } from "../api/api.js";
import { useAuth } from "../context/AuthContext.jsx";

const CommentCard = ({ user, createdAt, text }) => (
    <Card
        variant="outlined"
        sx={(theme) => ({
            borderRadius: 3,
            backgroundImage: theme.vars.overlays[1]
        })}
    >
        <CardActionArea component="div">
            <Stack direction="row" spacing={1} p={1}>
                <Avatar
                    src={user?.authorAvatarUrl ? `${apiBase}${user?.authorAvatarUrl}` : null}
                    sx={{ width: 32, height: 32, cursor: 'pointer' }}
                    onClick={() => user?.authorId && onPage("profile", { profileUserId: user?.authorId })}
                >
                    {user?.authorName?.[0]?.toUpperCase()}
                </Avatar>
                <Stack spacing={1} flex={1} pt={0.25}>
                    <Stack direction="row" spacing={1}>
                        <Typography variant="caption" color="text.secondary" lineHeight={1}>
                            <Typography
                                component="span"
                                variant="body2"
                                fontWeight="medium"
                                color="textPrimary"
                                lineHeight={1}
                            >
                                {user?.name ?? "User"}
                            </Typography>
                            {` · ${formatDate(createdAt)}`}
                        </Typography>
                    </Stack>
                    <Typography
                        variant="body2"
                        sx={{
                            whiteSpace: 'pre-wrap',      // сохраняем переносы строк + разрешаем wrap
                            overflowWrap: 'break-word',  // перенос по словам, если можно
                            wordBreak: 'break-word',     // перенос по буквам, если слово слишком длинное
                        }}
                    >
                        {text}
                    </Typography>
                </Stack>
            </Stack>
        </CardActionArea>
    </Card>
);

const VideoCard = ({ video }) => (
    <Card
        variant="outlined"
        sx={(theme) => ({
            borderRadius: 3,
            backgroundImage: theme.vars.overlays[1]
        })}
    >
        <CardActionArea component="div">
            <Stack spacing={0.5} py={1} px={1.5}>
                <Typography
                    variant="body2"
                    sx={{
                        whiteSpace: 'pre-wrap',      // сохраняем переносы строк + разрешаем wrap
                        overflowWrap: 'break-word',  // перенос по словам, если можно
                        wordBreak: 'break-word',     // перенос по буквам, если слово слишком длинное
                    }}
                >
                    {video?.name || "Видео 1 бла бла бла бла"}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                    <Typography
                        variant="caption"
                        //fontWeight="medium"
                        lineHeight={1}
                    >
                        {video?.user?.name ?? "User"}
                    </Typography>
                    {` · ${video?.viewsCount ?? 100} ${formatViews(video?.viewsCount ?? 10)}`}
                    {` · ${video?.likesCount ?? 10} ${formatLikes(video?.likesCount ?? 10)}`}
                    {` · 12.01.2026, 03:53:34`}
                </Typography>
            </Stack>
        </CardActionArea>
    </Card>
);

export function ComplaintsList({ onPage }) {
    const { token, isModerator } = useAuth();

    const [complaints, setComplaints] = useState([]);
    const [loading, setLoading] = useState(false);

    async function loadComplaints() {
        if (!token || !isModerator) return;

        try {
            const res = await fetch(apiBase + '/api/moderation/complaints/admin', {
                headers: authHeaders(token),
            });

            if (isUnauthorized(res)) {
                //setStatusModeration('Сессия истекла, войдите снова', 'error');
                onPage("auth");
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            const list = await res.json();
            setComplaints(Array.isArray(list) ? list : []);
        } catch (e) {
            //setStatusModeration('Ошибка загрузки жалоб: ' + e.message, 'error');
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
                //setStatusModeration('Сессия истекла, войдите снова', 'error');
                onPage("auth");
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            //setStatusModeration('Жалоба обработана', 'ok');
            loadComplaints();
        } catch (e) {
            //setStatusModeration('Ошибка закрытия жалобы: ' + e.message, 'error');
        }
    }

    useEffect(() => {
        loadComplaints();
    }, [token, isModerator]);

    return (
        <>
            <Stack direction="row" spacing={1} py={0.5} px={2}>
                <Typography variant="h6" fontWeight="bold">
                    Жалобы
                </Typography>
                <Typography variant="body2" color="text.secondary" pt={1}>
                    {complaints.length}
                </Typography>
            </Stack>

            {!complaints.length ? (
                <Typography mt={1.5} px={2}>
                    Нет открытых жалоб
                </Typography>
            ) : (
                <Box sx={{ borderRadius: 4, flex: 1, overflow: "hidden", m: 2 }}>
                    <Stack spacing={1.5} sx={{ height: 1, overflowX: "hidden", overflowY: "auto" }}>
                        {complaints.map(c => (
                            <Card key={c.id} elevation={1} sx={{ flexShrink: 0, p: 2 }}>
                                <Grid container columnSpacing={2} rowSpacing={1}>
                                    <Stack direction="row" spacing={0.5} alignItems="center">
                                        <ScheduleRoundedIcon
                                            sx={{ fontSize: "inherit", color: "text.secondary" }}
                                        />
                                        <Typography variant="caption" lineHeight={1}>
                                            {formatDate(c?.createdAt)}
                                        </Typography>
                                    </Stack>
                                    <Stack direction="row" spacing={0.5} alignItems="center">
                                        <PersonOutlinedIcon
                                            sx={{ fontSize: "inherit", color: "text.secondary" }}
                                        />
                                        <Typography variant="caption" lineHeight={1}>
                                            Username
                                        </Typography>
                                    </Stack>
                                </Grid>
                                <Box mt={3}>
                                    <Typography variant="body1" fontWeight="bold" mb={1.5}>
                                        На что пожаловались
                                    </Typography>
                                    {c.targetType === "VIDEO" ? (
                                        <VideoCard video={{ name: c?.videoUuid }}
                                        />
                                    ) : (
                                        <CommentCard
                                            createdAt={c?.createdAt}
                                            text={c?.commentId}
                                        />
                                    )}
                                </Box>
                                <Box mt={3}>
                                    <Typography variant="body1" fontWeight="bold" mb={1}>
                                        Текст жалобы
                                    </Typography>
                                    <Typography
                                        variant="body2"
                                        sx={{
                                            whiteSpace: 'pre-wrap',
                                            overflowWrap: 'break-word',
                                            wordBreak: 'break-word',
                                        }}
                                    >
                                        {c?.reason}
                                    </Typography>
                                </Box>
                                <Grid container spacing={1} mt={3}>
                                    <Button
                                        variant="contained"
                                        color="inherit"
                                        onClick={() => resolveComplaint(c.id, false, true)}
                                    >
                                        Отклонить
                                    </Button>
                                    <Button
                                        variant="contained"
                                        onClick={() => resolveComplaint(c.id, false, false)}
                                    >
                                        Принять
                                    </Button>
                                    <Button
                                        variant="contained"
                                        color="error"
                                        onClick={() => resolveComplaint(c.id, true, false)}
                                    >
                                        Удалить контент
                                    </Button>
                                </Grid>
                            </Card>
                        ))}
                    </Stack>
                </Box>
            )}
        </>
    );
}