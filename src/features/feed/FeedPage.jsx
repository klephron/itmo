import { Box, Button, Stack, Typography } from "@mui/material";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useApi } from "@/hooks";
import { api } from "@/services/api.js";
import { VideoCard } from "@/features/feed/VideoCard.jsx";
import { BOTTOM_NAV_HEIGHT } from "@/theme.js";

const COLORS = [
    "#ff5252", "#ff9800", "#ffeb3b", "#4caf50", "#00bcd4",
    "#2196f3", "#673ab7", "#e91e63", "#9c27b0", "#795548",
    "#3f51b5", "#009688", "#8bc34a", "#ffc107", "#f44336",
];

const PAGE_SIZE = 3;
const QUEUE_SIZE = 3;
const DRAG_THRESHOLD = 30;
const PREFETCH_THRESHOLD = 1;

export const FeedPage = () => {
    const [pageNum, setPageNum] = useState(0);
    const { data } = useApi(api.getVideosPage, pageNum, PAGE_SIZE);

    const directions = useRef(new Map([[0, "forward"]]));
    const pageCache = useRef({});
    const [pageQueue, setPageQueue] = useState([]);
    const [activePos, setActivePos] = useState({ pageNum: 0, indexInPage: 0 });

    const addPageToQueue = useCallback((page) => {
        const direction = directions.current.get(page.number);
        if (!direction) return; // no navigation intent -> skip (maybe duplicate call)

        directions.current.delete(page.number);
        setPageQueue((prev) => {
            const filtered = prev.filter((p) => p.number !== page.number);
            const next = direction === "backward" ? (
                [page, ...filtered.slice(0, QUEUE_SIZE - 1)] // unshift + cut
            ) : (
                [...filtered.slice(-(QUEUE_SIZE - 1)), page] // cut + push
            );

            // evict cut pages from cache (Set for extra reliability)
            const nextNumbers = new Set(next.map((p) => p.number));
            prev.filter((p) => !nextNumbers.has(p.number)).forEach((p) => delete pageCache.current[p.number])

            return next;
        });
    }, []);

    useEffect(() => {
        if (!data || pageCache.current[data.number]) return;

        pageCache.current[data.number] = data;
        addPageToQueue(data);
    }, [addPageToQueue, data]);

    const nextVideo = () => {
        const activePage = pageCache.current[activePos.pageNum];
        if (!activePage) return;

        const nextIndexInPage = (
            activePos.indexInPage === activePage.numberOfElements - 1 ? 0 : activePos.indexInPage + 1
        );
        const nextPageNum = (
            nextIndexInPage !== 0 ? (
                activePage.number // don't change the page if index got incremented
            ) : !activePage.last ? (
                activePage.number + 1 // increment pageNum if current page isn't last
            ) : (
                0 // start from first page if current page is last
            )
        );
        const nextPage = pageCache.current[nextPageNum];

        if (nextPage.totalPages > 1 && (
            nextIndexInPage === Math.max(nextPage.numberOfElements - 1 - PREFETCH_THRESHOLD, 0)
        )) {
            const prefetchPageNum = !nextPage.last ? nextPage.number + 1 : 0;
            const prefetchPage = pageCache.current[prefetchPageNum];

            directions.current.set(prefetchPageNum, "forward");
            if (prefetchPage) {
                addPageToQueue(prefetchPage);
            } else if (pageNum !== prefetchPageNum) {
                setPageNum(prefetchPageNum);
            }
        }

        setActivePos({ pageNum: nextPageNum, indexInPage: nextIndexInPage });
    };

    const prevVideo = () => {
        const activePage = pageCache.current[activePos.pageNum];
        if (!activePage) return;

        const prevPageNum = (
            activePos.indexInPage !== 0 ? (
                activePage.number // don't change the page
            ) : !activePage.first ? (
                activePage.number - 1 // decrement pageNum if current page isn't first
            ) : (
                activePage.totalPages - 1 // start from last page if current page is first
            )
        );
        const prevPage = pageCache.current[prevPageNum];
        if (!prevPage) return; // handle initial state

        const prevIndexInPage = (
            activePos.indexInPage !== 0 ? activePos.indexInPage - 1 : prevPage.numberOfElements - 1
        );

        if (prevPage.totalPages > 1 && (
            prevIndexInPage === Math.min(PREFETCH_THRESHOLD, prevPage.numberOfElements - 1)
        )) {
            const prefetchPageNum = !prevPage.first ? prevPage.number - 1 : activePage.totalPages - 1;
            const prefetchPage = pageCache.current[prefetchPageNum];

            directions.current.set(prefetchPageNum, "backward");
            if (prefetchPage) {
                addPageToQueue(prefetchPage);
            } else if (pageNum !== prefetchPageNum) {
                setPageNum(prefetchPageNum);
            }
        }

        setActivePos({ pageNum: prevPageNum, indexInPage: prevIndexInPage });
    };

    const activeVideoIndex = useMemo(() => {
        let offset = 0;
        for (const page of pageQueue) {
            if (page.number === activePos.pageNum) {
                return offset + activePos.indexInPage;
            }
            offset += page.content.length;
        }
        return 0;
    }, [activePos, pageQueue]);

    const videos = pageQueue.flatMap((page) => {
        if (!page) return [];
        return page.content.map((video, index) => ({ ...video, page, indexInPage: index }));
    });

    const windowData = useMemo(() => {
        if (!videos.length) return { from: 0, items: [] };

        const from = Math.max(activeVideoIndex - PAGE_SIZE, 0);
        const to = Math.min(activeVideoIndex + PAGE_SIZE + 1, videos.length);

        return { from, items: videos.slice(from, to) };
    }, [videos, activeVideoIndex]);

    return (
        <Box
            //ref={containerRef}
            //onMouseDown={onMouseDown}
            //onMouseUp={onMouseUp}
            sx={{
                height: `calc(100dvh - ${BOTTOM_NAV_HEIGHT}px)`,
                overflowY: "auto",
                scrollSnapType: "y mandatory",
                scrollbarWidth: "none",
                "&::-webkit-scrollbar": {
                    display: "none",
                },
            }}
        >
            <Button
                sx={{ position: "absolute", top: 0, left: 0, }}
                variant="contained"
                onClick={prevVideo}
            >
                Prev video
            </Button>
            <Button
                sx={{ position: "absolute", top: 0, right: 0, }}
                variant="contained"
                onClick={nextVideo}
            >
                Next video
            </Button>
            {windowData.items.map(({ page, uuid }, i) => {
                const globalIndex = windowData.from + i;

                return (
                    <VideoCard
                        key={uuid}
                        uuid={`${globalIndex} (page=${page.number}, ${
                            globalIndex === activeVideoIndex ? "active" : ""
                        })`}
                        color={COLORS[globalIndex % COLORS.length]}
                    />
                );
            })}
        </Box>
    );
};