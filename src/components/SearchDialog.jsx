import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router";
import { Autocomplete, Box, Stack, TextField, Typography } from "@mui/material";
import { SwipeableDialog } from "./SwipeableDialog";
import { LoadMoreButton } from "./LoadMoreButton";
import { apiBase, authHeaders } from "../api/api";
import { VideoCard } from "./VideoCard.jsx";

const PAGE_SIZE = 20;

export function SearchDialog({ open, setOpen }) {
    const navigate = useNavigate();

    const [query, setQuery] = useState("");
    const [results, setResults] = useState([]);
    const [status, setStatus] = useState("");

    const [paging, setPaging] = useState({ page: 0, size: PAGE_SIZE, totalElements: 0 });
    const [loading, setLoading] = useState(false);
    const [hasSearched, setHasSearched] = useState(false);
    const last = results.length >= paging.totalElements;

    const [suggests, setSuggests] = useState([]);
    const [loadingSuggest, setLoadingSuggest] = useState(false);

    const suggestTimer = useRef(null);

    const loadPage = useCallback(
        async (pageNumber, { replace = false } = {}) => {
            const q = query.trim();
            if (!q || loading) return;

            setLoading(true);
            setStatus("Ищу…");

            try {
                const res = await fetch(
                    `${apiBase}/api/search/videos?q=${encodeURIComponent(q)}&page=${pageNumber}&size=${PAGE_SIZE}`,
                    { headers: authHeaders() }
                );

                if (!res.ok) throw new Error(`HTTP ${res.status}`);

                const page = await res.json();
                const items = page?.content ?? [];

                setResults((prev) =>
                    replace ? items : prev.concat(items)
                );

                setPaging({
                    page: page.number ?? pageNumber,
                    size: PAGE_SIZE,
                    totalElements: page.totalElements ?? items.length,
                });

                setStatus(items.length ? "" : "Ничего не найдено");
            } catch (e) {
                setStatus("Ошибка поиска: " + e.message);
            } finally {
                setLoading(false);
            }
        },
        [query, loading]
    );

    const runSearch = useCallback(
        (text) => {
            const q = text.trim();
            if (!q) {
                setStatus("Введите запрос");
                return;
            }

            setHasSearched(true);
            setResults([]);
            setPaging({
                page: 0,
                size: PAGE_SIZE,
                totalElements: 0,
            });

            loadPage(0, { replace: true });
        },
        [loadPage]
    );

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
        setStatus("");
        setSuggests([]);
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
        navigate("/feed", { state: { feedMode: { type: "search", query, targetUuid, targetIndex } } });
        setOpen(false);
    }

    return (
        <SwipeableDialog open={open} setOpen={setOpen} title="Поиск видео">
            <Autocomplete
                size="small"
                freeSolo
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
                sx={{ mx: 2, mb: 1.5 }}
            />
            {hasSearched && (
                <Typography
                    variant="caption"
                    color="text.secondary"
                    textAlign="center"
                    sx={{ mb: 1.5 }}
                >
                    Найдено {paging.totalElements} видео
                </Typography>
            )}
            <Box flex={1} overflow="hidden" position="relative" mb={2}>
                <Stack
                    spacing={1}
                    sx={{ px: 2, height: 1, overflowX: "hidden", overflowY: "auto", }}
                >
                    {results.map((video, index) => (
                        <VideoCard
                            key={video.uuid}
                            video={video}
                            onClick={() => handleVideoOpen(video.uuid, index)}
                            elevation={24}
                        />
                    ))}
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
        </SwipeableDialog>
    );
}