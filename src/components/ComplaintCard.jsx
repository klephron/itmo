import { memo, useEffect, useState } from "react";
import { useNavigate } from "react-router";

import { Box, Button, Card, Grid, Stack, Typography } from "@mui/material";
import ScheduleRoundedIcon from "@mui/icons-material/ScheduleRounded";
import PersonOutlinedIcon from "@mui/icons-material/PersonOutlined";

import { formatDate } from "../utils/format.js";
import { apiBase, authHeaders, isUnauthorized } from "../api/api.js";
import { CommentCard } from "./CommentCard.jsx";
import { VideoCard } from "./VideoCard.jsx";
import { useAuth } from "../context/AuthContext.jsx";

export const ComplaintCard = memo(({
                                       id,
                                       createdAt,
                                       authorId,
                                       targetType,
                                       videoUuid,
                                       commentId,
                                       reason,
                                       loadComplaints
                                   }) => {
    const { token, isModerator } = useAuth();
    const navigate = useNavigate();

    const [video, setVideo] = useState(null);
    const [comment, setComment] = useState(null);
    const [author, setAuthor] = useState(null);

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
                navigate("/auth");
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
        let alive = true;

        if (targetType === "VIDEO") {
            fetch(`${apiBase}/api/streaming/video/metadata/${videoUuid}`)
                .then(r => r.ok ? r.json() : null)
                .then(d => alive && setVideo(d));
        } else {
            fetch(`${apiBase}/api/videos/${videoUuid}/comments/${commentId}`)
                .then(r => r.ok ? r.json() : null)
                .then(d => alive && setComment(d));
        }
        return () => {
            alive = false
        };
    }, [targetType, videoUuid, commentId]);

    useEffect(() => {
        let alive = true;

        if (!authorId) return;

        fetch(`${apiBase}/api/moderation/users/${authorId}`, { headers: authHeaders(token) })
            .then(r => r.ok ? r.json() : null)
            .then(d => alive && setAuthor(d));

        return () => {
            alive = false;
        };
    }, [authorId]);

    const handleOpenTarget = () => {
        navigate("/feed", { state: { feedMode: { type: "moderation", targetUuid: videoUuid } } });
    };

    return (
        <Card elevation={1} sx={{ flexShrink: 0, p: 2 }}>
            <Grid container columnSpacing={2} rowSpacing={1}>
                <Stack direction="row" spacing={0.5} alignItems="center">
                    <ScheduleRoundedIcon
                        sx={{ fontSize: "inherit", color: "text.secondary" }}
                    />
                    <Typography variant="caption" lineHeight={1}>
                        {formatDate(createdAt)}
                    </Typography>
                </Stack>
                <Stack direction="row" spacing={0.5} alignItems="center">
                    <PersonOutlinedIcon
                        sx={{ fontSize: "inherit", color: "text.secondary" }}
                    />
                    <Typography
                        variant="caption"
                        lineHeight={1}
                        sx={{ cursor: "pointer" }}
                        onClick={() => navigate(`/profile/${authorId}`)}
                    >
                        {author?.displayName ?? "-"}
                    </Typography>
                </Stack>
            </Grid>
            <Box mt={3}>
                <Typography variant="body1" fontWeight="bold" mb={1.5}>
                    На что пожаловались
                </Typography>
                {targetType === "VIDEO" && (
                    <VideoCard video={video} onClick={handleOpenTarget}/>
                )}
                {targetType === "COMMENT" && (
                    <CommentCard comment={comment} onClick={handleOpenTarget}/>
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
                    {reason}
                </Typography>
            </Box>
            <Grid container spacing={1} mt={3}>
                <Button
                    variant="contained"
                    color="inherit"
                    onClick={() => resolveComplaint(id, false, true)}
                >
                    Отклонить
                </Button>
                <Button
                    variant="contained"
                    onClick={() => resolveComplaint(id, false, false)}
                >
                    Принять
                </Button>
                <Button
                    variant="contained"
                    color="error"
                    onClick={() => resolveComplaint(id, true, false)}
                >
                    Удалить контент
                </Button>
            </Grid>
        </Card>
    );
});