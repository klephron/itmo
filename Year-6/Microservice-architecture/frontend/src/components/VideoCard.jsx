import { useNavigate } from "react-router";
import { Card, CardActionArea, Skeleton, Stack, Typography } from "@mui/material";
import { formatDate, formatLikes, formatViews } from "../utils/format.js";

export const VideoCard = ({ video, onClick, elevation = 1 }) => {
    const navigate = useNavigate();

    return (
        <Card
            variant="outlined"
            sx={(theme) => ({
                borderRadius: 3,
                flexShrink: 0,
                backgroundImage: theme.vars.overlays[elevation],
            })}
        >
            <CardActionArea component="div" onClick={onClick}>
                <Stack spacing={0.5} py={1} px={1.5}>
                    <Typography
                        variant="body2"
                        sx={{
                            whiteSpace: 'pre-wrap',      // сохраняем переносы строк + разрешаем wrap
                            overflowWrap: 'break-word',  // перенос по словам, если можно
                            wordBreak: 'break-word',     // перенос по буквам, если слово слишком длинное
                        }}
                    >
                        {!video ? <Skeleton animation="wave"/> : video.name}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                        {!video ? <Skeleton animation="wave" width="70%"/> : (
                            <>
                                <Typography
                                    variant="caption"
                                    fontWeight="medium"
                                    lineHeight={1}
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        navigate(`/profile/${video.uploaderId}`)
                                    }}
                                    onMouseDown={(e) => e.stopPropagation()}
                                    sx={{ ":hover": { color: "text.primary" } }}
                                >
                                    {video.uploaderName}
                                </Typography>
                                {video.viewsCount && ` · ${video.viewsCount} ${formatViews(video.viewsCount)}`}
                                {` · ${video.likesCount} ${formatLikes(video.likesCount)}`}
                                {` · ${formatDate(video.uploadedAt)}`}
                            </>
                        )}
                    </Typography>
                </Stack>
            </CardActionArea>
        </Card>
    );
}