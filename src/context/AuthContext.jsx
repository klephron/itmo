import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { apiBase, authHeaders } from '../api/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [token, setToken] = useState(null);
    const [user, setUser] = useState(null); // { userId, username, displayName, roles, avatarUrl }

    const isModerator = useMemo(() => {
        return !!user?.roles?.includes('MODERATOR') || !!user?.roles?.includes('ADMIN');
    }, [user]);

    const isAdmin = useMemo(() => {
        return !!user?.roles?.includes('ADMIN');
    }, [user]);

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
            return;
        }

        setToken(savedToken);
        if (savedUserJson) {
            try { setUser(JSON.parse(savedUserJson)); } catch { setUser(null); }
        }

        try {
            const res = await fetch(apiBase + '/api/auth/me', { headers: authHeaders(savedToken) });
            if (!res.ok) {
                if (res.status === 401 || res.status === 403) setAuth(null, null);
                return;
            }
            const data = await res.json();
            const nextUser = {
                userId: data.userId,
                username: data.username,
                displayName: data.displayName,
                roles: Array.isArray(data.roles) ? data.roles : [],
                avatarUrl: data.avatarUrl ?? null,
            };
            setAuth(savedToken, nextUser);
        } catch {
            // network errors: keep cached state
        }
    }, [setAuth]);

    useEffect(() => { initAuth(); }, [initAuth]);

    const value = useMemo(() => ({
        token,
        user,
        isModerator,
        isAdmin,
        setAuth,
        initAuth,
    }), [token, user, isModerator, isAdmin, setAuth, initAuth]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
    return ctx;
}
