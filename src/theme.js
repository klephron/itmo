import { createTheme, inputLabelClasses, outlinedInputClasses, selectClasses } from "@mui/material";
import { grey } from "@mui/material/colors";
import { ruRU } from "@mui/material/locale";
import ExpandMoreRoundedIcon from "@mui/icons-material/ExpandMoreRounded";

export const spacing = 8;
export const avatarSize = 56;

export const theme = createTheme(
    {
        spacing,
        cssVariables: {
            colorSchemeSelector: "class"
        },
        colorSchemes: {
            light: {
                palette: {
                    background: {
                        default: grey[200],
                    },
                    primary: {
                        main: '#4489FF',
                    },
                },
            },
            dark: {
                palette: {
                    background: {
                        paper: grey[900],
                    },
                    primary: {
                        main: '#f5f5f5',
                    },
                },
            },
        },
        components: {
            MuiButton: {
                defaultProps: {
                    disableElevation: true,
                },
                styleOverrides: {
                    root: ({ theme }) => ({
                        borderRadius: 999,
                        variants: [
                            {
                                props: { size: 'uniSmall' },
                                style: {
                                    padding: theme.spacing(1 / 2),
                                    minWidth: 'auto',
                                },
                            },
                            {
                                props: { size: 'uniMedium' },
                                style: {
                                    padding: theme.spacing(1),
                                    minWidth: 'auto',
                                },
                            },
                        ]
                    }),
                },
            },
            MuiFab: {
                styleOverrides: {
                    root: ({ theme }) => ({
                        variants: [
                            {
                                props: { size: 'small' },
                                style: {
                                    padding: theme.spacing(2),
                                },
                            },
                        ]
                    }),
                },
            },
            MuiMenu: {
                styleOverrides: {
                    paper: ({ theme }) => ({
                        borderRadius: `calc(4 * ${theme.vars.shape.borderRadius})`,
                    }),
                },
            },
            MuiOutlinedInput: {
                styleOverrides: {
                    root: ({ theme }) => ({
                        borderRadius: `calc(5 * ${theme.vars.shape.borderRadius})`,
                        variants: [
                            {
                                props: { multiline: true },
                                style: {
                                    paddingLeft: theme.spacing(2),
                                    paddingRight: theme.spacing(2),
                                },
                            },
                        ]
                    }),
                    input: ({ theme }) => ({
                        variants: [
                            {
                                props: { multiline: false },
                                style: {
                                    paddingLeft: theme.spacing(2),
                                    paddingRight: theme.spacing(2),
                                },
                            },
                        ]
                    }),
                },
            },
            MuiTextField: {
                styleOverrides: {
                    root: ({ theme }) => ({
                        variants: [
                            {
                                props: { select: true, size: "small" },
                                style: {
                                    [`& .${outlinedInputClasses.root} .${outlinedInputClasses.input}.${selectClasses.select}`]: {
                                        paddingRight: 43,
                                    },
                                    [`.${selectClasses.icon}`]: {
                                        right: 11,
                                    },
                                },
                            },
                        ],
                        [`& .${inputLabelClasses.root}`]: {
                            marginLeft: 2,
                        },
                        [`& .${outlinedInputClasses.notchedOutline} legend`]: {
                            marginLeft: 2,
                        },
                    }),
                },
            },
            MuiPaper: {
                defaultProps: {
                    elevation: 0,
                },
                styleOverrides: {
                    root: ({ theme }) => ({
                        borderRadius: `calc(4 * ${theme.vars.shape.borderRadius})`,
                    }),
                },
            },
            MuiSelect: {
                defaultProps: {
                    IconComponent: ExpandMoreRoundedIcon
                },
            },
            MuiLinearProgress: {
                styleOverrides: {
                    root: ({ theme }) => ({
                        height: theme.spacing(1), // половина spacing
                        borderRadius: theme.spacing(0.5),
                        backgroundColor: theme.vars.palette.divider,
                        overflow: "hidden",
                    }),
                    bar: ({ theme }) => ({
                        borderRadius: theme.spacing(0.5),
                    }),
                },
            },
        },
        typography: {
            fontFamily: 'Rubik Variable',
            button: {
                textTransform: 'none',
            },
        },
    },
    ruRU,
);