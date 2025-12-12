import { Outlet, useLocation } from "react-router";
import { ThemeProvider, CssBaseline, Box, Container } from "@mui/material";
import MainTabs from "./components/Navigation/MainTabs";
import { theme } from "./theme.js";

export default function App() {
    const location = useLocation();
    const hideTabs = location.pathname === "/auth";

    return (
        <ThemeProvider theme={theme} defaultMode="dark" noSsr>
            <CssBaseline/>
            {/* Фуллскрин мобильный контейнер */}
            <Box
                sx={{
                    width: "100vw",
                    height: "100vh",
                    display: "flex",
                    justifyContent: "center",
                    alignItems: "center",
                    bgcolor: "background.default",
                }}
            >
                {/* Центрированная мобильная колонка */}
                <Box
                    sx={{
                        width: "100%",
                        maxWidth: 480,
                        height: "100%",
                        display: "flex",
                        flexDirection: "column",
                        position: "relative",
                        overflow: "hidden",
                        bgcolor: "background.paper",
                    }}
                >
                    {/* Контентная зона */}
                    <Container
                        disableGutters
                        maxWidth={false}
                        sx={{
                            flex: 1,
                            overflowY: "auto",
                            overflowX: "hidden",
                            px: 0, // убираем отступы (для full-screen видео)
                        }}
                    >
                        <Outlet/>
                    </Container>

                    {/* Навигация */}
                    {!hideTabs && (
                        <Box
                            sx={{
                                position: "sticky",
                                bottom: 0,
                                width: "100%",
                                zIndex: 100,
                            }}
                        >
                            <MainTabs/>
                        </Box>
                    )}
                </Box>
            </Box>
        </ThemeProvider>
    );
}