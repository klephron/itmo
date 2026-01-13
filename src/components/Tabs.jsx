import { NavLink, useLocation } from "react-router";

import { Avatar, Button, Paper, Stack } from "@mui/material";
import HomeRoundedIcon from "@mui/icons-material/HomeRounded";
import ChevronLeftRoundedIcon from "@mui/icons-material/ChevronLeftRounded";
import ChevronRightRoundedIcon from "@mui/icons-material/ChevronRightRounded";
import AddRoundedIcon from "@mui/icons-material/AddRounded";

import { useAuth } from "../context/AuthContext.jsx";

export default function Tabs({ feedMode, feedControlsRef }) {
    const { user } = useAuth();
    const location = useLocation();

    const handlePrevClick = () => feedControlsRef.current?.goPrev();
    const handleNextClick = () => feedControlsRef.current?.goNext();

    return (
        <Stack
            component={Paper}
            direction="row"
            justifyContent="space-between"
            alignItems="center"
            spacing={1}
            px={1}
            py={0.5}
        >
            <Button
                size="uniSmall"
                color="inherit"
                onClick={handlePrevClick}
                disabled={location.pathname !== "/feed" || feedMode.type === "moderation"}
            >
                <ChevronLeftRoundedIcon sx={{ fontSize: 32 }}/>
            </Button>
            <NavLink
                to="/feed"
                style={{ textDecoration: "none", color: "inherit" }}
                draggable={false}
                state={{ feedMode: { type: "global" } }}
            >
                {({ isActive }) => (
                    <Button
                        variant={isActive && feedMode.type === "global" ? "contained" : "text"}
                        size="uniSmall"
                        color={isActive && feedMode.type === "global" ? "primary" : "inherit"}
                    >
                        <HomeRoundedIcon sx={{
                            fontSize: 32,
                            color: isActive && feedMode.type === "global" ? "inherit" : "text.secondary"
                        }}/>
                    </Button>
                )}
            </NavLink>
            <NavLink to="/upload" style={{ textDecoration: "none", color: "inherit" }} draggable={false}>
                {({ isActive }) => (
                    <Button
                        variant="contained"
                        size="uniSmall"
                        color={isActive ? "primary" : "inherit"}
                    >
                        <AddRoundedIcon sx={{ fontSize: 40 }}/>
                    </Button>
                )}
            </NavLink>
            {user ? (
                <NavLink to="/account" style={{ textDecoration: "none", color: "inherit" }}>
                    {({ isActive }) => (
                        <Button
                            variant={isActive ? "contained" : "text"}
                            size="uniSmall"
                            color={isActive ? "primary" : "inherit"}
                        >
                            <Avatar
                                src={user.avatarUrl}
                                sx={{ width: 32, height: 32 }}
                            >
                                {user.username?.[0]?.toUpperCase()}
                            </Avatar>
                        </Button>
                    )}
                </NavLink>
            ) : (
                <NavLink to="/auth" style={{ textDecoration: "none", color: "inherit" }}>
                    {({ isActive }) => (
                        <Button
                            variant={isActive ? "contained" : "text"}
                            size="uniSmall"
                            color={isActive ? "primary" : "inherit"}
                        >
                            <Avatar sx={{
                                width: 32,
                                height: 32,
                                bgcolor: isActive ? "background.paper" : "text.secondary",
                                color: isActive ? "text.primary" : "background.paper",
                            }}/>
                        </Button>
                    )}
                </NavLink>
            )}
            <Button
                size="uniSmall"
                color="inherit"
                onClick={handleNextClick}
                disabled={location.pathname !== "/feed" || feedMode.type === "moderation"}
            >
                <ChevronRightRoundedIcon sx={{ fontSize: 32 }}/>
            </Button>
        </Stack>
    );
}