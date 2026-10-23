// src/services/http.ts
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const http = axios.create({
  baseURL: API_BASE_URL,
});

const isAuthEndpoint = (url?: string) => url?.startsWith('/auth/') ?? false;

// Attach the JWT to every request except the public auth endpoints
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token && !isAuthEndpoint(config.url)) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// An expired or revoked token gets a 401: drop the session and send the user to login
http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401 && !isAuthEndpoint(error.config?.url)) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login') {
        window.location.assign('/login');
      }
    }
    return Promise.reject(error);
  },
);

/** Human-readable message from a failed request, preferring the API's problem `detail`. */
export function errorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const detail = (error.response?.data as { detail?: unknown } | undefined)?.detail;
    if (typeof detail === 'string') return detail;
  }
  return error instanceof Error ? error.message : String(error);
}
