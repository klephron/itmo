import { useState } from "react";
import { useNavigate } from "react-router";
import { Box, Button, Stack, Typography } from "@mui/material";
import { ConfirmDialog } from "./ConfirmDialog.jsx";
import { apiBase, authHeaders, isUnauthorized } from "../api/api.js";
import { useAuth } from "../context/AuthContext.jsx";

export function Service() {
    const { token, isAdmin } = useAuth();
    const navigate = useNavigate();

    const [confirmThumbsOpen, setConfirmThumbsOpen] = useState(false);
    const [confirmEmbeddingsOpen, setConfirmEmbeddingsOpen] = useState(false);

    async function rebuildThumbs() {
        if (!token || !isAdmin) return;

        try {
            const res = await fetch(apiBase + '/api/admin/thumbnails/rebuild', {
                method: 'POST',
                headers: authHeaders(token),
            });

            if (isUnauthorized(res)) {
                //setStatusModeration('Сессия истекла, войдите снова', 'error');
                navigate("/auth");
                return;
            }

            if (!res.ok && res.status !== 202) throw new Error('HTTP ' + res.status);

            //setStatusModeration('Запущена перегенерация превью (может занять время)', 'ok');
        } catch (e) {
            //setStatusModeration('Ошибка запуска перегенерации: ' + e.message, 'error');
        }
    }

    async function rebuildEmbeddings() {
        if (!token || !isAdmin) return;

        try {
            const res = await fetch(apiBase + "/api/moderation/videos/reembed", {
                method: "POST",
                headers: authHeaders(token),
            });

            if (isUnauthorized(res)) {
                navigate("/auth");
                return;
            }

            if (!res.ok && res.status !== 202) {
                throw new Error("HTTP " + res.status);
            }
        } catch (e) {
            // опционально: статус / snackbar
        }
    }

    return (
        <>
            <Typography variant="h6" fontWeight="bold" py={0.5} px={2}>
                Служебные действия
            </Typography>
            <Stack p={2} spacing={2}>
                <Button
                    fullWidth
                    variant="contained"
                    color="warning"
                    onClick={() => setConfirmThumbsOpen(true)}
                >
                    Перегенерировать превью всех видео
                </Button>
                <Button
                    fullWidth
                    variant="contained"
                    color="error"
                    onClick={() => setConfirmEmbeddingsOpen(true)}
                >
                    Пересчитать эмбединги всех видео
                </Button>
            </Stack>
            <ConfirmDialog
                open={confirmThumbsOpen}
                setOpen={setConfirmThumbsOpen}
                title="Подтверждение операции"
                text={
                    <Stack spacing={1.5}>
                        <Typography>
                            Вы действительно хотите перегенерировать превью всех видео? Это может занять некоторое
                            время.
                        </Typography>
                        <Typography>
                            Операцию нельзя отменить.
                        </Typography>
                    </Stack>
                }
                critical
                onConfirm={rebuildThumbs}
            />
            <ConfirmDialog
                open={confirmEmbeddingsOpen}
                setOpen={setConfirmEmbeddingsOpen}
                title="Подтверждение операции"
                text={
                    <Stack spacing={1.5}>
                        <Typography>
                            Вы действительно хотите пересчитать эмбединги для всех видео? Это может занять некоторое
                            время.
                        </Typography>
                        <Typography>
                            Операцию нельзя отменить.
                        </Typography>
                    </Stack>
                }
                critical
                onConfirm={rebuildEmbeddings}
            />
        </>
    );
}