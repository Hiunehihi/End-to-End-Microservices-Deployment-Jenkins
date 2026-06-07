import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { jwtDecode } from 'jwt-decode';
import { authService, RegisterPayload } from '../services/authService';
import { AuthTokenResponse } from '../types';

interface DecodedToken {
    sub: string;
    preferred_username?: string;
    email?: string;
    given_name?: string;
    family_name?: string;
    realm_access?: { roles?: string[] };
    exp: number;
}

interface AuthUser {
    id: string;
    username: string;
    email?: string;
    firstName?: string;
    lastName?: string;
    roles: string[];
}

interface AuthContextType {
    user: AuthUser | null;
    token: string | null;
    initializing: boolean;
    isAuthenticated: boolean;
    isAdmin: boolean;
    login: (username: string, password: string) => Promise<AuthUser>;
    register: (payload: RegisterPayload) => Promise<AuthUser>;
    logout: () => void;
    hasRole: (role: string) => boolean;
}

const TOKEN_KEY = 'token';
const REFRESH_KEY = 'refreshToken';

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const decodeUser = (token: string): AuthUser | null => {
    try {
        const decoded = jwtDecode<DecodedToken>(token);
        if (!decoded?.sub) return null;
        if (decoded.exp && decoded.exp * 1000 < Date.now()) return null;
        return {
            id: decoded.sub,
            username: decoded.preferred_username ?? decoded.sub,
            email: decoded.email,
            firstName: decoded.given_name,
            lastName: decoded.family_name,
            roles: decoded.realm_access?.roles ?? []
        };
    } catch {
        return null;
    }
};

const persistTokens = (data: AuthTokenResponse) => {
    localStorage.setItem(TOKEN_KEY, data.access_token);
    if (data.refresh_token) {
        localStorage.setItem(REFRESH_KEY, data.refresh_token);
    }
};

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY));
    const [user, setUser] = useState<AuthUser | null>(() => {
        const stored = localStorage.getItem(TOKEN_KEY);
        return stored ? decodeUser(stored) : null;
    });
    const [initializing, setInitializing] = useState(false);

    useEffect(() => {
        const handler = () => {
            setToken(null);
            setUser(null);
        };
        window.addEventListener('auth:logout', handler);
        return () => window.removeEventListener('auth:logout', handler);
    }, []);

    const apply = useCallback((data: AuthTokenResponse): AuthUser => {
        persistTokens(data);
        const decoded = decodeUser(data.access_token);
        setToken(data.access_token);
        setUser(decoded);
        if (!decoded) {
            throw new Error('Token không hợp lệ');
        }
        return decoded;
    }, []);

    const login = useCallback(async (username: string, password: string) => {
        setInitializing(true);
        try {
            const { data } = await authService.login(username, password);
            return apply(data);
        } finally {
            setInitializing(false);
        }
    }, [apply]);

    const register = useCallback(async (payload: RegisterPayload) => {
        setInitializing(true);
        try {
            const { data } = await authService.register(payload);
            return apply(data);
        } finally {
            setInitializing(false);
        }
    }, [apply]);

    const logout = useCallback(() => {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(REFRESH_KEY);
        setToken(null);
        setUser(null);
    }, []);

    const value = useMemo<AuthContextType>(() => ({
        user,
        token,
        initializing,
        isAuthenticated: !!user,
        isAdmin: !!user?.roles.includes('ADMIN'),
        login,
        register,
        logout,
        hasRole: (role: string) => !!user?.roles.includes(role)
    }), [user, token, initializing, login, register, logout]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error('useAuth must be used within AuthProvider');
    return ctx;
};
