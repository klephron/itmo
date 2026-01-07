import { Dialog, DialogContent, IconButton, Stack, Typography } from "@mui/material";
import CloseRoundedIcon from "@mui/icons-material/CloseRounded";

export function SmallDialog({ open, setOpen, title, children }) {
    const handleClose = () => setOpen(false);

    return (
        <Dialog
            open={open}
            onClose={handleClose}
            fullWidth
            maxWidth={false}
            slotProps={{
                paper: {
                    sx: (theme) => ({
                        width: "100%",
                        maxWidth: `calc(480px - ${theme.spacing(4)})`,
                        borderRadius: 4,
                        m: 2,
                        display: "flex",
                        flexDirection: "column",
                    }),
                },
            }}
        >
            <DialogContent
                sx={{
                    p: 0,
                    display: "flex",
                    flexDirection: "column",
                }}
            >
                <Stack direction="row" spacing={3} px={2} py={1.5} alignItems="center" justifyContent="space-between">
                    <Typography variant="h6" fontWeight="bold">{title}</Typography>
                    <IconButton size="small" onClick={handleClose}>
                        <CloseRoundedIcon fontSize="small"/>
                    </IconButton>
                </Stack>
                {children}
            </DialogContent>
        </Dialog>
    );
}