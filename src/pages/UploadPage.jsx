import { useRef, useState } from "react";
import { Navigate, useNavigate } from "react-router";
import { Box, Button, LinearProgress, Paper, Stack, TextField, Typography, } from "@mui/material";

import { apiBase } from "../api/api";
import { useAuth } from "../context/AuthContext";
import { formatDuration, humanSize } from "../utils/format.js";

const MAX_DURATION = 120; // seconds
const MAX_WIDTH = 1080;
const MAX_HEIGHT = 1920;
const MAX_PIXELS = MAX_HEIGHT * MAX_WIDTH;

function readVideoInfo(file) {
    return new Promise((resolve, reject) => {
        const video = document.createElement("video");
        video.preload = "metadata";
        video.muted = true;

        video.onloadedmetadata = () => {
            URL.revokeObjectURL(video.src);
            resolve({
                width: video.videoWidth,
                height: video.videoHeight,
                duration: video.duration,
            });
        };

        video.onerror = () => {
            URL.revokeObjectURL(video.src);
            reject(new Error("metadata read failed"));
        };

        video.src = URL.createObjectURL(file);
        video.load();
    });
}

function validateVideo(file, info) {
    if (!info) return null;

    if (info.duration > MAX_DURATION) {
        return `Длительность видео не должна превышать ${MAX_DURATION / 60} мин`;
    }

    if (info.width * info.height > MAX_PIXELS) {
        return `Разрешение видео не должно превышать ${MAX_WIDTH}×${MAX_HEIGHT}`;
    }

    return null;
}

export default function UploadPage() {
    const { token } = useAuth();
    const navigate = useNavigate();

    const inputRef = useRef(null);

    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");

    const [file, setFile] = useState(null);
    const [videoInfo, setVideoInfo] = useState(null);

    const [progress, setProgress] = useState(0);
    const [uploading, setUploading] = useState(false);
    const [uploaded, setUploaded] = useState(false);

    const [fileError, setFileError] = useState("");
    const [uploadStatus, setUploadStatus] = useState("");

    const canUpload = !!token && !uploading && !uploaded;

    function resetForm() {
        setTitle("");
        setDescription("");
        setFile(null);
        setVideoInfo(null);
        setProgress(0);
        setUploading(false);
        setUploaded(false);
        setFileError("");
        setUploadStatus("");
        if (inputRef.current) inputRef.current.value = "";
    }

    async function handleFileChange(e) {
        const selected = e.target.files?.[0];
        if (!selected) return;

        try {
            const info = await readVideoInfo(selected);
            const error = validateVideo(selected, info);

            if (error) {
                setFileError(error);
                setFile(null);
                setVideoInfo(null);
                if (inputRef.current) inputRef.current.value = "";
                return;
            }

            setFile(selected);
            setVideoInfo(info);
            setFileError("");
            setUploadStatus("");
        } catch {
            setFileError("Не удалось проверить параметры видео");
            setFile(selected);
            setVideoInfo(null);
        }
    }

    function startUpload() {
        if (!token) {
            setFileError("Войдите, чтобы загружать видео");
            navigate("/auth");
            return;
        }

        if (!file) {
            setFile("Выберите файл видео");
            return;
        }

        const formData = new FormData();
        formData.append("video", file);
        formData.append("videoName", title.trim() || file.name);

        if (description.trim()) {
            formData.append("description", description.trim());
        }

        setUploading(true);
        setUploaded(false);
        setProgress(0);
        setUploadStatus("Начинаю загрузку…");

        const xhr = new XMLHttpRequest();
        xhr.open("POST", apiBase + "/api/streaming/video/upload", true);
        xhr.setRequestHeader("Authorization", "Bearer " + token);

        xhr.upload.onprogress = (e) => {
            if (e.lengthComputable) {
                const percent = Math.round((e.loaded / e.total) * 100);
                setProgress(percent);
                setUploadStatus(`Загрузка файла… ${percent}%`);
            }
        };

        xhr.onload = () => {
            setProgress(100);
            setUploadStatus("Файл загружен, началась обработка");
            setUploaded(true);
            setUploading(false);
        };

        xhr.onreadystatechange = () => {
            if (xhr.readyState !== XMLHttpRequest.DONE) return;

            if (xhr.status >= 200 && xhr.status < 300) {
                let resp = null;
                try {
                    resp = JSON.parse(xhr.responseText);
                } catch {
                }

                if (resp?.jobId) {
                    /*
                    onJobTracked?.(
                        resp.jobId,
                        title.trim() || file.name,
                        null
                    );*/
                } else {
                    setFileError("Сервер не вернул jobId");
                }
            } else if (xhr.status === 401 || xhr.status === 403) {
                setFileError("Сессия истекла, войдите снова");
                navigate("/auth");
            } else {
                setFileError(`Ошибка загрузки: HTTP ${xhr.status}`,);
            }
        };

        xhr.onerror = () => {
            setFileError("Сетевая ошибка при загрузке");
            setUploading(false);
        };

        xhr.send(formData);
    }

    if (!token) return <Navigate to="/auth" replace/>;

    return (
        <>
            <Stack component={Paper} direction="row" p={1}>
                <Typography variant="h6" px={1} py="1px">
                    Загрузка видео
                </Typography>
            </Stack>

            <Stack component={Paper} p={2} spacing={2} flex={1} overflow="hidden">
                <Typography variant="body2">
                    Выберите видео не более {MAX_WIDTH}×{MAX_HEIGHT} и не длиннее {MAX_DURATION / 60} мин.
                    Оно будет автоматически перекодировано в MP4 (H.264).
                </Typography>

                <Button
                    variant="contained"
                    color="inherit"
                    disabled={!canUpload}
                    onClick={() => inputRef.current?.click()}
                >
                    {file ? "Выбрать другой файл" : "Выбрать файл"}
                </Button>
                <input
                    hidden
                    ref={inputRef}
                    type="file"
                    accept="video/*"
                    onChange={handleFileChange}
                />

                <Typography
                    variant="caption"
                    noWrap
                    sx={{
                        width: 1,
                        direction: "rtl",
                        textAlign: "center",
                        color: "text.secondary",
                    }}
                >
                    &lrm;
                    {!file ? (
                        "Видео не выбрано"
                    ) : videoInfo ? (
                        `${file.name} · ${videoInfo.width}×${videoInfo.height} · 
                        ${formatDuration(videoInfo.duration)} · ${humanSize(file.size)}`
                    ) : `${file.name} · ${humanSize(file.size)}`}
                </Typography>

                <TextField
                    placeholder="Как назвать видео?"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    disabled={!canUpload}
                    size="small"
                />

                <TextField
                    placeholder="О чем видео?"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    disabled={!canUpload}
                    size="small"
                />

                <Button
                    variant="contained"
                    onClick={startUpload}
                    disabled={!canUpload || !file || fileError}
                >
                    Загрузить
                </Button>

                {(uploading || uploaded) && (
                    <Box>
                        <Typography variant="caption" color="text.secondary" noWrap>
                            {uploadStatus}
                        </Typography>
                        <LinearProgress
                            variant="determinate"
                            value={progress}
                            color="success"
                            sx={{ mt: 1 }}
                        />
                    </Box>
                )}

                {uploaded && (
                    <Stack spacing={1}>
                        <Stack direction="row" spacing={2}>
                            <Button fullWidth variant="contained" color="inherit" onClick={resetForm}>
                                Загрузить еще
                            </Button>
                            <Button
                                fullWidth
                                variant="contained"
                                onClick={() => navigate("/account/uploads")}
                            >
                                К списку загрузок
                            </Button>
                        </Stack>
                    </Stack>
                )}

                {fileError && (
                    <Typography variant="caption" color="error" textAlign="center">
                        {fileError}
                    </Typography>
                )}
            </Stack>
        </>
    );
}