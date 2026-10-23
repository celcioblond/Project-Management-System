// src/services/http.ts
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const http = axios.create({
  baseURL: API_BASE_URL,
});

// Attach the JWT to every request except the public auth endpoints
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token && !config.url?.startsWith('/auth/')) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
