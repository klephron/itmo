import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router";
import { Autocomplete, Box, Checkbox, Divider, FormControlLabel, Stack, TextField, Typography, } from "@mui/material";

import { SwipeableDialog } from "./SwipeableDialog";
import { LoadMoreButton } from "./LoadMoreButton";
import { apiBase, authHeaders } from "../api/api";
import { VideoCard } from "./VideoCard.jsx";

const PAGE_SIZE = 20;

const initialAiState = {
    running: false,
    phase: "",
    rewrite: "",
    expand: "",
    context: "",
    finalQuery: null,
};

export function SearchDialog({ open, setOpen }) {
    const navigate = useNavigate();

    const [query, setQuery] = useState("");
    const [results, setResults] = useState([]);
    const [paging, setPaging] = useState({ page: 0, size: PAGE_SIZE, totalElements: 0 });
    const [hasSearched, setHasSearched] = useState(false);
    const [loading, setLoading] = useState(false);
    const last = results.length >= paging.totalElements;

    const [ai, setAi] = useState(initialAiState);
    const [aiEnabled, setAiEnabled] = useState(true);
    const [aiDebugEnabled, setAiDebugEnabled] = useState(false);
    const aiSourceRef = useRef(null);

    const [suggests, setSuggests] = useState([]);
    const [loadingSuggest, setLoadingSuggest] = useState(false);
    const suggestTimer = useRef(null);

    const isSearchBusy = loading || ai.running;

    const loadPage = useCallback(async (pageNumber, { replace = false } = {}) => {
        const q = query.trim();
        if (!q) return;

        setLoading(true);

        try {
            const res = await fetch(
                `${apiBase}/api/search/videos?q=${encodeURIComponent(q)}&page=${pageNumber}&size=${PAGE_SIZE}&ai=${aiEnabled}`,
                { headers: authHeaders() }
            );

            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const data = await res.json();
            const items = data?.content ?? [];

            setResults((prev) => replace ? items : prev.concat(items));
            setPaging({
                page: data.page ?? pageNumber,
                size: PAGE_SIZE,
                totalElements: data.totalElements ?? items.length,
            });
        } catch (e) {
            //setStatus("Ошибка поиска: " + e.message);
        } finally {
            setLoading(false);
        }
    }, [aiEnabled, query]);

    const startAiStream = useCallback(async (q, page) => {
        if (!aiEnabled || !q) return;

        aiSourceRef.current?.close();
        setAi({ ...initialAiState, running: true, phase: "LLM: запрашиваем модель…" });

        const src = new EventSource(`${apiBase}/api/search/llm/stream?q=${encodeURIComponent(q)}&ai=true&page=${page}&size=${PAGE_SIZE}`);
        aiSourceRef.current = src;

        src.addEventListener("status", (e) => {
            const value = (e.data || "").trim();
            if (!value) return;

            setAi(prev => {
                if (value === "rewrite:start") {
                    return { ...prev, phase: "LLM: rewrite…" };
                }
                if (value === "rewrite:done") {
                    return { ...prev, phase: "LLM: rewrite готово" };
                }
                if (value === "expand:start") {
                    return { ...prev, phase: "LLM: expand…" };
                }
                if (value === "expand:done") {
                    return { ...prev, phase: "LLM: expand готово" };
                }
                if (value === "too_short") {
                    src.close();
                    return { ...prev, running: false, phase: "LLM: запрос слишком короткий" };
                }
                if (value.startsWith("llm:disabled")) {
                    src.close();
                    return { ...prev, running: false, phase: "LLM: отключена" };
                }
                if (value === "disabled") {
                    src.close();
                    return { ...prev, running: false, phase: "LLM: выключена" };
                }
                if (value === "skip:short") {
                    src.close();
                    return { ...prev, running: false, phase: "LLM: пропуск" };
                }

                // fallback
                return { ...prev, phase: "LLM: " + value };
            });
        });

        src.addEventListener("context", (e) => {
            setAi(prev => ({ ...prev, context: e.data || "" }));
        });

        src.addEventListener("rewrite", (e) => {
            setAi(prev => ({ ...prev, rewrite: prev.rewrite + (e.data || "") }));
        });

        src.addEventListener("expand", (e) => {
            setAi(prev => ({ ...prev, expand: prev.expand + (e.data || "") }));
        });

        src.addEventListener("expand-final", (e) => {
            const payload = (e.data || "").trim();
            if (!payload) return;
            setAi(prev => ({ ...prev, expand: payload }));
        });

        src.addEventListener("done", () => {
            setAi(prev => ({ ...prev, running: false, phase: "LLM: готово" }));
            src.close();
        });

        src.addEventListener("llm-error", (e) => {
            setAi(prev => ({ ...prev, running: false, phase: "LLM: ошибка " + (e.data || "") }));
            src.close();
        });

        src.onerror = () => {
            setAi(prev => ({ ...prev, running: false, phase: "LLM: ошибка подключения" }));
            src.close();
        };
    }, [aiEnabled]);

    const runSearch = useCallback(async (text) => {
        const q = text.trim();
        if (isSearchBusy || !q) return;

        setResults([]);
        setPaging({ page: 0, size: PAGE_SIZE, totalElements: 0 });

        if (aiEnabled) await startAiStream(q, 0);
        await loadPage(0, { replace: true });

        setHasSearched(true);
    }, [aiEnabled, startAiStream, loadPage]);

    const runSuggest = useCallback(async (q) => {
        setLoadingSuggest(true);
        try {
            const res = await fetch(
                `${apiBase}/api/search/videos/suggest?q=${encodeURIComponent(q)}`,
                { headers: authHeaders() }
            );
            if (!res.ok) throw new Error();
            const items = await res.json();
            setSuggests(Array.isArray(items) ? items : []);
        } catch {
            setSuggests([]);
        } finally {
            setLoadingSuggest(false);
        }
    }, []);

    const resetSearchState = useCallback(() => {
        setQuery("");
        setResults([]);
        setPaging({ page: 0, size: PAGE_SIZE, totalElements: 0 });
        setHasSearched(false);
        setLoading(false);
        setAi(initialAiState);
        setSuggests([]);
        setLoadingSuggest(false);
    }, []);

    useEffect(() => {
        if (!query.trim()) {
            setSuggests([]);
            return;
        }

        if (suggestTimer.current) {
            clearTimeout(suggestTimer.current);
        }

        suggestTimer.current = setTimeout(() => {
            runSuggest(query);
        }, 250);

        return () => clearTimeout(suggestTimer.current);
    }, [query, runSuggest]);

    const handleLoadMore = () => loadPage(paging.page + 1);
    const handleVideoOpen = (targetUuid, targetIndex) => {
        navigate("/feed", { state: { feedMode: { type: "search", query, aiEnabled, targetUuid, targetIndex } } });
        setOpen(false);
    }

    return (
        <SwipeableDialog open={open} setOpen={setOpen} title="Поиск видео">
            <Autocomplete
                size="small"
                freeSolo
                disabled={isSearchBusy}
                inputValue={query}
                options={suggests}
                filterOptions={(x) => x}
                loading={loadingSuggest}
                getOptionLabel={(o) => typeof o === "string" ? o : o.name || ""}
                onInputChange={(_, value, reason) => {
                    if (reason === "clear") {
                        resetSearchState();
                        return;
                    }
                    setQuery(value);
                }}
                onChange={(_, value) => {
                    const text = typeof value === "string" ? value : value?.name || "";
                    if (text) {
                        setQuery(text);
                        runSearch(text);
                    }
                }}
                onKeyDown={(e) => {
                    if (e.key === "Enter") {
                        e.preventDefault();
                        runSearch(query);
                    }
                }}
                renderInput={(params) => (
                    <TextField
                        {...params}
                        placeholder="Введите название или описание"
                    />
                )}
                slotProps={{ paper: { elevation: 24 } }}
                sx={{ mx: 2, mb: 2 }}
            />
            <Stack
                direction="row"
                spacing={2}
                px={2}
                mb={1.5}
                justifyContent="space-between"
            >
                <FormControlLabel
                    control={
                        <Checkbox
                            size="small"
                            checked={aiEnabled}
                            disabled={isSearchBusy}
                            onChange={(e) => setAiEnabled(e.target.checked)}
                            sx={{ p: 0, mr: 1 }}
                        />
                    }
                    label="Использовать ИИ"
                />
                {aiEnabled && (
                    <FormControlLabel
                        control={
                            <Checkbox
                                size="small"
                                checked={aiDebugEnabled}
                                onChange={(e) => setAiDebugEnabled(e.target.checked)}
                                sx={{ p: 0, mr: 1 }}
                            />
                        }
                        label="Диагностика ИИ"
                    />
                )}
            </Stack>
            <Typography variant="caption" color="text.secondary" textAlign="center" mb={1.5}>
                {isSearchBusy ? (
                    "Ищем видео..."
                ) : hasSearched ? (
                    `Найдено ${paging.totalElements} видео`
                ) : "Запустите поиск"}
            </Typography>
            <Box flex={1} overflow="hidden" position="relative" mb={2}>
                <Stack
                    spacing={1}
                    sx={{ px: 2, height: 1, overflowX: "hidden", overflowY: "auto", }}
                >
                    {results?.length ? results.map((video, index) => (
                        <VideoCard
                            key={video.uuid}
                            video={video}
                            onClick={() => handleVideoOpen(video.uuid, index)}
                            elevation={24}
                        />
                    )) : isSearchBusy && (
                        <>
                            <VideoCard elevation={24}/>
                            <VideoCard elevation={24}/>
                        </>
                    )}
                </Stack>
                {!last && (
                    <LoadMoreButton
                        sx={(theme) => ({
                            position: 'absolute',
                            bottom: theme.spacing(1),
                            left: "50%",
                            transform: `translateX(-50%)`,
                        })}
                        onClick={handleLoadMore}
                        loading={loading}
                    />
                )}
            </Box>
            {aiEnabled && aiDebugEnabled && (
                <>
                    <Divider/>
                    <Stack spacing={1.5} px={2} py={1.5}>
                        <Typography fontWeight="bold" mb={1.5}>Диагностика ИИ</Typography>
                        {ai.phase && <Typography variant="body2">{ai.phase}</Typography>}
                        {ai.rewrite && <Typography variant="body2">Rewrite: {ai.rewrite}</Typography>}
                        {ai.expand && <Typography variant="body2">Expand: {ai.expand}</Typography>}
                    </Stack>
                </>
            )}
        </SwipeableDialog>
    );
}