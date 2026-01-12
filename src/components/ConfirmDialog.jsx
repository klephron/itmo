import { Button, Stack, Typography } from "@mui/material";
import { SmallDialog } from "./SmallDialog.jsx";

export const ConfirmDialog = ({ open, setOpen, title, text, critical = false, onReject, onConfirm }) => {
    const handleRejectClick = () => {
        setOpen(false);
        onReject?.();
    }

    const handleConfirmClick = () => {
        setOpen(false);
        onConfirm?.();
    }

    return (
        <SmallDialog
            open={open}
            setOpen={setOpen}
            title={title}
        >
            <Stack p={2} pt={0} spacing={2} sx={{ overflowX: "hidden", overflowY: "auto" }}>
                {typeof text === "string" ? (
                    <Typography whiteSpace="pre-line">{text}</Typography>
                ) : text}
                <Stack direction="row" spacing={2}>
                    <Button
                        variant="contained"
                        color="inherit"
                        sx={(theme) => ({ backgroundImage: theme.vars.overlays[24] })}
                        fullWidth
                        onClick={handleRejectClick}
                    >
                        Нет
                    </Button>
                    <Button
                        variant="contained"
                        color={critical ? "error" : "primary"}
                        fullWidth
                        onClick={handleConfirmClick}
                    >
                        Да
                    </Button>
                </Stack>
            </Stack>
        </SmallDialog>
    );
};