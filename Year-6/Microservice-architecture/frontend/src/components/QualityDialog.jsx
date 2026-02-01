import { Box, Button, Stack, Typography } from "@mui/material";
import CheckRoundedIcon from "@mui/icons-material/CheckRounded";
import { SwipeableDialog } from "./SwipeableDialog";

function availableQualities(video) {
    if (!video) return [];

    if (Array.isArray(video.availableQualities) && video.availableQualities.length > 0) {
        return [...new Set(video.availableQualities)].sort((a, b) => a - b);
    }

    if (video.defaultHeight) {
        return [video.defaultHeight];
    }

    return [];
}

function resolveQuality(video) {
    const qualities = availableQualities(video);
    if (!qualities.length) return null;

    if (video.selectedQuality && qualities.includes(video.selectedQuality)) {
        return video.selectedQuality;
    }

    if (video.defaultHeight && qualities.includes(video.defaultHeight)) {
        return video.defaultHeight;
    }

    return qualities[qualities.length - 1];
}

export function QualityDialog({ open, setOpen, video, onSelect }) {
    if (!video) return null;

    const qualities = availableQualities(video);
    const current = resolveQuality(video);

    return (
        <SwipeableDialog
            open={open}
            setOpen={setOpen}
            height="auto"
            title="Настройка качества видео"
        >
            <Stack p={2} pt={0} spacing={2} sx={{ overflowX: "hidden", overflowY: "auto" }}>
                {qualities.length === 0 ? (
                    <Typography>
                        Для этого видео нельзя выбрать качество
                    </Typography>
                ) : qualities.map((q) => {
                    const selected = q === current;

                    return (
                        <Button
                            key={q}
                            variant={selected ? "contained" : "outlined"}
                            color={selected ? "primary" : "inherit"}
                            onClick={() => {
                                if (selected) return;
                                onSelect?.(q)
                            }}
                        >
                            <Box sx={{ position: "relative" }}>
                                {selected && (
                                    <CheckRoundedIcon fontSize="small" sx={(theme) => ({
                                        position: "absolute",
                                        left: `calc(-1 * ${theme.spacing(1 / 2)})`,
                                        transform: "translateX(-100%)",
                                    })}/>
                                )}
                                {q}p
                            </Box>
                        </Button>
                    );
                })}
            </Stack>
        </SwipeableDialog>
    );
}