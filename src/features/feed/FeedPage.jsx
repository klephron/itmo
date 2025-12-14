/* eslint-disable react-hooks/refs */
import { Box, Button, Stack, Typography } from "@mui/material";
import { useMemo, useRef, useState } from "react";
import { useApi } from "@/hooks";
import { api } from "@/services/api.js";

const COLORS = [
    "#ff5252", "#ff9800", "#ffeb3b", "#4caf50", "#00bcd4",
    "#2196f3", "#673ab7", "#e91e63", "#9c27b0", "#795548",
    "#3f51b5", "#009688", "#8bc34a", "#ffc107", "#f44336",
];

const PAGE_SIZE = 3;
const DRAG_THRESHOLD = 30;
const PREFETCH_THRESHOLD = 1;

export const FeedPage = () => {
    const [pageNum, setPageNum] = useState(0);
    const { data } = useApi(api.getVideosPage, pageNum, PAGE_SIZE);

    const pageCache = useRef({});
    const pageQueue = useRef([]);
    const direction = useRef("forward");

    const [activePos, setActivePos] = useState({
        pageNum: 0,
        indexInPage: 0,
    });

    const addPageToQueue = (pageNum) => {
        const index = pageQueue.current.indexOf(pageNum);
        if (index !== -1) {
            pageQueue.current.splice(index, 1);
        }

        if (direction.current === "backward") {
            pageQueue.current.unshift(pageNum);
        } else {
            pageQueue.current.push(pageNum);
        }
    };

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

        direction.current = "forward";
        if (nextPage.totalPages > 1 && (
            nextIndexInPage === Math.max(nextPage.numberOfElements - 1 - PREFETCH_THRESHOLD, 0)
        )) {
            const prefetchPageNum = !nextPage.last ? nextPage.number + 1 : 0;
            if (pageCache.current[prefetchPageNum]) {
                addPageToQueue(prefetchPageNum);
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

        direction.current = "backward";
        if (prevPage.totalPages > 1 && (
            prevIndexInPage === Math.min(PREFETCH_THRESHOLD, prevPage.numberOfElements - 1)
        )) {
            const prefetchPageNum = !prevPage.first ? prevPage.number - 1 : activePage.totalPages - 1;
            if (pageCache.current[prefetchPageNum]) {
                addPageToQueue(prefetchPageNum);
            } else if (pageNum !== prefetchPageNum) {
                setPageNum(prefetchPageNum);
            }
        }

        setActivePos({ pageNum: prevPageNum, indexInPage: prevIndexInPage });
    };

    const activeVideoIndex = useMemo(() => {
        let offset = 0;
        for (const p of pageQueue.current) {
            const page = pageCache.current[p];
            if (!page) continue;

            if (p === activePos.pageNum) {
                return offset + activePos.indexInPage;
            }
            offset += page.content.length;
        }
        return 0;
    }, [activePos]);

    if (data && !pageCache.current[data.number]) {
        pageCache.current[data.number] = data;
        addPageToQueue(data.number);
    }

    const videos = pageQueue.current.flatMap((pageNum) => {
        const page = pageCache.current[pageNum];
        if (!page) return [];
        return page.content.map((video, index) => ({
            ...video,
            page,
            indexInPage: index,
        }));
    });

    return (
        <>
            <Button
                variant="contained"
                onClick={prevVideo}
            >
                Prev video
            </Button>
            <Button
                variant="contained"
                onClick={nextVideo}
            >
                Next video
            </Button>
            <Stack>
                {videos?.map(({ page }, index) => (
                    <div key={index}>
                        {`${index} (page = ${page.number}, ${activeVideoIndex === index ? "active" : ""})`}
                    </div>
                ))}
            </Stack>
        </>
    );

    /*
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
    */
};