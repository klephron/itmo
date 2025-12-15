import React from 'react';

export default function Avatar({ url, className = 'avatar-mini', onClick }) {
    return (
        <div className={className} onClick={onClick} style={onClick ? { cursor: 'pointer' } : undefined}>
            {url ? <img src={url} alt="" /> : null}
        </div>
    );
}
