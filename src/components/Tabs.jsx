import { Button, Paper, Stack } from "@mui/material";

import HomeRoundedIcon from "@mui/icons-material/HomeRounded";
import AccountCircleRoundedIcon from "@mui/icons-material/AccountCircleRounded";
import ChevronLeftRoundedIcon from "@mui/icons-material/ChevronLeftRounded";
import ChevronRightRoundedIcon from "@mui/icons-material/ChevronRightRounded";
import AddRoundedIcon from "@mui/icons-material/AddRounded";

export default function Tabs({ activeTab, onTab, showModeration, isAuthenticated, feedControlsRef }) {
    const handlePrevClick = () => feedControlsRef.current?.goPrev();
    const handleNextClick = () => feedControlsRef.current?.goNext();

    return (
        <Paper elevation={0}>
            <Stack
                direction="row"
                justifyContent="space-between"
                alignItems="center"
                spacing={1}
                px={1}
                py={1 / 2}
            >
                <Button
                    variant="contained"
                    size="uniSmall"
                    color="inherit"
                    onClick={handlePrevClick}
                    disabled={activeTab !== "feed"}
                >
                    <ChevronLeftRoundedIcon sx={{ fontSize: 32 }}/>
                </Button>
                <Button
                    variant="contained"
                    size="uniSmall"
                    color={activeTab === "feed" ? "primary" : "inherit"}
                    onClick={() => onTab("feed")}
                >
                    <HomeRoundedIcon sx={{ fontSize: 32 }}/>
                </Button>
                <Button
                    variant="contained"
                    size="uniSmall"
                    color={activeTab === "upload" ? "primary" : "inherit"}
                    onClick={() => onTab("upload")}
                >
                    <AddRoundedIcon sx={{ fontSize: 40 }}/>
                </Button>
                {isAuthenticated ? (
                    <Button
                        variant="contained"
                        size="uniSmall"
                        color={activeTab === "account" ? "primary" : "inherit"}
                        onClick={() => onTab("account")}
                    >
                        {/* TODO: Avatar */}
                        <AccountCircleRoundedIcon sx={{ fontSize: 32 }}/>
                    </Button>
                ) : (
                    <Button
                        variant="contained"
                        size="uniSmall"
                        color={activeTab === "auth" ? "primary" : "inherit"}
                        onClick={() => onTab("auth")}
                    >
                        <AccountCircleRoundedIcon sx={{ fontSize: 32 }}/>
                    </Button>
                )}
                <Button
                    variant="contained"
                    size="uniSmall"
                    color="inherit"
                    onClick={handleNextClick}
                    disabled={activeTab !== "feed"}
                >
                    <ChevronRightRoundedIcon sx={{ fontSize: 32 }}/>
                </Button>
            </Stack>
        </Paper>
    );
}