import { Box } from "@mui/material";
import { VideoCard } from "./VideoCard.jsx";
import { BOTTOM_NAV_HEIGHT } from "@/theme.js";
import { useRef } from "react";

const COLORS = [
    "#ff5252",
    "#ff9800",
    "#ffeb3b",
    "#4caf50",
    "#00bcd4",
    "#2196f3",
    "#673ab7",
    "#e91e63",
    "#9c27b0",
    "#795548",
    "#3f51b5",
    "#009688",
    "#8bc34a",
    "#ffc107",
    "#f44336",
    "#607d8b",
    "#cddc39",
    "#ff5722",
    "#03a9f4",
    "#9e9e9e",
];
const DRAG_THRESHOLD = 30;

export const FeedPage = () => {
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
                "&::-webkit-scrollbar": {
                    display: "none",
                },
            }}
        >
            {COLORS.map((color, index) => (
                <VideoCard
                    key={index}
                    index={index}
                    color={color}
                />
            ))}
        </Box>
    );
};