import React from 'react';
import { formatDate } from '../utils/format';

export function ThumbCard({ video, showDelete, onDelete, onOpen }) {
    const likesVal = typeof video?.likesCount === 'number'
        ? video.likesCount
        : (typeof video?.likes === 'number' ? video.likes : 0);

    const viewsVal = typeof video?.viewsCount === 'number'
        ? video.viewsCount
        : (typeof video?.views === 'number' ? video.views : 0);

    return (
        <div className="thumb-card" onClick={onOpen}>
            <img src={video.thumbnailUrl} alt={video.name || ''} />
            <div className="thumb-card-overlay" title={formatDate(video.uploadedAt)}>
                ♥ {likesVal} · 👁 {viewsVal}
            </div>
            {showDelete ? (
                <div
                    className="thumb-card-delete"
                    onClick={(e) => { e.stopPropagation(); onDelete?.(); }}
                    title="Удалить"
                >
                    🗑
                </div>
            ) : null}
        </div>
    );
}

export default function ThumbGrid({ videos, renderCard }) {
    return <div className="thumb-grid">{videos?.map((v) => renderCard(v))}</div>;
}
