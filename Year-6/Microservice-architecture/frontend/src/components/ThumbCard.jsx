import { Box, Button, Card, CardActionArea, Stack, Typography } from "@mui/material";
import DeleteOutlineRoundedIcon from "@mui/icons-material/DeleteOutlineRounded";
import { apiBase } from "../api/api.js";
import { formatCompactNumber } from "../utils/format.js";
import VisibilityRoundedIcon from "@mui/icons-material/VisibilityRounded";
import FavoriteRoundedIcon from "@mui/icons-material/FavoriteRounded";

export const ThumbCard = ({ video, showDelete, onDelete, onOpen }) => {
    const thumbnailUrl = video?.thumbnailUrl ? `${apiBase}${video.thumbnailUrl}` : null;

    return (
        <Card sx={{
            position: "relative",
            aspectRatio: "9 / 16",
            bgcolor: "Skeleton.bg",
            borderRadius: 0,
        }}>
            <CardActionArea component="div" onClick={onOpen} sx={{ height: 1, width: 1 }}>
                {thumbnailUrl && (
                    <>
                        <Box
                            component="img"
                            src={thumbnailUrl}
                            alt={video?.name || ""}
                            sx={{
                                width: 1,
                                height: 1,
                                objectFit: "contain",
                                bgcolor: "black",
                                display: "block",
                            }}
                        />
                        <Stack
                            direction="row"
                            sx={(theme) => ({
                                width: 1,
                                px: 1.5,
                                position: 'absolute',
                                bottom: theme.spacing(1),
                                zIndex: 1,
                                filter: 'drop-shadow(1px 1px 3px rgb(0,0,0)) drop-shadow(2px 2px 6px rgb(0,0,0))',
                                alignItems: 'center',
                                justifyContent: 'center',
                                flexWrap: 'wrap',
                                rowGap: 1,
                                columnGap: 1.5
                            })}
                        >
                            <Typography variant="body2">
                                <VisibilityRoundedIcon sx={{
                                    fontSize: "inherit",
                                    mr: 0.5,
                                    verticalAlign: "text-top"
                                }}/>
                                {formatCompactNumber(video?.viewsCount)}
                            </Typography>
                            <Typography variant="body2">
                                <FavoriteRoundedIcon sx={{
                                    fontSize: "inherit",
                                    mr: 0.5,
                                    verticalAlign: "text-top"
                                }}/>
                                {formatCompactNumber(video?.likesCount)}
                            </Typography>
                        </Stack>
                        {showDelete && (
                            <Button
                                variant="contained"
                                color="error"
                                size="uniSmall"
                                onClick={(e) => {
                                    e.stopPropagation();
                                    onDelete?.();
                                }}
                                onMouseDown={(e) => e.stopPropagation()}
                                sx={(theme) => ({
                                    position: "absolute",
                                    top: theme.spacing(1 / 2),
                                    right: theme.spacing(1 / 2),
                                    boxShadow: 6,
                                    zIndex: "fab",
                                })}
                            >
                                <DeleteOutlineRoundedIcon fontSize="small"/>
                            </Button>
                        )}
                    </>
                )}
            </CardActionArea>
        </Card>
    );
}