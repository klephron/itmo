import { Box, Button, Stack, Typography } from "@mui/material";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useApi } from "@/hooks";
import { api } from "@/services/api.js";

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
};