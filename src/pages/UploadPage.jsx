import { useRef, useState } from "react";
import { Button, LinearProgress, Paper, Stack, TextField, Typography, } from "@mui/material";
import { apiBase } from "../api/api";
import { useAuth } from "../context/AuthContext";
import Status from "../components/Status";
import { formatDuration, humanSize } from "../utils/format.js";

const MAX_DURATION = 120; // seconds
const MAX_WIDTH = 1080;
const MAX_HEIGHT = 1920;
const MAX_PIXELS = MAX_HEIGHT * MAX_WIDTH;

function readVideoInfo(file) {
    return new Promise((resolve, reject) => {
        const video = document.createElement("video");
        video.preload = "metadata";

        video.onloadedmetadata = () => {
            URL.revokeObjectURL(video.src);
            resolve({
                width: video.videoWidth,
                height: video.videoHeight,
                duration: video.duration, // seconds
            });
        };

        video.onerror = () => reject("Не удалось прочитать видеофайл");
        video.src = URL.createObjectURL(file);
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

export default function UploadPage({ active, onPage }) {
    const { token } = useAuth();
    const inputRef = useRef(null);

    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");

    const [file, setFile] = useState(null);
    const [videoInfo, setVideoInfo] = useState(null);

    const [progress, setProgress] = useState(0);
    const [uploading, setUploading] = useState(false);
    const [uploaded, setUploaded] = useState(false);

    const [status, setStatus] = useState({ msg: "", type: "" });

    const canUpload = !!token && !uploading && !uploaded;

    function setStatusUpload(msg = "", type = "") {
        setStatus({ msg, type });
    }

    function resetForm() {
        setTitle("");
        setDescription("");
        setFile(null);
        setVideoInfo(null);
        setProgress(0);
        setUploading(false);
        setUploaded(false);
        setStatus({ msg: "", type: "" });
        if (inputRef.current) inputRef.current.value = "";
    }

    async function handleFileChange(e) {
        const selected = e.target.files?.[0];
        if (!selected) return;

        try {
            const info = await readVideoInfo(selected);
            const error = validateVideo(selected, info);

            if (error) {
                setStatusUpload(error, "error");
                setFile(null);
                setVideoInfo(null);
                return;
            }

            setFile(selected);
            setVideoInfo(info);
            setStatus({ msg: "", type: "" });
        } catch (err) {
            // если метаданные не удалось прочитать — НЕ блокируем загрузку
            setFile(selected);
            setVideoInfo(null);
            setStatus({
                msg: "Не удалось проверить параметры видео, файл будет загружен без проверки",
                type: "ok",
            });
        }
    }

    function startUpload() {
        if (!token) {
            setStatusUpload("Войдите, чтобы загружать видео", "error");
            onPage("auth");
            return;
        }

        if (!file) {
            setStatusUpload("Выберите файл видео", "error");
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
        setStatusUpload("Начинаю загрузку…", "ok");

        const xhr = new XMLHttpRequest();
        xhr.open("POST", apiBase + "/api/streaming/video/upload", true);
        xhr.setRequestHeader("Authorization", "Bearer " + token);

        xhr.upload.onprogress = (e) => {
            if (e.lengthComputable) {
                const percent = Math.round((e.loaded / e.total) * 100);
                setProgress(percent);
                setStatusUpload(`Загрузка файла… ${percent}%`, "ok");
            }
        };

        xhr.onload = () => {
            setProgress(100);
            setStatusUpload("Файл загружен, началась обработка", "ok");
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
                    setStatusUpload("Сервер не вернул jobId", "error");
                }
            } else if (xhr.status === 401 || xhr.status === 403) {
                setStatusUpload("Сессия истекла, войдите снова", "error");
                onPage("auth");
            } else {
                setStatusUpload(
                    `Ошибка загрузки: HTTP ${xhr.status}`,
                    "error"
                );
            }
        };

        xhr.onerror = () => {
            setStatusUpload("Сетевая ошибка при загрузке", "error");
            setUploading(false);
        };

        xhr.send(formData);
    }

    if (!active) return null;

    return (
        <>
            <Stack component={Paper} direction="row" p={1}>
                <Typography variant="h6" px={1}>
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
                    disabled={!canUpload || !file}
                >
                    Загрузить
                </Button>

                <Status message={status.msg} type={status.type}/>

                <LinearProgress
                    variant="determinate"
                    value={progress}
                    color="success"
                />

                {uploaded && (
                    <Stack spacing={1}>
                        <Stack direction="row" spacing={2}>
                            <Button fullWidth variant="contained" color="inherit" onClick={resetForm}>
                                Загрузить еще
                            </Button>
                            <Button
                                fullWidth
                                variant="contained"
                                onClick={() => onPage("account", { activeAccountTab: "uploads" })}
                            >
                                К списку загрузок
                            </Button>
                        </Stack>
                    </Stack>
                )}
            </Stack>
        </>
    );
}