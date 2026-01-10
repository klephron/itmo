import { useCallback, useEffect, useRef, useState } from "react";
import { Autocomplete, Stack, TextField, Typography } from "@mui/material";
import { SwipeableDialog } from "./SwipeableDialog";
import { apiBase, authHeaders } from "../api/api";
import { formatCompactNumber, formatDate, humanSize } from "../utils/format";

export function SearchDialog({ open, setOpen, onGlobalPlayByUuid }) {
    const [query, setQuery] = useState("");
    const [results, setResults] = useState([]);
    const [total, setTotal] = useState(null);
    const [status, setStatus] = useState("");

    const [suggests, setSuggests] = useState([]);
    const [loadingSuggest, setLoadingSuggest] = useState(false);

    const suggestTimer = useRef(null);

    const runSearch = useCallback(async (text) => {
        const q = text.trim();
        if (!q) {
            setStatus("Введите запрос");
            return;
        }

        setStatus("Ищу…");
        setResults([]);

        try {
            const res = await fetch(
                `${apiBase}/api/search/videos?q=${encodeURIComponent(q)}&size=20`,
                { headers: authHeaders() }
            );

            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const page = await res.json();
            const items = Array.isArray(page.content)
                ? page.content
                : Array.isArray(page)
                    ? page
                    : [];

            setTotal(
                typeof page.totalElements === "number"
                    ? page.totalElements
                    : items.length
            );

            setResults(items);
            setStatus(items.length ? "" : "Ничего не найдено");
        } catch (e) {
            setStatus("Ошибка поиска: " + e.message);
        }
    }, []);

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

    return (
        <SwipeableDialog
            open={open}
            setOpen={setOpen}
            title="Поиск видео"
        >
            <Stack p={2} pt={0} spacing={1.5} sx={{ overflowY: "auto" }}>
                <Autocomplete
                    size="small"
                    freeSolo
                    inputValue={query}
                    options={suggests}
                    filterOptions={(x) => x}
                    loading={loadingSuggest}
                    getOptionLabel={(o) => typeof o === "string" ? o : o.name || ""}
                    onInputChange={(_, value) => setQuery(value)}
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
                />

                <Typography variant="caption" color="text.secondary" textAlign="center">
                    Найдено {total == null ? "—" : `${total} видео`}
                </Typography>

                <Stack spacing={1}>
                    {results.map((v) => (
                        <Stack
                            key={v.uuid}
                            spacing={0.5}
                            sx={{
                                p: 1,
                                borderRadius: 1,
                                bgcolor: "action.hover",
                                cursor: "pointer",
                            }}
                            onClick={() => onGlobalPlayByUuid?.(v.uuid)}
                        >
                            <Typography variant="body2">
                                {v.name || "(без названия)"}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                                {formatDate(v.uploadedAt)} ·{" "}
                                {humanSize(v.contentSize)}
                            </Typography>
                            {v.description && (
                                <Typography variant="caption" color="text.secondary">
                                    {v.description}
                                </Typography>
                            )}
                        </Stack>
                    ))}
                </Stack>

                {status && (
                    <Typography variant="caption" color="text.secondary">
                        {status}
                    </Typography>
                )}
            </Stack>
        </SwipeableDialog>
    );
}