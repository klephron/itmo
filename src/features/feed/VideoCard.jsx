import { Box, Typography } from "@mui/material";

export const VideoCard = ({ index, color }) => {
    return (
        <Box
            sx={{
                height: 1,
                scrollSnapAlign: "start",
                scrollSnapStop: "always",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                backgroundColor: color,
                color: "#fff",
                fontSize: 32,
                fontWeight: 700,
            }}
        >
            <Typography variant="h3">
                Video #{index + 1}
            </Typography>
        </Box>
    );
};