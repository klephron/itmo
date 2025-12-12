import { useApi } from "@/hooks";
import { api } from "@/services/api.js";
import { useState } from "react";
import { Button } from "@mui/material";

export const FeedPage = () => {
    const [pageNum, setPageNum] = useState(0);
    const [pageSize, setPageSize] = useState(10);

    const videoPage = useApi(api.getVideosPage, pageNum, pageSize);
    console.log(videoPage.loading);
    //console.log(videoPage.data);

    return (
        <>
            Loading: {videoPage.loading}
            <Button onClick={() => setPageSize((prevState) => prevState + 1)}>Click me</Button>
        </>
    );
}