import { memo, useEffect, useRef } from "react";
import { Box, Typography } from "@mui/material";

export const VideoCard = memo(({ index, uuid, color, onEnterView }) => {
    //console.log(uuid);

    const ref = useRef(null);

    const observerRef = useRef(null);
    const callbackRef = useRef(onEnterView);

    // keep callback updated
    useEffect(() => {
        callbackRef.current = onEnterView;
    }, [onEnterView]);

    useEffect(() => {
        if (!ref.current) return;

        // create observer only once
        observerRef.current = new IntersectionObserver(
            ([entry]) => {
                if (entry.isIntersecting && entry.intersectionRatio > 0.5) {
                    //console.log("Video in view:", index);
                    //callbackRef.current?.();
                }
            },
            { threshold: 0.5 }
        );

        observerRef.current.observe(ref.current);

        return () => {
            observerRef.current?.disconnect();
        };
    }, [index]);

    return (
        <Box
            ref={ref}
            sx={{
                height: 1,
                scrollSnapAlign: "start",
                scrollSnapStop: "always",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                backgroundColor: color,
                color: "#fff",
                fontSize: 32,
                fontWeight: 700,
            }}
        >
            <Typography variant="h3">
                Video #{uuid}
            </Typography>
        </Box>
    );
});