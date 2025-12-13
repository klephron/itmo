import { createTheme } from "@mui/material/styles";
import { ruRU } from "@mui/material/locale";

// Выбранный оранжевый primary (можем поменять)
const PURPLE_PRIMARY = {
    main: "#C95CFF",
    light: "#DE8CFF",
    dark: "#9A26CC",
    contrastText: "#ffffff",
};

export const BOTTOM_NAV_HEIGHT = 64;

export const theme = createTheme({
        colorSchemeSelector: "class",
        colorSchemes: {
            light: {
                palette: {
                    mode: "light",
                    primary: PURPLE_PRIMARY,
                    background: {
                        default: "#F5F3F8", // серый с лёгким violet tint (~2%)
                        paper: "#FBFAFD",   // почти белый, но не чистый
                    },
                    text: {
                        primary: "#111111",
                        secondary: "#555555",
                    },
                },
            },
            dark: {
                palette: {
                    mode: "dark",
                    primary: PURPLE_PRIMARY,
                    background: {
                        default: "#000000", // чистый
                        paper: "#0A0A0D",   // почти черный, 1–2% violet tint
                    },
                    text: {
                        primary: "#FFFFFF",
                        secondary: "#AAAAAA",
                    },
                },
            },
        },
        // Общие настройки (подходят для обеих схем)
        components: {
            MuiBottomNavigation: {
                styleOverrides: {
                    root: {
                        height: BOTTOM_NAV_HEIGHT,
                    },
                },
            },
            MuiBottomNavigationAction: {
                styleOverrides: {
                    root: {
                        padding: 0,
                        minWidth: 0,
                        flexGrow: 1,
                        position: "relative",

                        // правильный круглый ripple по центру
                        "& .MuiTouchRipple-root": {
                            borderRadius: "50%",
                            width: 48,
                            height: 48,
                            position: "absolute",
                            top: "50%",
                            left: "50%",
                            transform: "translate(-50%, -50%)",
                            overflow: "hidden",
                        },

                        "& .MuiTouchRipple-child": {
                            borderRadius: "50%",
                        },

                        "& .MuiBottomNavigationAction-label": {
                            display: "none",
                        },
                    },
                },
            },
        }


    },
    ruRU
);