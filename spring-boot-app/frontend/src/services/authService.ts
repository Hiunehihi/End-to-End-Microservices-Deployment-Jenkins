import api from './api';
import { AuthTokenResponse } from '../types';

export interface RegisterPayload {
    username: string;
    email: string;
    password: string;
    firstName?: string;
    lastName?: string;
}

export const authService = {
    login: (username: string, password: string) =>
        api.post<AuthTokenResponse>('/api/auth/login', { username, password }),
    register: (payload: RegisterPayload) =>
        api.post<AuthTokenResponse>('/api/auth/register', payload),
    refresh: (refreshToken: string) =>
        api.post<AuthTokenResponse>('/api/auth/refresh', { refreshToken })
};
