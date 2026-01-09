import React from 'react';

export default function ThumbGrid({ videos, renderCard }) {
    return <div className="thumb-grid">{videos?.map((v) => renderCard(v))}</div>;
}
