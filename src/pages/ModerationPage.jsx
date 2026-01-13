import { NavLink, Outlet, useNavigate } from "react-router";
import { Button, Grid, IconButton, Paper, Stack, Typography } from "@mui/material";
import ArrowBackRoundedIcon from "@mui/icons-material/ArrowBackRounded";

import { useAuth } from '../context/AuthContext';

export default function ModerationPage() {
    const { token, isModerator, isAdmin } = useAuth();
    const navigate = useNavigate();

    if (!token || !isModerator) {
        return (
            <Paper sx={{ p: 2 }}>
                <Typography variant="body2" color="text.secondary">
                    Нет прав для модерации
                </Typography>
            </Paper>
        );
    }

    return (
        <>
            <Stack component={Paper} direction="row" p={1} alignItems="center">
                <IconButton onClick={() => navigate(-1)} size="small">
                    <ArrowBackRoundedIcon/>
                </IconButton>
                <Typography variant="h6" noWrap px={1}>Модерация</Typography>
            </Stack>
            <Stack component={Paper} pt={2} flex={1} overflow="hidden">
                <Grid container spacing={1} mb={2} px={2}>
                    <NavLink to="users" replace style={{ textDecoration: "none", color: "inherit" }}>
                        {({ isActive }) => (
                            <Button
                                size="small"
                                variant="contained"
                                color={isActive ? "primary" : "inherit"}
                            >
                                Пользователи
                            </Button>
                        )}
                    </NavLink>
                    <NavLink to="complaints" replace style={{ textDecoration: "none", color: "inherit" }}>
                        {({ isActive }) => (
                            <Button
                                size="small"
                                variant="contained"
                                color={isActive ? "primary" : "inherit"}
                            >
                                Жалобы
                            </Button>
                        )}
                    </NavLink>
                    {isAdmin && (
                        <NavLink to="service" replace style={{ textDecoration: "none", color: "inherit" }}>
                            {({ isActive }) => (
                                <Button
                                    size="small"
                                    variant="contained"
                                    color={isActive ? "primary" : "inherit"}
                                >
                                    Сервис
                                </Button>
                            )}
                        </NavLink>
                    )}
                </Grid>
                <Outlet/>
            </Stack>
        </>
    );
}