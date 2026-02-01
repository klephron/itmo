import { useCallback, useEffect, useState, useTransition } from "react";
import { useNavigate } from "react-router";
import { Box, Skeleton, Stack, Typography } from "@mui/material";
import { useSnackbar } from "notistack";

import { apiBase, authHeaders, isUnauthorized } from "../api/api.js";
import { useAuth } from "../context/AuthContext.jsx";
import { ComplaintCard } from "./ComplaintCard.jsx";

export function ComplaintsList() {
    const { token, isModerator, setAuth } = useAuth();
    const { enqueueSnackbar } = useSnackbar();
    const navigate = useNavigate();

    const [complaints, setComplaints] = useState([]);
    const [isPending, startTransition] = useTransition();

    const loadComplaints = useCallback(async () => {
        if (!token || !isModerator) return;

        try {
            const res = await fetch(
                apiBase + "/api/moderation/complaints/admin",
                { headers: authHeaders(token) }
            );

            if (isUnauthorized(res)) {
                enqueueSnackbar("Сессия истекла, войдите снова", { variant: "error" });
                navigate("/auth");
                setAuth(null, null);
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            const list = await res.json();

            startTransition(() => {
                setComplaints(Array.isArray(list) ? list : []);
            });
        } catch (e) {
            enqueueSnackbar("Ошибка загрузки жалоб: " + e.message, { variant: "error" });
        }
    }, [token, isModerator, navigate, enqueueSnackbar, setAuth]);

    useEffect(() => {
        loadComplaints();
    }, [loadComplaints]);

    return (
        <>
            <Stack direction="row" spacing={1} py={0.5} px={2}>
                <Typography variant="h6" fontWeight="bold">
                    Жалобы
                </Typography>
                <Typography variant="body2" color="text.secondary" pt={1}>
                    {isPending ? <Skeleton width={24} animation="wave"/> : complaints.length}
                </Typography>
            </Stack>
            {!complaints.length && !isPending ? (
                <Typography mt={1.5} px={2}>
                    Нет открытых жалоб
                </Typography>
            ) : (
                <Box sx={{ borderRadius: 4, flex: 1, overflow: "hidden", m: 2 }}>
                    <Stack spacing={1.5} sx={{ height: 1, overflowX: "hidden", overflowY: "auto" }}>
                        {complaints.map((c) => (
                            <ComplaintCard
                                key={c.id}
                                id={c.id}
                                createdAt={c.createdAt}
                                authorId={c.authorId}
                                targetType={c.targetType}
                                videoUuid={c.videoUuid}
                                commentId={c.commentId}
                                reason={c.reason}
                                loadComplaints={loadComplaints}
                            />
                        ))}
                    </Stack>
                </Box>
            )}
        </>
    );
}