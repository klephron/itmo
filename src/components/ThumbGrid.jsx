import { useEffect, useState, useTransition } from "react";
import { useNavigate, useOutletContext } from "react-router";
import { Box, Grid, MenuItem, Stack, TextField, Typography } from "@mui/material";

import { apiBase, authHeaders } from "../api/api";
import { useAuth } from "../context/AuthContext";
import { ThumbCard } from "./ThumbCard";
import { LoadMoreButton } from "./LoadMoreButton";
import { ConfirmDialog } from "./ConfirmDialog.jsx";

const PAGE_SIZE = 12;
const COLUMNS = 3;

export function ThumbGrid({ label = "Видео", showDelete = false }) {
    const { token } = useAuth();
    const { userId } = useOutletContext();
    const navigate = useNavigate();

    const [isPending, startTransition] = useTransition();

    const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);
    const [selectedId, setSelectedId] = useState(null);

    const [videos, setVideos] = useState([]);
    const [paging, setPaging] = useState({
        page: 0,
        size: PAGE_SIZE,
        last: true,
        totalElements: 0,
    });

    const [loading, setLoading] = useState(false);
    const [sort, setSort] = useState("uploadedAt,desc");

    async function loadPage(pageNumber, { replace = false } = {}) {
        if (!userId) return;

        const res = await fetch(
            `${apiBase}/api/users/${userId}/videos?page=${pageNumber}&size=${PAGE_SIZE}&sort=${encodeURIComponent(sort)}`,
            { headers: authHeaders(token) }
        );
        if (!res.ok) throw new Error("HTTP " + res.status);

        const page = await res.json();
        const vids = page?.content ?? [];

        startTransition(() => {
            setVideos(prev => (replace ? vids : prev.concat(vids)));
            setPaging({
                page: page.number,
                size: PAGE_SIZE,
                last: !!page.last,
                totalElements: page.totalElements ?? 0,
            });
        });
    }

    async function reloadUpToCurrentPage() {
        const pagesToLoad = paging.page + 1;

        setVideos([]);
        setPaging(p => ({ ...p, page: 0, last: false }));

        for (let p = 0; p < pagesToLoad; p++) {
            await loadPage(p, { replace: p === 0 });
        }
    }

    async function deleteVideo(uuid) {
        if (loading) return;
        setLoading(true);

        try {
            const res = await fetch(
                `${apiBase}/api/streaming/video/delete/${uuid}`,
                { method: "POST", headers: authHeaders(token) }
            );
            if (!res.ok) throw new Error("Delete failed");

            await reloadUpToCurrentPage();
        } finally {
            setSelectedId(null);
            setLoading(false);
        }
    }

    // ---------- первая загрузка / смена сортировки ----------
    useEffect(() => {
        if (!userId) return;
        loadPage(0, { replace: true });
    }, [userId, sort]);

    const handleLoadMore = () => loadPage(paging.page + 1);
    const handleVideoOpen = (targetUuid, targetIndex) => () => (
        navigate("/feed", { state: { feedMode: { type: "user", userId, sort, targetUuid, targetIndex } } })
    );

    const handleDeleteVideo = (uuid) => () => {
        setSelectedId(uuid);
        setConfirmDialogOpen(true);
    }
    const handleConfirm = () => deleteVideo(selectedId);

    return (
        <>
            <Stack direction="row" justifyContent="space-between" alignItems="center" px={2}>
                <Stack direction="row" spacing={1}>
                    <Typography variant="h6" fontWeight="bold">{label}</Typography>
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
                <Typography mt={1.5} px={2}>Видео пока нет</Typography>
            ) : (
                <Box sx={{ borderRadius: 3, flex: 1, overflow: "hidden", position: "relative", m: 2 }}>
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
                                            showDelete={showDelete}
                                            onOpen={handleVideoOpen(v.uuid, index)}
                                            onDelete={handleDeleteVideo(v.uuid)}
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
            <ConfirmDialog
                open={confirmDialogOpen}
                setOpen={setConfirmDialogOpen}
                title="Подтверждение удаления"
                text="Вы уверены, что хотите безвозвратно удалить видео?"
                critical
                onConfirm={handleConfirm}
            />
        </>
    );
}