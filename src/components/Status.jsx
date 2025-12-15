import React from 'react';

export default function Status({ id, message, type = '' }) {
    return (
        <div id={id} className={'status' + (type ? ' ' + type : '')}>
            {message || ''}
        </div>
    );
}
