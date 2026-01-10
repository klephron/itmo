import { useEffect, useState } from "react";
import { Avatar, Box, Button, Divider, Skeleton, Stack, TextField, Typography } from "@mui/material";
import SendRoundedIcon from "@mui/icons-material/SendRounded";
import DeleteForeverRoundedIcon from "@mui/icons-material/DeleteForeverRounded";
import FlagRoundedIcon from "@mui/icons-material/FlagRounded";

import { SwipeableDialog } from "./SwipeableDialog";
import { formatCompactNumber, formatDate } from "../utils/format.js";
import { useAuth } from "../context/AuthContext.jsx";
import { apiBase, authHeaders, isUnauthorized } from "../api/api.js";
import { ComplaintDialog } from "./ComplaintDialog.jsx";
import { ConfirmDialog } from "./ConfirmDialog.jsx";
import { LoadMoreButton } from "./LoadMoreButton.jsx";

const PAGE_SIZE = 20;

export function CommentsDialog({ open, setOpen, videoUuid, onPage }) {
    const { token, user } = useAuth();

    const [comments, setComments] = useState([]);
    const [page, setPage] = useState(0);
    const [hasNext, setHasNext] = useState(false);
    const [totalElements, setTotalElements] = useState(0);
    const [loading, setLoading] = useState(false);

    const [status, setStatus] = useState({ msg: "", type: "" });
    const [text, setText] = useState("");

    const [complaintDialogOpen, setComplaintDialogOpen] = useState(false);
    const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);
    const [commentId, setCommentId] = useState(null);

    const loadPage = async (pageToLoad) => {
        if (!videoUuid || loading) return;

        setLoading(true);
        setStatus({ msg: "Загрузка комментариев…", type: "" });

        try {
            const res = await fetch(
                `${apiBase}/api/videos/${videoUuid}/comments?page=${pageToLoad}&size=${PAGE_SIZE}`,
                { headers: authHeaders(token) }
            );

            if (!res.ok) throw new Error("HTTP " + res.status);

            const data = await res.json();

            setComments((prev) => [...prev, ...data.content]);
            setHasNext(!data.last);
            setTotalElements(data.totalElements);
            setPage(pageToLoad);
            setStatus({ msg: "", type: "" });
        } catch (e) {
            setStatus({
                msg: "Ошибка загрузки комментариев: " + e.message,
                type: "error"
            });
        } finally {
            setLoading(false);
        }
    }

    async function send() {
        if (!token) {
            setStatus({ msg: "Войдите, чтобы комментировать", type: "error" });
            onPage("auth");
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
                onPage("auth");
                return;
            }

            if (!res.ok) throw new Error("HTTP " + res.status);

            const newComment = await res.json();
            setComments((prev) => [...prev, newComment]);
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
            onPage("auth");
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
                onPage("auth");
                return;
            }

            if (!res.ok && res.status !== 204)
                throw new Error("HTTP " + res.status);

            setComments((prev) => prev.filter((c) => c.id !== id));
            setStatus({ msg: "Комментарий удалён", type: "ok" });
        } catch (e) {
            setStatus({ msg: "Ошибка удаления: " + e.message, type: "error" });
        }
    }

    useEffect(() => {
        if (!open || !videoUuid) return;

        setComments([]);
        setPage(0);
        setHasNext(false);
        setTotalElements(0);
        setStatus({ msg: "", type: "" });

        loadPage(0);
    }, [open, videoUuid]);

    const handleKeyDown = (e) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            //e.target.form?.requestSubmit();
            send();
        }
    }

    const handleLoadMore = () => loadPage(page + 1);
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
                        {loading && !totalElements ? (
                            <Skeleton
                                width={24}
                                animation="wave"
                            />
                        ) : (
                            formatCompactNumber(totalElements)
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
                            src={c.authorAvatarUrl ? `${apiBase}${c.authorAvatarUrl}` : null}
                            sx={{ width: 32, height: 32, cursor: 'pointer' }}
                            onClick={() => c.authorId && onPage("profile", { profileUserId: c.authorId })}
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
                                    onClick={() => c.authorId && onPage("profile", { profileUserId: c.authorId })}
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
                {hasNext && (
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