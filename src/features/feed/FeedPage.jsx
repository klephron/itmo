import { Box, Button } from "@mui/material";
import { VideoCard } from "./VideoCard.jsx";
import { BOTTOM_NAV_HEIGHT } from "@/theme.js";
import { useEffect, useMemo, useRef, useState } from "react";
import { useApi } from "@/hooks";
import { api } from "@/services/api.js";

const COLORS = [
    "#ff5252", "#ff9800", "#ffeb3b", "#4caf50", "#00bcd4",
    "#2196f3", "#673ab7", "#e91e63", "#9c27b0", "#795548",
    "#3f51b5", "#009688", "#8bc34a", "#ffc107", "#f44336",
];

const PAGE_SIZE = 2;
const DRAG_THRESHOLD = 30;

export const FeedPage = () => {
    /* ---------- Scroll ---------- */

    const containerRef = useRef(null);
    const startY = useRef(0);
    const wasDragged = useRef(false);
    const isDragging = useRef(false);

    const onMouseDown = (e) => {
        startY.current = e.clientY;
        wasDragged.current = false;
        isDragging.current = true;
    };

    const onMouseUp = (e) => {
        if (!isDragging.current) return;
        isDragging.current = false;

        const deltaY = e.clientY - startY.current;
        const container = containerRef.current;
        const pageHeight = container.clientHeight;

        if (Math.abs(deltaY) > DRAG_THRESHOLD) {
            wasDragged.current = true;

            container.scrollBy({
                top: deltaY > 0 ? -pageHeight : pageHeight,
                behavior: "smooth",
            });
        }
    };

    /* ---------- Data ---------- */

    const [pages, setPages] = useState({}); // влияет на UI
    const [requestedPage, setRequestedPage] = useState(0);
    const [activeVideo, setActiveVideo] = useState(0);
    const activePage = Math.floor(activeVideo / PAGE_SIZE)
    const loadedPagesRef = useRef(new Set()); // не влияет на UI

    const { data } = useApi(api.getVideosPage, requestedPage, PAGE_SIZE);

    /* ---------- Save page ---------- */

    useEffect(() => {
        if (!data || !Array.isArray(data.content)) return;

        const page = data.number;

        // guard — без setState
        if (loadedPagesRef.current.has(page)) return;

        loadedPagesRef.current.add(page);

        setPages((prev) => {
            const next = { ...prev, [page]: data.content };

            // чистим по activePage
            const keep = new Set([activePage - 1, activePage, activePage + 1]);

            Object.keys(next).forEach((p) => {
                if (!keep.has(Number(p))) {
                    delete next[p];
                    loadedPagesRef.current.delete(Number(p));
                }
            });

            return next;
        });
    }, [data, activePage]);

    /* ---------- Flatten ---------- */

    useEffect(() => {
        if (activeVideo % PAGE_SIZE === PAGE_SIZE - 1) {
            if (loadedPagesRef.current.has(activePage + 1)) return;

            //console.log(activeVideo)

            setRequestedPage(activePage + 1);
        }
    }, [activePage, activeVideo]);

    const videos = useMemo(() => {
        return Object.keys(pages)
            .map(Number)
            .sort((a, b) => a - b)
            .flatMap((page) => pages[page]);
    }, [pages]);

    console.log(activePage, activeVideo, Object.keys(pages));

    return (
        <Box
            ref={containerRef}
            onMouseDown={onMouseDown}
            onMouseUp={onMouseUp}
            sx={{
                height: `calc(100dvh - ${BOTTOM_NAV_HEIGHT}px)`,
                overflowY: "auto",
                scrollSnapType: "y mandatory",
                scrollbarWidth: "none",
                "&::-webkit-scrollbar": { display: "none" },
            }}
        >
            <Button
                variant="contained"
                sx={{ position: "absolute", top: 0, left: 0 }}
                onClick={() => setActiveVideo((prev) => prev + 1)}
            >
                Next video
            </Button>
            {videos.map((_, index) => (
                <VideoCard
                    key={index}
                    index={index}
                    color={COLORS[index % COLORS.length]}
                />
            ))}
        </Box>
    );
};