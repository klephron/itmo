import React, { useCallback, useEffect, useRef, useState } from "react";
import { apiBase, authHeaders } from "../api/api";
import { formatDate, humanSize } from "../utils/format";

export default function SearchPage({ active, onGlobalPlayByUuid }) {
    const [query, setQuery] = useState("");
    const [results, setResults] = useState([]);
    const [total, setTotal] = useState(null);
    const [status, setStatus] = useState("");

    const [suggests, setSuggests] = useState([]);
    const [showSuggests, setShowSuggests] = useState(false);

    const suggestTimerRef = useRef(null);

    /* =========================
       SEARCH
       ========================= */

    const runSearchWithQuery = useCallback(async (q) => {
        const queryText = q.trim();
        if (!queryText) {
            setStatus("Введите запрос");
            return;
        }

        setStatus("Ищу…");
        setResults([]);
        setShowSuggests(false);
        setQuery(queryText);

        try {
            const res = await fetch(
                `${apiBase}/api/search/videos?q=${encodeURIComponent(queryText)}&size=20`,
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

    const runSearch = useCallback(() => {
        runSearchWithQuery(query);
    }, [query, runSearchWithQuery]);


    /* =========================
       SUGGEST
       ========================= */

    const runSuggest = useCallback(async (q) => {
        try {
            const res = await fetch(
                `${apiBase}/api/search/videos/suggest?q=${encodeURIComponent(q)}`,
                { headers: authHeaders() }
            );

            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const items = await res.json();
            setSuggests(Array.isArray(items) ? items : []);
            setShowSuggests(true);
        } catch (e) {
            setSuggests([{ __error: e.message }]);
            setShowSuggests(true);
        }
    }, []);

    const onInputChange = (e) => {
        const val = e.target.value;
        setQuery(val);

        if (suggestTimerRef.current) {
            clearTimeout(suggestTimerRef.current);
        }

        if (!val.trim()) {
            setShowSuggests(false);
            return;
        }

        suggestTimerRef.current = setTimeout(
            () => runSuggest(val.trim()),
            250
        );
    };

    /* =========================
       EFFECTS
       ========================= */

    useEffect(() => {
        if (!active) {
            setShowSuggests(false);
        }
    }, [active]);

    /* =========================
       RENDER
       ========================= */

    return (
        <div className={`card page ${active ? "active" : ""}`} id="pageSearch">
            <div className="header">
                <div className="title">Поиск</div>
                <div className="badge">
                    {total == null ? "—" : `${total} видео`}
                </div>
            </div>

            <div className="form-row">
                <label htmlFor="searchInput">Запрос</label>
                <div className="search-input-wrapper">
                    <input
                        id="searchInput"
                        type="text"
                        placeholder="Название или описание"
                        value={query}
                        onChange={onInputChange}
                        onKeyDown={(e) => {
                            if (e.key === "Enter") {
                                e.preventDefault();
                                runSearch();
                            }
                        }}
                    />

                    <div
                        className={`suggest-dropdown ${
                            showSuggests ? "visible" : ""
                        }`}
                    >
                        {suggests.length === 0 && (
                            <div className="suggest-item">
                                Нет подсказок
                            </div>
                        )}

                        {suggests.map((v, idx) => {
                            if (v.__error) {
                                return (
                                    <div key={idx} className="suggest-item">
                                        Ошибка: {v.__error}
                                    </div>
                                );
                            }

                            return (
                                <div
                                    key={v.uuid || idx}
                                    className="suggest-item"
                                    onClick={() => {
                                        const name = v.name || "";
                                        runSearchWithQuery(name);
                                    }}
                                >
                                    <div>{v.name || "(без названия)"}</div>
                                    <div className="suggest-meta">
                                        {formatDate(v.uploadedAt)} ·{" "}
                                        {humanSize(v.contentSize || 0)}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </div>
            </div>

            <div className="controls" style={{ marginTop: 6 }}>
                <button className="full" onClick={runSearch}>
                    Искать
                </button>
            </div>

            <div
                className="studio-videos"
                style={{
                    marginTop: 8,
                    maxHeight: 320,
                    overflowY: "auto",
                }}
            >
                {results.map((v) => (
                    <div
                        key={v.uuid}
                        className="studio-row"
                        onClick={() => onGlobalPlayByUuid?.(v.uuid)}
                    >
                        <div className="studio-row-header">
                            <div className="studio-row-title">
                                {v.name || "(без названия)"}
                            </div>
                        </div>
                        <div className="studio-row-meta">
                            {formatDate(v.uploadedAt)} ·{" "}
                            {humanSize(v.contentSize)}
                        </div>
                        {v.description && (
                            <div className="studio-row-meta">
                                {v.description}
                            </div>
                        )}
                    </div>
                ))}
            </div>

            <div className="status">{status}</div>
        </div>
    );
}