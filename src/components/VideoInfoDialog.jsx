import { Stack, Typography } from "@mui/material";
import { SwipeableDialog } from "./SwipeableDialog";
import { formatDate, humanSize } from "../utils/format.js";

export function VideoInfoDialog({ open, setOpen, video }) {
    if (!video) return null;

    return (
        <SwipeableDialog
            open={open}
            setOpen={setOpen}
            title={video.name || "Без названия"}
        >
            <Stack direction="row" justifyContent="space-between">
                <Typography variant="caption" color="text.secondary">
                    {formatDate(video.uploadedAt)}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                    {humanSize(video?.contentSize)}
                </Typography>
            </Stack>
            <Typography whiteSpace="pre-line">
                {video.description ?? "Описание отсутствует."}
            </Typography>
        </SwipeableDialog>
    );
}