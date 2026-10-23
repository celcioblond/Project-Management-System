// src/services/api.ts
import type { AxiosResponse } from 'axios';
import { http } from './http';
import type {
  AuthResponse,
  CommentResponse,
  CommentUpdate,
  LoginRequest,
  ProjectCommentCreate,
  ProjectCreate,
  ProjectResponse,
  ProjectUpdate,
  RegisterRequest,
  TaskCommentCreate,
  TaskCreate,
  TaskResponse,
  TaskUpdate,
  UserCreate,
  UserResponse,
  UserUpdate,
} from '../types/api';

export type * from '../types/api';

// Unwraps the response body and turns any failure into an Error with a readable prefix
async function call<T>(request: Promise<AxiosResponse<T>>, errorMessage: string): Promise<T> {
  try {
    const response = await request;
    return response.data;
  } catch (error) {
    throw new Error(`${errorMessage}: ${String(error)}`);
  }
}

export const apiService = {
  // Auth
  login: (credentials: LoginRequest) =>
    call(http.post<AuthResponse>('/auth/login', credentials), 'Login failed'),

  register: (credentials: RegisterRequest) =>
    call(http.post<AuthResponse>('/auth/register', credentials), 'Registration failed'),

  // Employee
  getMyProjects: () =>
    call(http.get<ProjectResponse[]>('/projects/my-projects'), 'Failed to fetch projects'),

  getMyColleagues: (projectId: number) =>
    call(http.get<UserResponse[]>(`/projects/${projectId}/colleagues`), 'Failed to fetch colleagues'),

  getMyTasks: () =>
    call(http.get<TaskResponse[]>('/tasks/my-tasks'), 'Failed to fetch tasks'),

  getMyProjectComments: () =>
    call(http.get<CommentResponse[]>('/project-comments/my-comments'), 'Failed to fetch project comments'),

  getMyTaskComments: () =>
    call(http.get<CommentResponse[]>('/tasks-comments/my-comments'), 'Failed to fetch task comments'),

  // Projects
  getAllProjects: () =>
    call(http.get<ProjectResponse[]>('/projects'), 'Failed to fetch all projects'),

  getProjectById: (id: number) =>
    call(http.get<ProjectResponse>(`/projects/${id}`), 'Failed to fetch project by id'),

  addProject: (data: ProjectCreate) =>
    call(http.post<ProjectResponse>('/projects', data), 'Failed to create project'),

  updateProject: (id: number, data: ProjectUpdate) =>
    call(http.put<ProjectResponse>(`/projects/${id}`, data), 'Failed to update project'),

  deleteProject: (id: number) =>
    call(http.delete<void>(`/projects/${id}`), 'Failed to delete project'),

  // Tasks
  getAllTasks: () =>
    call(http.get<TaskResponse[]>('/tasks'), 'Failed to get all tasks'),

  getTaskById: (id: number) =>
    call(http.get<TaskResponse>(`/tasks/${id}`), 'Failed to get task by id'),

  getTasksByProject: (projectId: number) =>
    call(http.get<TaskResponse[]>(`/projects/${projectId}/tasks`), 'Failed to fetch tasks by project'),

  getTasksByEmployee: (id: number) =>
    call(http.get<TaskResponse[]>(`/employees/${id}/tasks`), 'Failed to fetch tasks by employee'),

  createTask: (data: TaskCreate, assignedByAdminId: number) =>
    call(http.post<TaskResponse>('/tasks', data, { params: { assignedByAdminId } }), 'Failed to create task'),

  updateTask: (id: number, data: TaskUpdate) =>
    call(http.put<TaskResponse>(`/tasks/${id}`, data), 'Failed to update task'),

  deleteTask: (id: number) =>
    call(http.delete<void>(`/tasks/${id}`), 'Failed to delete task'),

  // Users
  getAllUsers: () =>
    call(http.get<UserResponse[]>('/users'), 'Failed to get all users'),

  getUserById: (id: number) =>
    call(http.get<UserResponse>(`/users/${id}`), 'Failed to get user by id'),

  getUserByEmail: (email: string) =>
    call(http.get<UserResponse>(`/users/email/${encodeURIComponent(email)}`), 'Failed to get user'),

  createUser: (data: UserCreate) =>
    call(http.post<UserResponse>('/users', data), 'Failed to create user'),

  updateUser: (id: number, data: UserUpdate) =>
    call(http.put<UserResponse>(`/users/${id}`, data), 'Failed to update user'),

  deleteUser: (id: number) =>
    call(http.delete<void>(`/users/${id}`), 'Failed to delete user'),

  updatePassword: (id: number, newPassword: string) =>
    call(http.patch<void>(`/users/${id}/password`, { newPassword }), 'Failed to update password'),

  // Project comments
  getAllProjectComments: (projectId: number) =>
    call(http.get<CommentResponse[]>(`/projects/${projectId}/comments`), 'Failed to get all project comments'),

  createProjectComment: (data: ProjectCommentCreate) =>
    call(http.post<CommentResponse>('/project-comments', data), 'Failed to create project comment'),

  updateProjectComment: (id: number, data: CommentUpdate) =>
    call(http.put<CommentResponse>(`/project-comments/${id}`, data), 'Failed to update project comment'),

  deleteProjectComment: (id: number) =>
    call(http.delete<void>(`/project-comments/${id}`), 'Failed to delete project comment'),

  // Task comments
  getAllTaskComments: (taskId: number) =>
    call(http.get<CommentResponse[]>(`/tasks/${taskId}/comments`), 'Failed to get all task comments'),

  createTaskComment: (data: TaskCommentCreate) =>
    call(http.post<CommentResponse>('/task-comments', data), 'Failed to create task comment'),

  updateTaskComment: (id: number, data: CommentUpdate) =>
    call(http.put<CommentResponse>(`/task-comments/${id}`, data), 'Failed to update task comment'),

  deleteTaskComment: (id: number) =>
    call(http.delete<void>(`/task-comments/${id}`), 'Failed to delete task comment'),

  // AI: description, diagram and chatbot
  generateProject: (name: string, type: string) =>
    call(http.post<string>('/project/generate-project', { name, type }), 'Failed to generate description'),

  generateDiagram: async (name: string, type: string, description: string): Promise<Blob> => {
    const data = await call(
      http.post<ArrayBuffer>('/project/generate-diagram', { title: name, type, description }, { responseType: 'arraybuffer' }),
      'Failed to generate diagram',
    );
    return new Blob([data], { type: 'image/jpeg' });
  },

  askBot: (userQuery: string, username: string) =>
    call(http.get<string>('/chat/ask', { params: { userQuery, username } }), 'Failed to chat with bot'),
};
