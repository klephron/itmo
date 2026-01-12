import { useAuth } from '../context/AuthContext';
import { Button, Grid, IconButton, Paper, Stack, Typography } from "@mui/material";
import ArrowBackRoundedIcon from "@mui/icons-material/ArrowBackRounded";

import { UsersList } from "../components/UsersList.jsx";
import { ComplaintsList } from "../components/ComplaintsList.jsx";
import { Service } from "../components/Service.jsx";

export default function ModerationPage({ active, activeTab, setActiveTab, onPage }) {
    const { token, isModerator, isAdmin } = useAuth();

    if (!active) return null;

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
                <IconButton onClick={() => onPage("account")} size="small">
                    <ArrowBackRoundedIcon/>
                </IconButton>
                <Typography variant="h6" noWrap px={1}>Модерация</Typography>
            </Stack>
            <Stack component={Paper} pt={2} flex={1} overflow="hidden">
                <Grid container spacing={1} mb={2} px={2}>
                    <Button
                        variant="contained"
                        color={activeTab === "users" ? "primary" : "inherit"}
                        size="small"
                        onClick={() => setActiveTab("users")}
                    >
                        Пользователи
                    </Button>
                    <Button
                        variant="contained"
                        color={activeTab === "complaints" ? "primary" : "inherit"}
                        size="small"
                        onClick={() => setActiveTab("complaints")}
                    >
                        Жалобы
                    </Button>
                    {isAdmin && (
                        <Button
                            variant="contained"
                            color={activeTab === "service" ? "primary" : "inherit"}
                            size="small"
                            onClick={() => setActiveTab("service")}
                        >
                            Сервис
                        </Button>
                    )}
                </Grid>
                {activeTab === 'users' ? (
                    <UsersList onPage={onPage}/>
                ) : activeTab === 'complaints' ? (
                    <ComplaintsList onPage={onPage}/>
                ) : activeTab === 'service' && isAdmin && (
                    <Service onPage={onPage}/>
                )}
            </Stack>
        </>
    );
}