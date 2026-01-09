import { useEffect, useState } from "react";
import { Box, Grid, MenuItem, Stack, TextField, Typography } from "@mui/material";
import { apiBase, authHeaders } from "../api/api";
import { useAuth } from "../context/AuthContext";
import { ThumbCard } from "./ThumbCard";
import { LoadMoreButton } from "./LoadMoreButton";

const PAGE_SIZE = 1;
const COLUMNS = 3;

export function ThumbGrid({ active, userId, onOpenVideo }) {
    const { token } = useAuth();

    const [videos, setVideos] = useState([]);
    const [paging, setPaging] = useState({
        page: 0,
        size: PAGE_SIZE,
        last: true,
        totalElements: 0,
    });

    const [loading, setLoading] = useState(false);
    const [sort, setSort] = useState("uploadedAt,desc");

    // ---------- загрузка страницы ----------
    async function loadPage(pageNumber, { replace = false } = {}) {
        if (!userId || loading) return;

        setLoading(true);

        try {
            const res = await fetch(
                `${apiBase}/api/users/${userId}/videos?page=${pageNumber}&size=${PAGE_SIZE}&sort=${encodeURIComponent(sort)}`,
                { headers: authHeaders(token) }
            );

            if (!res.ok) throw new Error("HTTP " + res.status);

            const page = await res.json();
            const vids = page?.content ?? [];

            setVideos((prev) => (replace ? vids : prev.concat(vids)));

            setPaging({
                page: page.number,
                size: PAGE_SIZE,
                last: !!page.last,
                totalElements: page.totalElements ?? 0,
            });
        } finally {
            setTimeout(() => setLoading(false), 5000);
            //setLoading(false);
        }
    }

    // ---------- первая загрузка / смена сортировки ----------
    useEffect(() => {
        if (!active || !userId) return;
        loadPage(0, { replace: true });
    }, [active, userId, sort]);

    // ---------- reset при уходе ----------
    useEffect(() => {
        if (!active) {
            setVideos([]);
            setPaging({
                page: 0,
                size: PAGE_SIZE,
                last: true,
                totalElements: 0,
            });
            setLoading(false);
        }
    }, [active]);

    const handleLoadMore = () => loadPage(paging.page + 1);

    return (
        <>
            <Stack direction="row" justifyContent="space-between" alignItems="center" pb={1}>
                <Stack direction="row" spacing={1}>
                    <Typography variant="h6" fontWeight="bold">Видео</Typography>
                    <Typography variant="body2" color="text.secondary" pt={1}>
                        {paging?.totalElements ?? 0}
                    </Typography>
                </Stack>
                <TextField
                    select
                    size="small"
                    value={sort}
                    onChange={(e) => setSort(e.target.value)}
                >
                    <MenuItem value="uploadedAt,desc">Сначала новые</MenuItem>
                    <MenuItem value="uploadedAt,asc">Сначала старые</MenuItem>
                    <MenuItem value="viewsCount,desc">По просмотрам</MenuItem>
                    <MenuItem value="likesCount,desc">По лайкам</MenuItem>
                </TextField>
            </Stack>
            {!videos || videos.length === 0 ? (
                <Typography>Видео пока нет</Typography>
            ) : (
                <Box sx={{ borderRadius: 3, flex: 1, overflow: "hidden", position: "relative" }}>
                    <Box sx={{ height: 1, overflowX: "hidden", overflowY: "auto" }}>
                        <Grid container spacing="1px">
                            {videos.map((v, index) => {
                                const total = videos.length;
                                const rows = Math.ceil(total / COLUMNS);
                                const row = Math.floor(index / COLUMNS);
                                const col = index % COLUMNS;

                                const lastRowCount = total % COLUMNS || COLUMNS;
                                const lastRowIndex = rows - 1;
                                const prevRowIndex = rows - 2;

                                const isFirstRow = row === 0;
                                const isFirstCol = col === 0;
                                const isInLastRow = row === lastRowIndex;
                                const isLastInRow = col === COLUMNS - 1 || index === total - 1;
                                const isLastInPrevRow =
                                    lastRowCount !== COLUMNS &&
                                    row === prevRowIndex &&
                                    col === COLUMNS - 1;

                                return (
                                    <Grid
                                        key={v.uuid}
                                        size={12 / COLUMNS}
                                        sx={(theme) => ({
                                            borderTopLeftRadius:
                                                isFirstRow && isFirstCol
                                                    ? `calc(${theme.vars.shape.borderRadius} * 3)`
                                                    : 0,
                                            borderTopRightRadius:
                                                isFirstRow && isLastInRow
                                                    ? `calc(${theme.vars.shape.borderRadius} * 3)`
                                                    : 0,
                                            borderBottomLeftRadius:
                                                isInLastRow && isFirstCol
                                                    ? `calc(${theme.vars.shape.borderRadius} * 3)`
                                                    : 0,
                                            borderBottomRightRadius:
                                                (isInLastRow && isLastInRow) || isLastInPrevRow
                                                    ? `calc(${theme.vars.shape.borderRadius} * 3)`
                                                    : 0,
                                            overflow: "hidden",
                                        })}
                                    >
                                        <ThumbCard
                                            video={v}
                                            showDelete={false}
                                            onOpen={() =>
                                                onOpenVideo?.({
                                                    userId,
                                                    sort,
                                                    uuid: v.uuid,
                                                })
                                            }
                                        />
                                    </Grid>
                                );
                            })}
                        </Grid>
                    </Box>
                    {!paging.last && (
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
            )}
        </>
    );
}