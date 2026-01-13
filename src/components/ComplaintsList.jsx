import { useCallback, useEffect, useState, useTransition } from "react";
import { useNavigate } from "react-router";
import { Box, Stack, Typography } from "@mui/material";

import { apiBase, authHeaders, isUnauthorized } from "../api/api.js";
import { useAuth } from "../context/AuthContext.jsx";
import { ComplaintCard } from "./ComplaintCard.jsx";

export function ComplaintsList() {
    const { token, isModerator } = useAuth();
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
                navigate("/auth");
                return;
            }

            if (!res.ok) throw new Error();

            const list = await res.json();

            startTransition(() => {
                setComplaints(Array.isArray(list) ? list : []);
            });
        } catch {
            /* обработка ошибки по желанию */
        }
    }, [token, isModerator, navigate]);

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