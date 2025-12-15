import React from 'react';

export default function Tabs({ activeTab, onTab, showModeration, isAuthenticated }) {
    return (
        <div className="tabs">
            <button
                className={'tab-btn ' + (activeTab === 'feed' ? 'active' : '')}
                onClick={() => onTab('feed')}
            >
                Лента
            </button>

            <button
                className={'tab-btn ' + (activeTab === 'upload' ? 'active' : '')}
                onClick={() => onTab('upload')}
            >
                Загрузка
            </button>

            {showModeration && (
                <button
                    className={'tab-btn ' + (activeTab === 'moderation' ? 'active' : '')}
                    onClick={() => onTab('moderation')}
                >
                    Модерация
                </button>
            )}

            {isAuthenticated ? (
                <button
                    className={'tab-btn ' + (activeTab === 'account' ? 'active' : '')}
                    onClick={() => onTab('account')}
                >
                    Аккаунт
                </button>
            ) : (
                <button
                    className={'tab-btn ' + (activeTab === 'auth' ? 'active' : '')}
                    onClick={() => onTab('auth')}
                >
                    Вход
                </button>
            )}
        </div>
    );
}