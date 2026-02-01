import { useEffect, useState } from "react";
import { Button, Paper, Stack, TextField, Typography, } from "@mui/material";
import CloseRoundedIcon from "@mui/icons-material/CloseRounded";
import { useSnackbar } from "notistack";
import { useNavigate } from "react-router";

import { apiBase, authHeaders, isUnauthorized } from "../api/api";
import { useAuth } from "../context/AuthContext";

export function Preferences() {
    const { user, token, setAuth } = useAuth();
    const { enqueueSnackbar } = useSnackbar();
    const navigate = useNavigate();

    const [prefs, setPrefs] = useState({
        likeWeight: "",
        followWeight: "",
        watchWeight: "",
        watchThreshold: "",
    });
    const [topics, setTopics] = useState([]);

    useEffect(() => {
        if (!user || !token) return;

        let cancelled = false;
        fetch(apiBase + "/api/streaming/video/feed/preferences", {
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
            .then((data) => {
                if (cancelled) return;
                setPrefs({
                    likeWeight: data.likeWeight ?? "",
                    followWeight: data.followWeight ?? "",
                    watchWeight: data.watchWeight ?? "",
                    watchThreshold: data.watchThreshold ?? "",
                });
            })
            .catch((e) => {
                if (cancelled) return;
                enqueueSnackbar("Ошибка загрузки весов: " + e.message, { variant: "error" });
            });

        return () => {
            cancelled = true;
        };
    }, [user?.userId, token]);

    const save = () => {
        const payload = {
            likeWeight: prefs.likeWeight === "" ? null : Number(prefs.likeWeight),
            followWeight: prefs.followWeight === "" ? null : Number(prefs.followWeight),
            watchWeight: prefs.watchWeight === "" ? null : Number(prefs.watchWeight),
            watchThreshold: prefs.watchThreshold === "" ? null : Number(prefs.watchThreshold),
        };

        fetch(apiBase + "/api/streaming/video/feed/preferences", {
            method: "PUT",
            headers: {
                ...authHeaders(token),
                "Content-Type": "application/json",
            },
            body: JSON.stringify(payload),
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
            .then((data) => {
                setPrefs({
                    likeWeight: data.likeWeight ?? "",
                    followWeight: data.followWeight ?? "",
                    watchWeight: data.watchWeight ?? "",
                    watchThreshold: data.watchThreshold ?? "",
                });
                enqueueSnackbar("Весы сохранены", { variant: "success" });
            })
            .catch((e) => {
                enqueueSnackbar("Ошибка сохранения весов: " + e.message, { variant: "error" });
            });
    };

    useEffect(() => {
        if (!user || !token) return;

        let cancelled = false;

        fetch(apiBase + "/api/streaming/video/feed/preferences/topics", {
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
            .then((topics) => {
                if (cancelled) return;
                setTopics(topics ?? []);
            })
            .catch((e) => {
                if (cancelled) return;
                enqueueSnackbar("Ошибка загрузки тем: " + e.message, { variant: "error" });
            });

        return () => {
            cancelled = true;
        };
    }, [user?.userId, token]);

    const saveTopics = () => {
        fetch(apiBase + "/api/streaming/video/feed/preferences/topics", {
            method: "PUT",
            headers: {
                ...authHeaders(token),
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ topics }),
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
            .then((topics) => {
                setTopics(topics ?? []);
                enqueueSnackbar("Темы сохранены", { variant: "success" });
            })
            .catch((e) => {
                enqueueSnackbar("Ошибка сохранения тем: " + e.message, { variant: "error" });
            });
    };

    return (
        <>
            <Typography variant="h6" fontWeight="bold" py={0.5} px={2}>
                Настройка весов
            </Typography>
            <Stack mt={2} px={2} spacing={2}>
                <TextField
                    label="Вес лайков"
                    type="number"
                    value={prefs.likeWeight}
                    onChange={(e) => setPrefs({ ...prefs, likeWeight: e.target.value })}
                    slotProps={{ htmlInput: { min: 0, step: 0.1 } }}
                    size="small"
                />
                <TextField
                    label="Вес подписок"
                    type="number"
                    value={prefs.followWeight}
                    onChange={(e) => setPrefs({ ...prefs, followWeight: e.target.value })}
                    slotProps={{ htmlInput: { min: 0, step: 0.1 } }}
                    size="small"
                />
                <TextField
                    label="Вес просмотров"
                    type="number"
                    value={prefs.watchWeight}
                    onChange={(e) => setPrefs({ ...prefs, watchWeight: e.target.value })}
                    slotProps={{ htmlInput: { min: 0, step: 0.1 } }}
                    size="small"
                />
                <TextField
                    label="Порог просмотра (0..1)"
                    type="number"
                    value={prefs.watchThreshold}
                    onChange={(e) => setPrefs({ ...prefs, watchThreshold: e.target.value })}
                    slotProps={{ htmlInput: { min: 0, max: 1, step: 0.05 } }}
                    size="small"
                />
                <Button variant="contained" onClick={save}>Сохранить</Button>

            </Stack>
            <Typography variant="h6" fontWeight="bold" mt={2} px={2} py={0.5}>
                Настройка тем
            </Typography>
            {!topics?.length ? (
                <Typography mt={1.5} px={2} flex={1}>
                    Тем пока нет
                </Typography>
            ) : (
                <Stack mt={2} px={2} spacing={1.5} sx={{ height: 1, overflowX: "hidden", overflowY: "auto" }}>
                    {topics.map((t, i) => (
                        <Stack
                            key={i}
                            component={Paper}
                            variant="outlined"
                            direction="row"
                            spacing={1}
                            sx={{
                                p: 1,
                                borderRadius: 3,
                            }}
                        >
                            <TextField
                                label="Тема"
                                value={t.term}
                                onChange={(e) => {
                                    const copy = [...topics];
                                    copy[i] = { ...copy[i], term: e.target.value };
                                    setTopics(copy);
                                }}
                                size="small"
                                fullWidth
                            />
                            <TextField
                                label="Вес"
                                type="number"
                                value={t.weight}
                                onChange={(e) => {
                                    const copy = [...topics];
                                    copy[i] = {
                                        ...copy[i],
                                        weight: Number(e.target.value),
                                    };
                                    setTopics(copy);
                                }}
                                slotProps={{ htmlInput: { step: 0.1 } }}
                                size="small"
                                sx={{ width: 150 }}
                            />
                            <Button
                                variant="contained"
                                color="error"
                                onClick={() => setTopics(topics.filter((_, idx) => idx !== i))}
                            >
                                <CloseRoundedIcon fontSize="small"/>
                            </Button>
                        </Stack>
                    ))}
                </Stack>
            )}
            <Stack direction="row" spacing={1} p={2}>
                <Button
                    fullWidth
                    variant="contained"
                    color="inherit"
                    onClick={() => setTopics([...topics, { term: "", weight: 1 }])}
                >
                    Добавить
                </Button>
                <Button fullWidth variant="contained" onClick={saveTopics}>
                    Сохранить
                </Button>
            </Stack>
        </>
    );
}