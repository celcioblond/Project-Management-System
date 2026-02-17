const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
import axios from 'axios';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  username: string;
  role: string;
}

class ApiService {
  private baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  async login(credentials: LoginRequest): Promise<AuthResponse> {
    try {
      const response = await axios.post<AuthResponse>(`${this.baseUrl}/auth/login`, credentials);
      return response.data;
    } catch (error) {
      throw new Error('Login failed: ' + String(error));
    }
  }

  async register(credentials: RegisterRequest): Promise<AuthResponse> {
    try {
      const response = await axios.post<AuthResponse>(`${this.baseUrl}/auth/register`, credentials);
      return response.data;
    } catch (error) {
      throw new Error('Registration failed: ' + String(error));
    }
  }

}

export const apiService = new ApiService(API_BASE_URL);
