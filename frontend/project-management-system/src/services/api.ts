// src/services/api.ts
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

export interface CommentResponse {
  id: number;
  content: string;
  authorName: string;
  createdAt: string;
  updatedAt: string;
}

export interface TaskResponse {
  id: number;
  title: string;
  description: string;
  priority: string;
  status: string;
  dueDate: string;
  projectName: string;
  assignedEmployeeNames: string[];
  assignedByAdminName: string;
  comments: CommentResponse[];
  createdAt: string;
}

export interface ProjectResponse {
  id: number;
  title: string;
  description: string;
  status: string;
  startDate: string;
  endDate: string;
  assignedEmployeeNames: string[];
  createdByAdminName: string;
  tasks: TaskResponse[];
  comments: CommentResponse[];
  createdAt: string;
}

export interface UserResponse {
  id: number;
  name: string;
  email: string;
  username: string;
  position: string;
  department: string;
  role: string;
}

class ApiService {
  private baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  // Helper method to get auth headers
  private getAuthHeaders() {
    const token = localStorage.getItem('token');
    return {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : '',
    };
  }

  async login(credentials: LoginRequest): Promise<AuthResponse> {
    try {
      const response = await axios.post<AuthResponse>(
        `${this.baseUrl}/auth/login`,
        credentials
      );
      return response.data;
    } catch (error) {
      throw new Error('Login failed: ' + String(error));
    }
  }

  async register(credentials: RegisterRequest): Promise<AuthResponse> {
    try {
      const response = await axios.post<AuthResponse>(
        `${this.baseUrl}/auth/register`,
        credentials
      );
      return response.data;
    } catch (error) {
      throw new Error('Registration failed: ' + String(error));
    }
  }

  async getMyProjects(): Promise<ProjectResponse[]> {
    try {
      const response = await axios.get<ProjectResponse[]>(
        `${this.baseUrl}/projects/my-projects`,
        { headers: this.getAuthHeaders() }
      );
      return response.data;
    } catch (error) {
      throw new Error('Failed to fetch projects: ' + String(error));
    }
  }

  async getMyColleagues(projectId: number): Promise<UserResponse[]> {
    try {
      const response = await axios.get<UserResponse[]>(
        `${this.baseUrl}/projects/${projectId}/colleagues`,
        { headers: this.getAuthHeaders() }
      );
      return response.data;
    } catch (error) {
      throw new Error("Failed to fetch colleagues : " + String(error));
    }
  }

  async getMyTasks(): Promise<TaskResponse[]> {
    try {
      const response = await axios.get<TaskResponse[]>(
        `${this.baseUrl}/tasks/my-tasks`,
        { headers: this.getAuthHeaders() }
      );
      return response.data;
    } catch (error) {
      throw new Error("Failed to fetch tasks : " + String(error));
    }
  }

  async getMyProjectComments(): Promise<CommentResponse[]> {
    try {
      const response = await axios.get<CommentResponse[]>(
        `${this.baseUrl}/project-comments/my-comments`,
        { headers: this.getAuthHeaders() }
      );
      return response.data;
    } catch (error) {
      throw new Error("Failed to fetch project comments: " + String(error));
    }
  }

  async getMyTaskComments(): Promise<CommentResponse[]> {
    try {
      const response = await axios.get<CommentResponse[]>(
        `${this.baseUrl}/tasks-comments/my-comments`,
        { headers: this.getAuthHeaders() }
      );
      return response.data;
    } catch (error) {
      throw new Error("Failed to fetch task comments: " + String(error));
    }
  }

  async getProjectById(id: number): Promise<ProjectResponse> {
    try {
      const response = await axios.get<ProjectResponse>(
        `${this.baseUrl}/projects/${id}`,
        { headers: this.getAuthHeaders()}
      );
      return response.data;
    } catch (error) {
      throw new Error("Failed to fetch project by id " + String(error));
    }
  }
}

export const apiService = new ApiService(API_BASE_URL);