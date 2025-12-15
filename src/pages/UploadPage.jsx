import React, { useMemo, useRef, useState } from 'react';
import { apiBase } from '../api/api';
import { useAuth } from '../context/AuthContext';
import Status from '../components/Status';

export default function UploadPage({ active, onRequireAuth, onJobTracked }) {
    const { token } = useAuth();
    const fileRef = useRef(null);

    const [title, setTitle] = useState('');
    const [progress, setProgress] = useState({ show: false, value: 0 });
    const [status, setStatus] = useState({ msg: '', type: '' });

    function setStatusUpload(msg = '', type = '') {
        setStatus({ msg, type });
    }

    const canUpload = !!token;

    const hint = useMemo(() => (
        <div className="hint">
            Видео будет автоматически перекодировано в MP4 (H.264), чтобы браузер его точно открыл.
        </div>
    ), []);

    function startUpload() {
        if (!token) {
            setStatusUpload('Войдите, чтобы загружать видео', 'error');
            onRequireAuth?.();
            return;
        }

        const file = fileRef.current?.files?.[0];
        const videoName = title.trim();

        if (!file) {
            setStatusUpload('Выберите файл видео', 'error');
            return;
        }

        const formData = new FormData();
        formData.append('video', file);
        formData.append('videoName', videoName || file.name);

        setProgress({ show: true, value: 0 });
        setStatusUpload('Начинаю загрузку…', 'ok');

        const xhr = new XMLHttpRequest();
        xhr.open('POST', apiBase + '/streaming/video/upload', true);
        xhr.setRequestHeader('Authorization', 'Bearer ' + token);

        xhr.upload.onprogress = (e) => {
            if (e.lengthComputable) {
                const percent = Math.round((e.loaded / e.total) * 100);
                setProgress({ show: true, value: percent });
                setStatusUpload(`Загружаю файл… ${percent}%`, 'ok');
            }
        };

        xhr.upload.onload = () => {
            setStatusUpload('Файл загружен, идёт обработка…', 'ok');
            setProgress({ show: true, value: 100 });
        };

        xhr.onreadystatechange = () => {
            if (xhr.readyState !== XMLHttpRequest.DONE) return;

            if (xhr.status >= 200 && xhr.status < 300) {
                let resp = null;
                try { resp = JSON.parse(xhr.responseText); } catch {}
                if (!resp || !resp.jobId) {
                    setStatusUpload('Сервер не вернул jobId', 'error');
                    return;
                }

                const jobId = resp.jobId;
                const jobLabel = videoName || file.name;

                setStatusUpload('Идёт обработка видео…', 'ok');
                onJobTracked?.(jobId, jobLabel, null);
            } else if (xhr.status === 401 || xhr.status === 403) {
                setStatusUpload('Сессия истекла, войдите снова', 'error');
                onRequireAuth?.();
            } else {
                setStatusUpload(`Ошибка загрузки: HTTP ${xhr.status}`, 'error');
            }
        };

        xhr.onerror = () => setStatusUpload('Сетевая ошибка при загрузке', 'error');

        xhr.send(formData);
    }

    if (!active) return null;

    return (
        <div className={'card page ' + (active ? 'active' : '')} id="pageUpload">
            <div className="header">
                <div className="title">Загрузка нового видео</div>
            </div>

            <div className="form-row">
                <label htmlFor="uploadTitle">Название видео</label>
                <input
                    id="uploadTitle"
                    type="text"
                    placeholder="Как назвать ролик?"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    disabled={!canUpload}
                />
            </div>

            <div className="form-row">
                <label htmlFor="uploadFile">Файл видео</label>
                <input id="uploadFile" ref={fileRef} type="file" accept="video/*" disabled={!canUpload} />
                {hint}
            </div>

            <button className="full" id="btnUpload" onClick={startUpload} disabled={!canUpload}>
                ⬆ Загрузить
            </button>

            <progress
                id="uploadProgress"
                value={progress.value}
                max="100"
                style={{ width: '100%', marginTop: 6, display: progress.show ? 'block' : 'none' }}
            />

            <Status id="statusUpload" message={status.msg} type={status.type} />
        </div>
    );
}
