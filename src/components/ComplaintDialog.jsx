import { useEffect, useState } from 'react';
import { Button, Stack, TextField } from '@mui/material';
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
    const { token } = useAuth();

    const [reason, setReason] = useState('');
    const [status, setStatus] = useState({ msg: '', type: '' });
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (open) {
            setReason('');
            setStatus({ msg: '', type: '' });
            setLoading(false);
        }
    }, [open]);

    async function submit() {
        if (!token) {
            setStatus({ msg: 'Войдите, чтобы отправить жалобу', type: 'error' });
            return;
        }

        if (!reason.trim()) {
            setStatus({ msg: 'Укажите причину жалобы', type: 'error' });
            return;
        }

        let url;

        if (mode === 'comment') {
            if (!videoUuid || !commentId) {
                setStatus({ msg: 'Некорректные данные комментария', type: 'error' });
                return;
            }
            url = `${apiBase}/api/moderation/complaints/videos/${videoUuid}/comments/${commentId}`;
        } else {
            if (!videoUuid) {
                setStatus({ msg: 'Некорректные данные видео', type: 'error' });
                return;
            }
            url = `${apiBase}/api/moderation/complaints/videos/${videoUuid}`;
        }

        try {
            setLoading(true);
            setStatus({ msg: '', type: '' });

            const res = await fetch(url, {
                method: 'POST',
                headers: {
                    ...authHeaders(token),
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ reason }),
            });

            if (isUnauthorized(res)) {
                setStatus({ msg: 'Сессия истекла, войдите снова', type: 'error' });
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            setStatus({ msg: 'Жалоба отправлена', type: 'success' });
            onSuccess?.();
            setOpen(false);
        } catch (e) {
            setStatus({
                msg: 'Ошибка отправки жалобы: ' + e.message,
                type: 'error',
            });
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
                >
                    {loading ? 'Отправка…' : 'Отправить жалобу'}
                </Button>
            </Stack>
        </SmallDialog>
    );
};