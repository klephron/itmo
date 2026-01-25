import { useEffect, useState } from 'react';
import { useNavigate } from "react-router";
import { Button, Stack, TextField } from '@mui/material';
import { useSnackbar } from "notistack";

import { SmallDialog } from './SmallDialog.jsx';
import { apiBase, authHeaders, isUnauthorized } from '../api/api';
import { useAuth } from '../context/AuthContext';

export const ComplaintDialog = ({
                                    open,
                                    setOpen,
                                    mode,           // 'video' | 'comment'
                                    videoUuid,
                                    commentId,
                                    onSuccess,
                                }) => {
    const { token, setAuth } = useAuth();
    const { enqueueSnackbar } = useSnackbar();
    const navigate = useNavigate();

    const [reason, setReason] = useState('');
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (open) {
            setReason('');
            setLoading(false);
        }
    }, [open]);

    async function submit() {
        let url;
        if (mode === 'comment') {
            url = `${apiBase}/api/moderation/complaints/videos/${videoUuid}/comments/${commentId}`;
        } else {
            url = `${apiBase}/api/moderation/complaints/videos/${videoUuid}`;
        }

        try {
            setLoading(true);

            const res = await fetch(url, {
                method: 'POST',
                headers: {
                    ...authHeaders(token),
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ reason }),
            });

            if (isUnauthorized(res)) {
                enqueueSnackbar("Сессия истекла, войдите снова", { variant: "error" });
                navigate("/auth");
                setAuth(null, null);
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            enqueueSnackbar("Жалоба отправлена", { variant: "success" });
            onSuccess?.();
            setOpen(false);
        } catch (e) {
            enqueueSnackbar("Ошибка отправки жалобы: " + e.message, { variant: "error" });
        } finally {
            setLoading(false);
        }
    }

    return (
        <SmallDialog
            open={open}
            setOpen={setOpen}
            title={mode === 'comment' ? 'Пожаловаться на комментарий' : 'Пожаловаться на видео'}
        >
            <Stack p={2} pt="1px" spacing={2} sx={{ overflowX: "hidden", overflowY: "auto" }}>
                <TextField
                    size="small"
                    placeholder="Подробно опишите причину"
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    fullWidth
                    multiline
                    minRows={3}
                    disabled={loading}
                />
                <Button
                    variant="contained"
                    color="error"
                    fullWidth
                    disabled={!reason.trim()}
                    onClick={submit}
                    loading={loading}
                >
                    Отправить жалобу
                </Button>
            </Stack>
        </SmallDialog>
    );
};