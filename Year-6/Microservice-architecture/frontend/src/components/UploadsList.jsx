import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router";
import { Box, LinearProgress, Stack, Typography, } from "@mui/material";
import { useSnackbar } from "notistack";

import { apiBase, authHeaders, isUnauthorized } from "../api/api";
import { useAuth } from "../context/AuthContext";

export function UploadsList() {
    const { token, setAuth } = useAuth();
    const { enqueueSnackbar } = useSnackbar();
    const navigate = useNavigate();

    const jobsRef = useRef(new Map());
    const [jobs, setJobs] = useState([]);

    const syncJobs = useCallback(() => {
        setJobs(
            Array.from(jobsRef.current.values()).map((j) => ({
                jobId: j.jobId,
                fileName: j.fileName,
                lastStatus: j.lastStatus,
            }))
        );
    }, []);

    const trackJob = useCallback((jobId, fileName, initialStatus) => {
        if (jobsRef.current.has(jobId)) return;

        const job = {
            jobId,
            fileName,
            lastStatus: initialStatus,
            intervalId: null,
        };
        jobsRef.current.set(jobId, job);
        syncJobs();

        job.intervalId = setInterval(async () => {
            try {
                const res = await fetch(
                    apiBase +
                    "/api/streaming/video/upload/status/" +
                    jobId,
                    { headers: authHeaders(token) }
                );

                if (isUnauthorized(res)) {
                    clearInterval(job.intervalId);
                    jobsRef.current.delete(jobId);
                    syncJobs();
                    enqueueSnackbar("Сессия истекла, войдите снова", { variant: "error" });
                    navigate("/auth");
                    setAuth(null, null);
                    return;
                }

                if (!res.ok) throw new Error("HTTP " + res.status);

                const st = await res.json();
                job.lastStatus = st;
                syncJobs();

                if (st.stage === "DONE" || st.stage === "ERROR") {
                    clearInterval(job.intervalId);

                    setTimeout(() => {
                        jobsRef.current.delete(jobId);
                        syncJobs();
                    }, 1000);
                }
            } catch {
                // тихо, без спама
            }
        }, 1000);
    }, [token, syncJobs, navigate, enqueueSnackbar, setAuth]);

    useEffect(() => {
        if (!token) return;

        let cancelled = false;

        fetch(apiBase + "/api/streaming/video/upload/active", {
            headers: authHeaders(token),
        })
            .then((res) => {
                if (isUnauthorized(res)) {
                    enqueueSnackbar("Сессия истекла, войдите снова", { variant: "error" });
                    navigate("/auth");
                    setAuth(null, null);
                    return;
                }
                if (!res.ok) throw new Error("HTTP " + res.status);
                return res.json();
            })
            .then((list) => {
                if (cancelled) return;
                jobsRef.current.clear();
                (list || []).forEach((j) =>
                    trackJob(j.jobId, j.fileName || "(без названия)", {
                        stage: j.stage,
                        percent: j.percent,
                        message: j.message,
                    })
                );
            })
            .catch((e) => {
                enqueueSnackbar("Ошибка при получении списка загрузок: " + e.message, { variant: "error" });
            });

        return () => {
            cancelled = true;
            jobsRef.current.forEach((job) => {
                if (job.intervalId) clearInterval(job.intervalId);
            });
            jobsRef.current.clear();
        };

    }, [token, trackJob]);

    return (
        <>
            <Stack direction="row" spacing={1} py={0.5} px={2}>
                <Typography variant="h6" fontWeight="bold">
                    Активные загрузки
                </Typography>
                <Typography variant="body2" color="text.secondary" pt={1}>
                    {jobs.length}
                </Typography>
            </Stack>

            {!jobs.length ? (
                <Typography mt={1.5} px={2}>
                    Загрузок пока нет
                </Typography>
            ) : (
                <Box sx={{ m: 1, borderRadius: 3, overflow: "hidden", flex: 1 }}>
                    <Stack spacing={2} sx={{ p: 1, overflowX: "hidden", overflowY: "auto", height: 1 }}>
                        {jobs.map((job) => (
                            <Box key={job?.jobId}>
                                <Stack direction="row" spacing={1.5} justifyContent="space-between">
                                    <Typography variant="body2" fontWeight="medium" noWrap>
                                        {job?.fileName}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        {job?.lastStatus?.percent}%
                                    </Typography>
                                </Stack>
                                <Typography variant="caption" color="text.secondary" noWrap>
                                    {job?.lastStatus?.message || job?.lastStatus?.stage}
                                </Typography>
                                <LinearProgress
                                    variant="determinate"
                                    value={job?.lastStatus?.percent}
                                    color={job?.lastStatus?.stage === "ERROR" ? "error" : "success"}
                                    sx={{ mt: 1 }}
                                />
                            </Box>
                        ))}
                    </Stack>
                </Box>
            )}
        </>
    );
}
