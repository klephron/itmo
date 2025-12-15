import React from 'react';

export default function Tabs({ activeTab, onTab, showModeration }) {
    return (
        <div className="tabs">
            <button className={'tab-btn ' + (activeTab === 'feed' ? 'active' : '')} onClick={() => onTab('feed')}>
                Лента
            </button>

            <button className={'tab-btn ' + (activeTab === 'upload' ? 'active' : '')} onClick={() => onTab('upload')}>
                Загрузка
            </button>

            <button
                className={'tab-btn ' + (activeTab === 'moderation' ? 'active' : '')}
                onClick={() => onTab('moderation')}
                style={{ display: showModeration ? '' : 'none' }}
            >
                Модерация
            </button>

            <button className={'tab-btn ' + (activeTab === 'auth' ? 'active' : '')} onClick={() => onTab('auth')}>
                Аккаунт
            </button>
        </div>
    );
}
