import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { Avatar, Box, Button, Divider, Skeleton, Stack, TextField, Typography } from "@mui/material";
import SendRoundedIcon from "@mui/icons-material/SendRounded";
import DeleteForeverRoundedIcon from "@mui/icons-material/DeleteForeverRounded";
import FlagRoundedIcon from "@mui/icons-material/FlagRounded";

import { SwipeableDialog } from "./SwipeableDialog";
import { formatCompactNumber, formatDate } from "../utils/format.js";
import { useAuth } from "../context/AuthContext.jsx";
import { apiBase, authHeaders, getAvatarUrl, isUnauthorized } from "../api/api.js";
import { ComplaintDialog } from "./ComplaintDialog.jsx";
import { ConfirmDialog } from "./ConfirmDialog.jsx";
import { LoadMoreButton } from "./LoadMoreButton.jsx";

const PAGE_SIZE = 20;

export function CommentsDialog({ open, setOpen, videoUuid, setTrigger }) {
    const { token, user } = useAuth();
    const navigate = useNavigate();

    const [comments, setComments] = useState([]);
    const [paging, setPaging] = useState({
        page: 0,
        size: PAGE_SIZE,
        last: true,
        totalElements: 0,
    });
    const [loading, setLoading] = useState(false);

    const [status, setStatus] = useState({ msg: "", type: "" });
    const [text, setText] = useState("");

    const [complaintDialogOpen, setComplaintDialogOpen] = useState(false);
    const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);
    const [commentId, setCommentId] = useState(null);

    async function loadPage(pageNumber, { replace = false } = {}) {
        if (loading) return;
        setLoading(true);

        try {
            const res = await fetch(
                `${apiBase}/api/videos/${videoUuid}/comments?page=${pageNumber}&size=${PAGE_SIZE}`,
                { headers: authHeaders(token) }
            );

            if (!res.ok) throw new Error("HTTP " + res.status);

            const page = await res.json();
            const list = page?.content ?? [];

            setComments(prev => (replace ? list : prev.concat(list)));
            setPaging({
                page: page.number,
                size: PAGE_SIZE,
                last: !!page.last,
                totalElements: page.totalElements ?? 0,
            });
        } finally {
            setLoading(false);
        }
    }

    async function reloadUpToCurrentPage() {
        const pagesToLoad = paging.page + 1;

        setComments([]);
        setPaging(p => ({ ...p, page: 0, last: false }));

        for (let p = 0; p < pagesToLoad; p++) {
            await loadPage(p, { replace: p === 0 });
        }
    }

    async function send() {
        if (!token) {
            setStatus({ msg: "Войдите, чтобы комментировать", type: "error" });
            navigate("/auth");
            return;
        }

        const trimmed = text.trim();
        if (!trimmed) {
            setStatus({ msg: "Введите текст комментария", type: "error" });
            return;
        }

        try {
            const res = await fetch(
                `${apiBase}/api/videos/${videoUuid}/comments`,
                {
                    method: "POST",
                    headers: {
                        ...authHeaders(token),
                        "Content-Type": "application/json",
                    },
                    body: JSON.stringify({ text: trimmed }),
                }
            );

            if (isUnauthorized(res)) {
                setStatus({
                    msg: "Сессия истекла, войдите снова",
                    type: "error",
                });
                navigate("/auth");
                return;
            }

            if (!res.ok) throw new Error("HTTP " + res.status);

            await reloadUpToCurrentPage();
            setText("");
            setStatus({ msg: "Комментарий отправлен", type: "ok" });
        } catch (e) {
            setStatus({ msg: "Ошибка отправки: " + e.message, type: "error" });
        }
    }

    async function deleteComment(id) {
        if (!token) {
            setStatus({
                msg: "Войдите, чтобы удалять комментарии",
                type: "error",
            });
            navigate("/auth");
            return;
        }

        try {
            const res = await fetch(
                `${apiBase}/api/videos/${videoUuid}/comments/${id}`,
                {
                    method: "DELETE",
                    headers: authHeaders(token),
                }
            );

            if (isUnauthorized(res)) {
                setStatus({
                    msg: "Сессия истекла, войдите снова",
                    type: "error",
                });
                navigate("/auth");
                return;
            }

            if (!res.ok && res.status !== 204)
                throw new Error("HTTP " + res.status);

            await reloadUpToCurrentPage();
            setStatus({ msg: "Комментарий удалён", type: "ok" });
        } catch (e) {
            setStatus({ msg: "Ошибка удаления: " + e.message, type: "error" });
        }
    }

    function resetState() {
        setComments([]);
        setPaging({
            page: 0,
            size: PAGE_SIZE,
            last: true,
            totalElements: 0,
        });
        setStatus({ msg: "", type: "" });
        setText("");
        setCommentId(null);
        setComplaintDialogOpen(false);
        setConfirmDialogOpen(false);
    }

    useEffect(() => {
        if (!open || !videoUuid) return;

        loadPage(0, { replace: true });
    }, [open, videoUuid]);

    useEffect(() => {
        if (open) return;

        setTrigger((t) => t + 1);

        const t = setTimeout(() => {
            resetState();
        }, 250); // 200–300мс — оптимально под MUI dialog

        return () => {
            clearTimeout(t);
        };
    }, [open]);

    const handleKeyDown = (e) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            //e.target.form?.requestSubmit();
            send();
        }
    }

    const handleLoadMore = () => loadPage(paging.page + 1);
    const handleConfirm = () => deleteComment(commentId);

    if (!videoUuid) return null;

    return (
        <SwipeableDialog
            open={open}
            setOpen={setOpen}
            title={
                <Stack direction="row" spacing={2}>
                    <span>Комментарии</span>
                    <Typography component="span" variant="body2" color="textSecondary" pt={1}>
                        {loading && !paging.totalElements ? (
                            <Skeleton
                                width={24}
                                animation="wave"
                            />
                        ) : (
                            formatCompactNumber(paging.totalElements)
                        )}
                    </Typography>
                </Stack>
            }
        >
            <Stack
                px={2}
                spacing={1}
                sx={{
                    flex: 1,
                    overflowX: 'hidden',
                    overflowY: 'auto',
                }}
            >
                {loading && !comments.length ? Array.from({ length: 3 }, (_, index) => (
                    <Stack
                        key={index}
                        direction="row"
                        spacing={1.5}
                        alignItems="flex-start"
                        pt={index === 0 ? 1 : 0}
                    >
                        <Skeleton
                            variant="circular"
                            width={32}
                            height={32}
                            animation="wave"
                        />
                        <Stack spacing={0.5} flex={1}>
                            <Typography variant="caption" sx={{ position: 'relative', top: -2 }}>
                                <Skeleton width="30%" animation="wave"/>
                            </Typography>
                            <Typography variant="body2">
                                <Skeleton width="100%" animation="wave"/>
                            </Typography>
                        </Stack>
                    </Stack>
                )) : !comments.length ? (
                    <Typography>
                        Комментариев пока нет
                    </Typography>
                ) : comments.map((c, index) => (
                    <Stack
                        key={c.id}
                        direction="row"
                        spacing={1.5}
                        alignItems="flex-start"
                        pt={index === 0 ? 1 : 0}
                    >
                        <Avatar
                            src={getAvatarUrl(c?.authorId)}
                            sx={{ width: 32, height: 32, cursor: 'pointer' }}
                            onClick={() => navigate(`/profile/${c.authorId}`)}
                        >
                            {c.authorName?.[0]?.toUpperCase()}
                        </Avatar>
                        <Stack
                            spacing={0.5}
                            flex={1}
                            sx={{
                                position: 'relative',
                                top: user ? -8 : -2,
                                overflow: 'hidden',
                            }}
                        >
                            <Stack
                                direction="row"
                                spacing={1}
                                alignItems={"flex-end"}
                                flexWrap="wrap"
                            >
                                <Typography
                                    variant="subtitle2"
                                    sx={{ cursor: 'pointer' }}
                                    onClick={() => navigate(`/profile/${c.authorId}`)}
                                >
                                    @{c.authorName}
                                </Typography>
                                <Typography variant="caption" color="text.secondary">
                                    {formatDate(c.createdAt)}
                                </Typography>
                                {(c?.mine || user?.roles?.some((r) => r === 'ADMIN' || r === 'MODERATOR')) && (
                                    <Button
                                        size="uniSmall"
                                        color="error"
                                        onClick={() => {
                                            setCommentId(c?.id);
                                            setConfirmDialogOpen(true);
                                        }}
                                        sx={{
                                            position: 'relative',
                                            top: 2,
                                        }}
                                    >
                                        <DeleteForeverRoundedIcon sx={{ fontSize: 20 }}/>
                                    </Button>
                                )}
                                {user && !c?.mine && (
                                    <Button
                                        size="uniSmall"
                                        onClick={() => {
                                            setCommentId(c?.id);
                                            setComplaintDialogOpen(true);
                                        }}
                                        sx={{
                                            position: 'relative',
                                            top: 2,
                                        }}
                                    >
                                        <FlagRoundedIcon sx={{ fontSize: 20 }}/>
                                    </Button>
                                )}
                            </Stack>
                            <Typography
                                variant="body2"
                                sx={{
                                    whiteSpace: 'pre-wrap',      // сохраняем переносы строк + разрешаем wrap
                                    overflowWrap: 'break-word',  // перенос по словам, если можно
                                    wordBreak: 'break-word',     // перенос по буквам, если слово слишком длинное
                                }}
                            >
                                {c.text}
                            </Typography>
                        </Stack>
                    </Stack>
                ))}
            </Stack>
            <Divider/>
            <Box p={2} position="relative">
                {!paging.last && (
                    <LoadMoreButton
                        sx={(theme) => ({
                            position: 'absolute',
                            top: 0,
                            left: "50%",
                            transform: `translate(-50%, calc(-100% - 1px - ${theme.spacing(1)}))`,
                        })}
                        onClick={handleLoadMore}
                        loading={loading}
                    />
                )}
                {user ? (
                    <Stack direction="row" spacing={2} alignItems="flex-end">
                        <TextField
                            maxRows={5}
                            multiline
                            fullWidth
                            size="small"
                            placeholder={user ? "Напишите комментарий…" : "Войдите в аккаунт, чтобы оставлять комментарии"}
                            value={text}
                            onChange={(e) => setText(e.target.value)}
                            onKeyDown={handleKeyDown}
                        />
                        <Button
                            variant="contained"
                            onClick={send}
                            sx={{ py: 1 }}
                            disabled={!text?.trim()}
                        >
                            <SendRoundedIcon/>
                        </Button>
                    </Stack>
                ) : (
                    <Typography
                        variant="body2"
                        color="text.secondary"
                        textAlign="center"
                    >
                        Войдите в аккаунт, чтобы оставлять комментарии
                    </Typography>
                )}
            </Box>
            <ComplaintDialog
                open={complaintDialogOpen}
                setOpen={setComplaintDialogOpen}
                mode="comment"
                videoUuid={videoUuid}
                commentId={commentId}
            />
            <ConfirmDialog
                open={confirmDialogOpen}
                setOpen={setConfirmDialogOpen}
                title="Подтверждение удаления"
                text="Вы уверены, что хотите безвозвратно удалить комментарий?"
                critical
                onConfirm={handleConfirm}
            />
        </SwipeableDialog>
    );
}