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
            <Stack p={2} pt={0} spacing={1.5} sx={{ overflowX: "hidden", overflowY: "auto" }}>
                <Stack direction="row" justifyContent="space-between">
                    <Typography variant="caption" color="text.secondary">
                        {formatDate(video.uploadedAt)}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                        {humanSize(video?.contentSize)}
                    </Typography>
                </Stack>
                <Typography sx={{
                    whiteSpace: 'pre-wrap',      // сохраняем переносы строк + разрешаем wrap
                    overflowWrap: 'break-word',  // перенос по словам, если можно
                    wordBreak: 'break-word',     // перенос по буквам, если слово слишком длинное
                }}>
                    {video.description ?? "Описание отсутствует"}
                </Typography>
            </Stack>
        </SwipeableDialog>
    );
}