import { useNavigate } from "react-router";
import { Avatar, Card, CardActionArea, Stack, Typography } from "@mui/material";
import { getAvatarUrl } from "../api/api.js";
import { formatDate } from "../utils/format.js";

export const CommentCard = ({ comment, onClick }) => {
    const navigate = useNavigate();

    if (!comment) {
        return <Card variant="outlined">Загрузка комментария…</Card>;
    }

    return (
        <Card
            variant="outlined"
            sx={(theme) => ({
                borderRadius: 3,
                backgroundImage: theme.vars.overlays[1]
            })}
        >
            <CardActionArea component="div" onClick={onClick}>
                <Stack direction="row" spacing={1} p={1}>
                    <Avatar
                        src={getAvatarUrl(comment.authorId)}
                        sx={{ width: 32, height: 32, cursor: 'pointer' }}
                        onClick={(e) => {
                            e.stopPropagation();
                            navigate(`/profile/${comment.authorId}`);
                        }}
                        onMouseDown={(e) => e.stopPropagation()}
                    >
                        {comment.authorName?.[0]?.toUpperCase()}
                    </Avatar>
                    <Stack spacing={1} flex={1} pt={0.25}>
                        <Stack direction="row" spacing={1}>
                            <Typography variant="caption" color="text.secondary" lineHeight={1}>
                                <Typography
                                    component="span"
                                    variant="body2"
                                    fontWeight="medium"
                                    color="textPrimary"
                                    lineHeight={1}
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        navigate(`/profile/${comment.authorId}`);
                                    }}
                                    onMouseDown={(e) => e.stopPropagation()}
                                >
                                    {comment.authorName}
                                </Typography>
                                {` · ${formatDate(comment.createdAt)}`}
                            </Typography>
                        </Stack>
                        <Typography
                            variant="body2"
                            sx={{
                                whiteSpace: 'pre-wrap',      // сохраняем переносы строк + разрешаем wrap
                                overflowWrap: 'break-word',  // перенос по словам, если можно
                                wordBreak: 'break-word',     // перенос по буквам, если слово слишком длинное
                            }}
                        >
                            {comment.text}
                        </Typography>
                    </Stack>
                </Stack>
            </CardActionArea>
        </Card>
    );
}