import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { apiBase, authHeaders } from '../api/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [token, setToken] = useState(null);
    const [user, setUser] = useState(null); // { userId, username, displayName, roles, avatarUrl }
    const [authReady, setAuthReady] = useState(false);

    const isModerator = useMemo(() => {
        return !!user?.roles?.includes('MODERATOR') || !!user?.roles?.includes('ADMIN');
    }, [user?.roles]);

    const isAdmin = useMemo(() => {
        return !!user?.roles?.includes('ADMIN');
    }, [user?.roles]);

    const setAuth = useCallback((newToken, newUser) => {
        if (newToken && newUser?.userId != null) {
            setToken(newToken);
            setUser(newUser);
            localStorage.setItem('authToken', newToken);
            localStorage.setItem('authUser', JSON.stringify(newUser));
        } else {
            setToken(null);
            setUser(null);
            localStorage.removeItem('authToken');
            localStorage.removeItem('authUser');
        }
    }, []);

    const initAuth = useCallback(async () => {
        const savedToken = localStorage.getItem('authToken');
        const savedUserJson = localStorage.getItem('authUser');

        if (!savedToken) {
            setAuth(null, null);
            setAuthReady(true);
            return;
        }

        setToken(savedToken);
        if (savedUserJson) {
            try {
                setUser(JSON.parse(savedUserJson));
            } catch {
            }
        }

        try {
            const res = await fetch(apiBase + '/api/auth/me', { headers: authHeaders(savedToken) });
            if (!res.ok) {
                if (res.status === 401 || res.status === 403) {
                    setAuth(null, null);
                }
                setAuthReady(true);
                return;
            }

            const data = await res.json();
            setAuth(savedToken, {
                userId: data.userId,
                username: data.username,
                displayName: data.displayName,
                roles: data.roles ?? [],
                avatarUrl: data.avatarUrl,
            });
        } finally {
            setAuthReady(true);
        }
    }, [setAuth]);

    useEffect(() => {
        initAuth();
    }, [initAuth]);

    const value = useMemo(() => ({
        token,
        user,
        authReady,
        isModerator,
        isAdmin,
        setAuth,
        initAuth,
    }), [token, user, isModerator, isAdmin, setAuth, initAuth, authReady]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
    return ctx;
}
