import { useEffect, useRef, useState } from "react";
import { Avatar, Box, Button, Stack, Typography } from "@mui/material";
import AddAPhotoOutlinedIcon from "@mui/icons-material/AddAPhotoOutlined";

import { SmallDialog } from "./SmallDialog.jsx";
import { apiBase, authHeaders, isUnauthorized } from "../api/api";
import { useAuth } from "../context/AuthContext";
import { humanSize } from "../utils/format.js";

const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
const MAX_DIMENSION = 1024;
const ALLOWED_TYPES = ["image/jpeg", "image/png"];

export function AvatarUploadDialog({ open, setOpen, onSuccess }) {
    const { token, user, setAuth } = useAuth();

    const inputRef = useRef(null);

    const [file, setFile] = useState(null);
    const [previewUrl, setPreviewUrl] = useState(null);
    const [imageInfo, setImageInfo] = useState(null);
    const [status, setStatus] = useState({ msg: "", type: "" });
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (open) {
            setFile(null);
            setImageInfo(null);
            setStatus({ msg: "", type: "" });
            setLoading(false);
            setPreviewUrl(
                user?.avatarUrl ? `${apiBase}${user.avatarUrl}` : null
            );
            if (inputRef.current) inputRef.current.value = "";
        }
    }, [open, user]);

    useEffect(() => {
        return () => {
            if (previewUrl?.startsWith("blob:")) {
                URL.revokeObjectURL(previewUrl);
            }
        };
    }, [previewUrl]);

    function validateFile(file, imageInfo) {
        if (!ALLOWED_TYPES.includes(file.type)) {
            return "Поддерживаются только JPG и PNG";
        }

        if (file.size > MAX_FILE_SIZE) {
            return `Размер файла не должен превышать ${humanSize(MAX_FILE_SIZE)}`;
        }

        if (imageInfo.width > MAX_DIMENSION || imageInfo.height > MAX_DIMENSION) {
            return `Максимальное разрешение — ${MAX_DIMENSION}×${MAX_DIMENSION}`;
        }

        return null;
    }

    function readImageInfo(file) {
        return new Promise((resolve, reject) => {
            const img = new Image();
            img.onload = () => {
                resolve({
                    width: img.width,
                    height: img.height,
                });
            };
            img.onerror = () => reject("Не удалось прочитать изображение");
            img.src = URL.createObjectURL(file);
        });
    }

    async function handleFileChange(e) {
        const selected = e.target.files?.[0];
        if (!selected) return;

        try {
            const info = await readImageInfo(selected);
            const error = validateFile(selected, info);

            if (error) {
                setStatus({ msg: error, type: "error" });
                setFile(null);
                setImageInfo(null);
                return;
            }

            if (previewUrl?.startsWith("blob:")) {
                URL.revokeObjectURL(previewUrl);
            }

            setFile(selected);
            setImageInfo(info);
            setPreviewUrl(URL.createObjectURL(selected));
            setStatus({ msg: "", type: "" });
        } catch (err) {
            setStatus({ msg: err, type: "error" });
            setFile(null);
            setImageInfo(null);
        }
    }

    async function submit() {
        if (!token) {
            setStatus({ msg: "Войдите, чтобы обновлять аватар", type: "error" });
            return;
        }

        if (!file) {
            setStatus({ msg: "Файл не выбран", type: "error" });
            return;
        }

        const fd = new FormData();
        fd.append("avatar", file);

        try {
            setLoading(true);
            setStatus({ msg: "", type: "" });

            const res = await fetch(`${apiBase}/api/users/me/avatar`, {
                method: "POST",
                headers: authHeaders(token),
                body: fd,
            });

            if (isUnauthorized(res)) {
                setStatus({ msg: "Сессия истекла, войдите снова", type: "error" });
                return;
            }

            if (!res.ok) throw new Error("HTTP " + res.status);

            const data = await res.json();
            const avatarUrl = `${data.url}?t=${Date.now()}`;

            setAuth(token, { ...user, avatarUrl });
            onSuccess?.(avatarUrl);
            setOpen(false);
        } catch (e) {
            setStatus({
                msg: "Ошибка загрузки аватара: " + e.message,
                type: "error",
            });
        } finally {
            setLoading(false);
        }
    }

    return (
        <SmallDialog open={open} setOpen={setOpen} title="Обновление аватара">
            <Stack p={2} pt={0} spacing={2} alignItems="center">
                <Button
                    variant="outlined"
                    sx={{
                        p: 0,
                        position: "relative",
                        overflow: "hidden",
                        filter: "blur(0px)",
                    }}
                    onClick={() => inputRef.current?.click()}
                    disabled={loading}
                >
                    <Avatar src={previewUrl} sx={{ width: 96, height: 96, fontSize: 48 }}>
                        {user?.username?.[0]?.toUpperCase()}
                    </Avatar>
                    <Box sx={{
                        p: 0.5,
                        position: "absolute",
                        left: 0,
                        right: 0,
                        bottom: 0,
                        bgcolor: "rgba(255,255,255,0.5)",
                        backdropFilter: "blur(1px)",
                        display: "flex",
                        justifyContent: "center",
                        pointerEvents: "none",
                    }}>
                        <AddAPhotoOutlinedIcon sx={{ fontSize: 20, color: "primary.contrastText" }}/>
                    </Box>
                </Button>
                <input
                    ref={inputRef}
                    type="file"
                    accept={ALLOWED_TYPES.join()}
                    hidden
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
                    {file
                        ? `${file.name} · ${imageInfo?.width}×${imageInfo?.height} · ${humanSize(file.size)}`
                        : "Файл не выбран"}
                </Typography>
                <Typography variant="body2">
                    Выберите изображение в формате JPG или PNG не более {MAX_DIMENSION}×{MAX_DIMENSION} и не
                    более {humanSize(MAX_FILE_SIZE)}
                </Typography>
                {status.msg && (
                    <Typography
                        variant="caption"
                        color={status.type === "error" ? "error" : "success.main"}
                        align="center"
                    >
                        {status.msg}
                    </Typography>
                )}
                <Button
                    variant="contained"
                    fullWidth
                    disabled={!file}
                    onClick={submit}
                    loading={loading}
                >
                    Обновить аватар
                </Button>
            </Stack>
        </SmallDialog>
    );
}