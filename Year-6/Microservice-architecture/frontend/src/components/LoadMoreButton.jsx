import { CircularProgress, Fab } from "@mui/material";
import PlayForWorkRoundedIcon from '@mui/icons-material/PlayForWorkRounded';

export const LoadMoreButton = ({ loading, onClick, sx }) => {
    return (
        <Fab
            variant="extended"
            size="small"
            sx={sx}
            onClick={onClick}
            disabled={loading}
        >
            Загрузить еще
            {loading ? (
                <CircularProgress
                    color="inherit"
                    size={16}
                    sx={{ ml: 1, position: 'relative', left: 4 }}
                />
            ) : (
                <PlayForWorkRoundedIcon sx={{ position: 'relative', left: 6 }}/>
            )}
        </Fab>
    )
}