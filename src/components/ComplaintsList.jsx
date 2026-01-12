import { Box, Button, Card, Grid, IconButton, Stack, Typography } from "@mui/material";
import OpenInNewRoundedIcon from "@mui/icons-material/OpenInNewRounded";

import { formatDate } from "../utils/format.js";

export function ComplaintsList({ complaints, onResolve }) {
    if (!complaints.length) {
        return <Typography variant="body2">Нет открытых жалоб</Typography>;
    }

    return (
        <>
            <Stack direction="row" spacing={1} py={0.5} px={2}>
                <Typography variant="h6" fontWeight="bold">
                    Жалобы
                </Typography>
                <Typography variant="body2" color="text.secondary" pt={1}>
                    {complaints.length}
                </Typography>
            </Stack>

            {!complaints.length ? (
                <Typography mt={1.5} px={2}>
                    Нет открытых жалоб
                </Typography>
            ) : (
                <Box sx={{ borderRadius: 4, flex: 1, overflow: "hidden", m: 2 }}>
                    <Stack spacing={1.5} sx={{ height: 1, overflowX: "hidden", overflowY: "auto" }}>
                        {complaints.map(c => (
                            <Card key={c.id} elevation={1} sx={{ flexShrink: 0 }}>
                                <Stack spacing={1.5} p={2} pt={1.5}>
                                    <Box>
                                        <Typography fontWeight="bold" position="relative" pr={6}>
                                            Жалоба на {c.targetType === "VIDEO" ? "видео" : "комментарий"}
                                            <IconButton
                                                size="small"
                                                sx={{
                                                    ml: 1.5,
                                                    fontSize: "inherit",
                                                    position: "absolute",
                                                }}
                                            >
                                                <OpenInNewRoundedIcon fontSize="inherit"/>
                                            </IconButton>
                                        </Typography>
                                        <Typography
                                            component="p"
                                            variant="caption"
                                            color="text.secondary"
                                        >
                                            {formatDate(c.createdAt)}
                                        </Typography>
                                    </Box>

                                    <Typography variant="body2">
                                        {c.reason}
                                    </Typography>

                                    <Grid container spacing={1} pt={0.5}>
                                        <Button
                                            variant="contained"
                                            color="inherit"
                                            onClick={() => onResolve(c.id, false, true)}
                                        >
                                            Отклонить
                                        </Button>
                                        <Button
                                            variant="contained"
                                            onClick={() => onResolve(c.id, false, false)}
                                        >
                                            Принять
                                        </Button>
                                        <Button
                                            variant="contained"
                                            color="error"
                                            onClick={() => onResolve(c.id, true, false)}
                                        >
                                            Удалить {c.targetType === "VIDEO" ? "видео" : "комментарий"}
                                        </Button>
                                    </Grid>
                                </Stack>
                            </Card>
                        ))}
                    </Stack>
                </Box>
            )}
        </>
    );
}