import { forwardRef, useEffect, useRef } from "react";
import { Box, Dialog, DialogContent, Slide, Typography } from "@mui/material";

const Transition = forwardRef(({ children, ...rest }, ref) => (
    <Slide direction="up" ref={ref} {...rest}>
        {children}
    </Slide>
));

export function SwipeableDialog({ open, setOpen, title, children }) {
    const paperRef = useRef(null);

    const startXRef = useRef(null);
    const startYRef = useRef(null);
    const deltaYRef = useRef(0);

    // единое состояние жеста
    const gestureRef = useRef("idle"); // "idle" | "drag" | "ignore"

    const handleClose = () => setOpen(false);

    /* =========================
       Gesture handlers
       ========================= */

    const start = (x, y) => {
        // если есть выделение текста — не перехватываем
        const sel = window.getSelection?.();
        if (sel && !sel.isCollapsed) {
            gestureRef.current = "ignore";
            return;
        }

        startXRef.current = x;
        startYRef.current = y;
        deltaYRef.current = 0;
        gestureRef.current = "idle";
    };

    const move = (x, y) => {
        if (startXRef.current == null || startYRef.current == null) return;

        const dx = x - startXRef.current;
        const dy = y - startYRef.current;

        // ещё не определились
        if (gestureRef.current === "idle") {
            const absX = Math.abs(dx);
            const absY = Math.abs(dy);

            if (absX < 6 && absY < 6) return;

            if (absY > absX * 1.5) {
                // вертикальный drag
                gestureRef.current = "drag";

                document.body.style.userSelect = "none";
                if (paperRef.current) {
                    paperRef.current.style.userSelect = "none";
                }
            } else {
                // горизонталь → игнор
                gestureRef.current = "ignore";
                return;
            }
        }

        if (gestureRef.current !== "drag") return;
        if (dy < 0) return;

        deltaYRef.current = dy;

        paperRef.current.style.transition = "none";
        paperRef.current.style.transform = `translateY(${dy}px)`;
    };

    const end = () => {
        document.body.style.userSelect = "";
        if (paperRef.current) {
            paperRef.current.style.userSelect = "";
        }

        if (gestureRef.current !== "drag") {
            reset();
            return;
        }

        const delta = deltaYRef.current;
        const height = paperRef.current.getBoundingClientRect().height;

        if (delta >= height * 0.4) {
            handleClose();
            return;
        }

        paperRef.current.style.transition = "transform 200ms ease-out";
        paperRef.current.style.transform = "translateY(0)";

        reset();
    };

    const reset = () => {
        startXRef.current = null;
        startYRef.current = null;
        deltaYRef.current = 0;
        gestureRef.current = "idle";
    };

    useEffect(() => {
        if (!open) {
            document.body.style.userSelect = "";
            if (paperRef.current) {
                paperRef.current.style.userSelect = "";
            }
            reset();
        }
    }, [open]);

    return (
        <Dialog
            open={open}
            onClose={handleClose}
            fullWidth
            maxWidth={false}
            slots={{ transition: Transition }}
            slotProps={{
                container: {
                    sx: { alignItems: "flex-end" },
                },
                paper: {
                    ref: paperRef,
                    sx: {
                        width: "100%",
                        maxWidth: 480,
                        height: "max(70dvh, min(480px, 100dvh))",
                        borderRadius: "16px 16px 0 0",
                        m: "0 auto",
                        display: "flex",
                        flexDirection: "column",
                        willChange: "transform",
                    },
                    onMouseDown: (e) => start(e.clientX, e.clientY),
                    onMouseMove: (e) => move(e.clientX, e.clientY),
                    onMouseUp: end,
                    onMouseLeave: () => {
                        if (gestureRef.current === "drag") end();
                    },
                    onTouchStart: (e) => {
                        const t = e.touches[0];
                        start(t.clientX, t.clientY);
                    },
                    onTouchMove: (e) => {
                        const t = e.touches[0];
                        move(t.clientX, t.clientY);
                    },
                    onTouchEnd: end,
                },
            }}
        >
            <DialogContent
                sx={{
                    p: 0,
                    position: "relative",
                    display: "flex",
                    flexDirection: "column",
                }}
            >
                <Box
                    sx={{
                        position: "absolute",
                        top: 8,
                        left: "50%",
                        transform: "translateX(-50%)",
                        width: 48,
                        height: 4,
                        borderRadius: 2,
                        bgcolor: "text.disabled",
                        pointerEvents: "none",
                    }}
                />
                {title && (
                    <Typography variant="h6" fontWeight="bold" px={2} pt={3} pb={1.5}>
                        {title}
                    </Typography>
                )}
                {children}
            </DialogContent>
        </Dialog>
    );
}