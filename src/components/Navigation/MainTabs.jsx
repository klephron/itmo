import { useLocation, useNavigate } from "react-router";
import { BottomNavigation, BottomNavigationAction, Paper, } from "@mui/material";

import HomeRoundedIcon from "@mui/icons-material/HomeRounded";
import SearchRoundedIcon from "@mui/icons-material/SearchRounded";
import AddCircleRoundedIcon from "@mui/icons-material/AddCircleRounded";
import MailRoundedIcon from "@mui/icons-material/MailRounded";
import AccountCircleRoundedIcon from "@mui/icons-material/AccountCircleRounded";

export default function MainTabs() {
    const location = useLocation();
    const navigate = useNavigate();

    const current = "/" + location.pathname.split("/")[1];

    // TODO: replace with real authentication state
    const isAuthenticated = false;

    const handleNav = (path) => {
        if (!isAuthenticated && ["/upload", "/profile", "/messages"].includes(path)) {
            navigate("/auth");
            return;
        }
        navigate(path);
    };

    return (
        <Paper
            elevation={8}
            sx={{
                position: "sticky",
                bottom: 0,
                left: 0,
                right: 0,
                borderRadius: 0,
            }}
        >
            <BottomNavigation
                showLabels={false}
                value={current}
            >
                <BottomNavigationAction
                    value="/feed"
                    icon={<HomeRoundedIcon sx={{ fontSize: 32 }}/>}
                    onClick={() => handleNav("/feed")}
                />
                <BottomNavigationAction
                    value="/search"
                    icon={<SearchRoundedIcon sx={{ fontSize: 32 }}/>}
                    onClick={() => handleNav("/search")}
                />
                <BottomNavigationAction
                    value="/upload"
                    icon={<AddCircleRoundedIcon sx={{ fontSize: 40 }}/>}
                    onClick={() => handleNav("/upload")}
                />
                <BottomNavigationAction
                    value="/messages"
                    icon={<MailRoundedIcon sx={{ fontSize: 32 }}/>}
                    onClick={() => handleNav("/messages")}
                />
                <BottomNavigationAction
                    value="/profile"
                    icon={<AccountCircleRoundedIcon sx={{ fontSize: 32 }}/>}
                    onClick={() => handleNav("/profile")}
                />
            </BottomNavigation>
        </Paper>
    );
}
