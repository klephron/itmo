import { useState } from "react";
import { Box, Button, Stack, Typography } from "@mui/material";
import { ConfirmDialog } from "./ConfirmDialog.jsx";
import { apiBase, authHeaders, isUnauthorized } from "../api/api.js";
import { useAuth } from "../context/AuthContext.jsx";

export function Service({ onPage }) {
    const { token, isAdmin } = useAuth();

    const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);

    async function rebuildThumbs() {
        if (!token || !isAdmin) return;

        try {
            const res = await fetch(apiBase + '/api/admin/thumbnails/rebuild', {
                method: 'POST',
                headers: authHeaders(token),
            });

            if (isUnauthorized(res)) {
                //setStatusModeration('Сессия истекла, войдите снова', 'error');
                onPage("auth");
                return;
            }

            if (!res.ok && res.status !== 202) throw new Error('HTTP ' + res.status);

            //setStatusModeration('Запущена перегенерация превью (может занять время)', 'ok');
        } catch (e) {
            //setStatusModeration('Ошибка запуска перегенерации: ' + e.message, 'error');
        }
    }

    return (
        <>
            <Typography variant="h6" fontWeight="bold" py={0.5} px={2}>
                Служебные действия
            </Typography>
            <Box p={2}>
                <Button
                    fullWidth
                    variant="contained"
                    color="warning"
                    onClick={() => setConfirmDialogOpen(true)}
                >
                    Перегенерировать превью всех видео
                </Button>
            </Box>
            <ConfirmDialog
                open={confirmDialogOpen}
                setOpen={setConfirmDialogOpen}
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
        </>
    );
}