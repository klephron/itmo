import { useEffect, useMemo, useState } from "react";
import { Avatar, Button, Divider, Stack, TextField, Typography } from "@mui/material";
import SendRoundedIcon from "@mui/icons-material/SendRounded";
import DeleteForeverRoundedIcon from "@mui/icons-material/DeleteForeverRounded";

import { SwipeableDialog } from "./SwipeableDialog";
import { formatCompactNumber, formatDate } from "../utils/format.js";
import { useAuth } from "../context/AuthContext.jsx";
import { apiBase, authHeaders, isUnauthorized } from "../api/api.js";

export function CommentsDialog({ open, setOpen, videoUuid, onOpenProfile, onRequireAuth }) {
    const { token, user } = useAuth();
    const [cache] = useState(() => new Map());
    const [list, setList] = useState([]);
    const [status, setStatus] = useState({ msg: '', type: '' });
    const [text, setText] = useState('');

    const cached = useMemo(() => (videoUuid ? cache.get(videoUuid) : null), [cache, videoUuid]);

    useEffect(() => {
        if (!videoUuid) return;
        if (cached) setList(cached);

        let aborted = false;

        async function load() {
            setStatus({ msg: 'Загрузка комментариев…', type: '' });
            try {
                const res = await fetch(apiBase + '/api/videos/' + videoUuid + '/comments', {
                    headers: authHeaders(token),
                });
                if (!res.ok) throw new Error('HTTP ' + res.status);
                const data = await res.json();
                if (aborted) return;
                cache.set(videoUuid, data);
                setList(data);
                setStatus({ msg: '', type: '' });
            } catch (e) {
                if (aborted) return;
                setStatus({ msg: 'Ошибка загрузки комментариев: ' + e.message, type: 'error' });
            }
        }

        load();
        return () => {
            aborted = true;
        };
    }, [videoUuid, token, cache, cached]);

    async function send() {
        if (!token) {
            setStatus({ msg: 'Войдите, чтобы комментировать', type: 'error' });
            onRequireAuth?.();
            return;
        }
        const trimmed = text.trim();
        if (!trimmed) {
            setStatus({ msg: 'Введите текст комментария', type: 'error' });
            return;
        }

        try {
            const res = await fetch(apiBase + '/api/videos/' + videoUuid + '/comments', {
                method: 'POST',
                headers: { ...authHeaders(token), 'Content-Type': 'application/json' },
                body: JSON.stringify({ text: trimmed }),
            });

            if (isUnauthorized(res)) {
                setStatus({ msg: 'Сессия истекла, войдите снова', type: 'error' });
                onRequireAuth?.();
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            const newComment = await res.json();
            const next = [...(cache.get(videoUuid) || []), newComment];
            cache.set(videoUuid, next);
            setList(next);
            setText('');
            setStatus({ msg: 'Комментарий отправлен', type: 'ok' });
        } catch (e) {
            setStatus({ msg: 'Ошибка отправки: ' + e.message, type: 'error' });
        }
    }

    async function deleteComment(commentId) {
        if (!token) {
            setStatus({ msg: 'Войдите, чтобы удалять комментарии', type: 'error' });
            onRequireAuth?.();
            return;
        }
        if (!confirm('Удалить комментарий?')) return;

        try {
            const res = await fetch(apiBase + `/api/videos/${videoUuid}/comments/${commentId}`, {
                method: 'DELETE',
                headers: authHeaders(token),
            });

            if (isUnauthorized(res)) {
                setStatus({ msg: 'Сессия истекла, войдите снова', type: 'error' });
                onRequireAuth?.();
                return;
            }

            if (!res.ok && res.status !== 204) throw new Error('HTTP ' + res.status);

            const next = (cache.get(videoUuid) || []).filter((c) => c.id !== commentId);
            cache.set(videoUuid, next);
            setList(next);
            setStatus({ msg: 'Комментарий удалён', type: 'ok' });
        } catch (e) {
            setStatus({ msg: 'Ошибка удаления: ' + e.message, type: 'error' });
        }
    }

    const handleKeyDown = (e) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            //e.target.form?.requestSubmit();
            send();
        }
    }

    if (!videoUuid) return null;

    return (
        <SwipeableDialog
            open={open}
            setOpen={setOpen}
            title={
                <Stack direction="row" spacing={2}>
                    <span>Комментарии</span>
                    <Typography component="span" variant="body2" color="textSecondary" pt={1}>
                        {formatCompactNumber(list?.length)}
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
                {/* Список комментариев */}
                {!list || list.length === 0 ? (
                    <Typography>
                        Комментариев пока нет
                    </Typography>
                ) : list.map((c, index) => (
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
                            onClick={() =>
                                c.authorId && onOpenProfile?.(c.authorId)
                            }
                        >
                            {c.authorName?.[0]?.toUpperCase()}
                        </Avatar>
                        <Stack
                            spacing={0.5}
                            flex={1}
                            sx={{
                                position: 'relative',
                                top: -8,
                                overflow: 'hidden',
                            }}
                        >
                            <Stack
                                direction="row"
                                spacing={1}
                                alignItems="flex-end"
                                flexWrap="wrap"
                            >
                                <Typography
                                    variant="subtitle2"
                                    sx={{ cursor: 'pointer' }}
                                    onClick={() =>
                                        c.authorId && onOpenProfile?.(c.authorId)
                                    }
                                >
                                    {c.authorName}
                                </Typography>
                                <Typography variant="caption" color="text.secondary">
                                    {formatDate(c.createdAt)}
                                </Typography>
                                {(c?.mine || user?.roles?.some((r) => r === 'ADMIN' || r === 'MODERATOR')) && (
                                    <Button
                                        size="uniSmall"
                                        color="error"
                                        onClick={() => deleteComment(c.id)}
                                        sx={{
                                            position: 'relative',
                                            top: 2,
                                        }}
                                    >
                                        <DeleteForeverRoundedIcon sx={{ fontSize: 20 }}/>
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
            <Stack spacing={1} p={2}>
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
            </Stack>
        </SwipeableDialog>
    );
}