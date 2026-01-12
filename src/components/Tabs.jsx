import { Avatar, Button, Paper, Stack } from "@mui/material";

import HomeRoundedIcon from "@mui/icons-material/HomeRounded";
import ChevronLeftRoundedIcon from "@mui/icons-material/ChevronLeftRounded";
import ChevronRightRoundedIcon from "@mui/icons-material/ChevronRightRounded";
import AddRoundedIcon from "@mui/icons-material/AddRounded";

import { useAuth } from "../context/AuthContext.jsx";
import { getAvatarUrl } from "../api/api.js";

export default function Tabs({ activeTab, onTab, feedMode, feedControlsRef }) {
    const { user } = useAuth();

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
                disabled={activeTab !== "feed" || feedMode.type === "moderation"}
            >
                <ChevronLeftRoundedIcon sx={{ fontSize: 32 }}/>
            </Button>
            <Button
                variant={activeTab === "feed" && feedMode.type === "global" ? "contained" : "text"}
                size="uniSmall"
                color={activeTab === "feed" && feedMode.type === "global" ? "primary" : "inherit"}
                onClick={() => onTab("feed", { feedMode: { type: "global" } })}
            >
                <HomeRoundedIcon sx={{
                    fontSize: 32,
                    color: activeTab === "feed" && feedMode.type === "global" ? "inherit" : "text.secondary"
                }}/>
            </Button>
            <Button
                variant="contained"
                size="uniSmall"
                color={activeTab === "upload" ? "primary" : "inherit"}
                onClick={() => onTab("upload")}
            >
                <AddRoundedIcon sx={{ fontSize: 40 }}/>
            </Button>
            {user ? (
                <Button
                    variant={activeTab === "account" ? "contained" : "text"}
                    size="uniSmall"
                    color={activeTab === "account" ? "primary" : "inherit"}
                    onClick={() => onTab("account")}
                >
                    <Avatar
                        src={user.avatarUrl}
                        sx={{ width: 32, height: 32 }}
                    >
                        {user.username?.[0]?.toUpperCase()}
                    </Avatar>
                </Button>
            ) : (
                <Button
                    variant={activeTab === "auth" ? "contained" : "text"}
                    size="uniSmall"
                    color={activeTab === "auth" ? "primary" : "inherit"}
                    onClick={() => onTab("auth")}
                >
                    <Avatar sx={{
                        width: 32,
                        height: 32,
                        bgcolor: activeTab === "auth" ? "background.paper" : "text.secondary",
                        color: activeTab === "auth" ? "text.primary" : "background.paper",
                    }}/>
                </Button>
            )}
            <Button
                size="uniSmall"
                color="inherit"
                onClick={handleNextClick}
                disabled={activeTab !== "feed" || feedMode.type === "moderation"}
            >
                <ChevronRightRoundedIcon sx={{ fontSize: 32 }}/>
            </Button>
        </Stack>
    );
}