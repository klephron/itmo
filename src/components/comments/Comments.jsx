import React, { useEffect, useMemo, useState } from 'react';
import { apiBase, authHeaders, isUnauthorized } from '../../api/api';
import { formatDate } from '../../utils/format';
import { useAuth } from '../../context/AuthContext';
import Status from '../Status';

export default function Comments({ videoUuid, onOpenProfile, onRequireAuth }) {
    const { token, user } = useAuth();
    const [cache] = useState(() => new Map());
    const [list, setList] = useState([]);
    const [status, setStatus] = useState({ msg: '', type: '' });
    const [text, setText] = useState('');

    const cached = useMemo(() => (videoUuid ? cache.get(videoUuid) : null), [cache, videoUuid]);

    useEffect(() => {
        if (!videoUuid) return;
        if (cached) setList(cached);

        let aborted = false;

        async function load() {
            setStatus({ msg: 'Загрузка комментариев…', type: '' });
            try {
                const res = await fetch(apiBase + '/api/videos/' + videoUuid + '/comments', {
                    headers: authHeaders(token),
                });
                if (!res.ok) throw new Error('HTTP ' + res.status);
                const data = await res.json();
                if (aborted) return;
                cache.set(videoUuid, data);
                setList(data);
                setStatus({ msg: '', type: '' });
            } catch (e) {
                if (aborted) return;
                setStatus({ msg: 'Ошибка загрузки комментариев: ' + e.message, type: 'error' });
            }
        }

        load();
        return () => { aborted = true; };
    }, [videoUuid, token, cache, cached]);

    async function send() {
        if (!token) {
            setStatus({ msg: 'Войдите, чтобы комментировать', type: 'error' });
            onRequireAuth?.();
            return;
        }
        const trimmed = text.trim();
        if (!trimmed) {
            setStatus({ msg: 'Введите текст комментария', type: 'error' });
            return;
        }

        try {
            const res = await fetch(apiBase + '/api/videos/' + videoUuid + '/comments', {
                method: 'POST',
                headers: { ...authHeaders(token), 'Content-Type': 'application/json' },
                body: JSON.stringify({ text: trimmed }),
            });

            if (isUnauthorized(res)) {
                setStatus({ msg: 'Сессия истекла, войдите снова', type: 'error' });
                onRequireAuth?.();
                return;
            }

            if (!res.ok) throw new Error('HTTP ' + res.status);

            const newComment = await res.json();
            const next = [...(cache.get(videoUuid) || []), newComment];
            cache.set(videoUuid, next);
            setList(next);
            setText('');
            setStatus({ msg: 'Комментарий отправлен', type: 'ok' });
        } catch (e) {
            setStatus({ msg: 'Ошибка отправки: ' + e.message, type: 'error' });
        }
    }

    async function deleteComment(commentId) {
        if (!token) {
            setStatus({ msg: 'Войдите, чтобы удалять комментарии', type: 'error' });
            onRequireAuth?.();
            return;
        }
        if (!confirm('Удалить комментарий?')) return;

        try {
            const res = await fetch(apiBase + `/api/videos/${videoUuid}/comments/${commentId}`, {
                method: 'DELETE',
                headers: authHeaders(token),
            });

            if (isUnauthorized(res)) {
                setStatus({ msg: 'Сессия истекла, войдите снова', type: 'error' });
                onRequireAuth?.();
                return;
            }

            if (!res.ok && res.status !== 204) throw new Error('HTTP ' + res.status);

            const next = (cache.get(videoUuid) || []).filter((c) => c.id !== commentId);
            cache.set(videoUuid, next);
            setList(next);
            setStatus({ msg: 'Комментарий удалён', type: 'ok' });
        } catch (e) {
            setStatus({ msg: 'Ошибка удаления: ' + e.message, type: 'error' });
        }
    }

    return (
        <div className="comments">
            <div className="studio-section-title" style={{ marginTop: 0 }}>Комментарии</div>
            <div className="comments-list">
                {!list || list.length === 0 ? (
                    <div className="studio-row-meta">Комментариев пока нет</div>
                ) : (
                    list.map((c) => {
                        const canDelete = !!c?.mine || (user?.roles || []).some((r) => r === 'ADMIN' || r === 'MODERATOR');
                        return (
                            <div key={c.id} className="comment-row">
                                <div
                                    className="comment-avatar"
                                    onClick={() => c.authorId && onOpenProfile?.(c.authorId)}
                                    title={c.authorName || 'Автор'}
                                >
                                    {c.authorAvatarUrl ? <img src={c.authorAvatarUrl} alt="" /> : null}
                                </div>

                                <div className="comment-body">
                                    <div>
                    <span className="comment-author" onClick={() => c.authorId && onOpenProfile?.(c.authorId)}>
                      {c.authorName || 'Аноним'}
                    </span>
                                        <span className="comment-meta">{formatDate(c.createdAt)}</span>

                                        {canDelete ? (
                                            <span className="comment-delete" onClick={() => deleteComment(c.id)}>
                        удалить
                      </span>
                                        ) : null}
                                    </div>

                                    <div className="comment-text">{c.text}</div>
                                </div>
                            </div>
                        );
                    })
                )}
            </div>

            {user ? (
                <div className="form-row">
                    <input
                        type="text"
                        placeholder="Напишите комментарий…"
                        value={text}
                        onChange={(e) => setText(e.target.value)}
                        onKeyDown={(e) => { if (e.key === 'Enter') send(); }}
                    />
                    <button className="small full" onClick={send}>Отправить</button>
                </div>
            ) : (
                <div className="hint">Войдите в аккаунт, чтобы оставлять комментарии</div>
            )}

            <Status id="commentsStatus" message={status.msg} type={status.type} />
        </div>
    );
}
