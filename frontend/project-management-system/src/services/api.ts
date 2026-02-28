// src/services/api.ts
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
import axios from 'axios';

//Auth

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

//Responses

export interface CommentResponse {
  id: number;
  content: string;
  authorName: string;
  createdAt: string;
  updatedAt: string;
  projectId?: number;
  taskId?: number;
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
  age: number;
  email: string;
  username: string;
  position: string;
  department: string;
  role: string;
}

//Create and update

export interface UserCreate {
  name: string;
  username: string;
  age: number;
  email: string;
  password: string;
  position: string;
  department: string;
  role: string;
}

export interface UserUpdate {
  name?: string;
  age?: number;
  email?: string;
  position?: string;
  department?: string;
  role?: string;
}

export interface ProjectCreate{
  name: string;
  description: string;
  status: string;
  startDate: string;     // ISO LocalDateTime string e.g. "2025-01-15T00:00:00"
  endDate: string;
  assignedEmployeeIds: number[];
  createdByAdminId: number;
  tasks?: TaskCreate[];
  comments?: string[];
}

export interface ProjectUpdate{
  name?: string;
  description?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  assignedEmployeeIds?: number[];
  updatedByAdminId?: number;
  newTasks?: TaskCreate[];
}

export interface TaskCreate{
  title: string;
  description: string;
  status: string;
  priority: string;
  dueDate: string;       // ISO LocalDateTime string
  projectId: number;
  assignedEmployeeIds: number[];
  assignedByAdminId: number;
}

export interface TaskUpdate{
  title?: string;
  description?: string;
  status?: string;
  priority?: string;
  dueDate?: string;
  updatedByAdminId?: number;
  assignedEmployeeIds?: number[];
}

export interface ProjectCommentCreate{
  content: string;
  projectId: number;
  authorId: number;
}

export interface TaskCommentCreate {
  content: string;
  taskId: number;
  authorId: number;
}

export interface CommentUpdate {
  content: string;
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

  //Employee functions
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

  //PROJECTS CRUD
  async getAllProjects(): Promise<ProjectResponse> {
    try{
      const response = await axios.get<ProjectResponse>(
        `${this.baseUrl}/projects`,
        {headers: this.getAuthHeaders()},
      );
      return response.data
    } catch(error){
      throw new Error("Failed to fetch all projects: " + String(error));
    }
  }

  async addProject(data: ProjectCreate): Promise<ProjectResponse> {
    try{
      const response = await axios.post<ProjectResponse>(
        `${this.baseUrl}/projects`,
        data,
        {headers: this.getAuthHeaders()},
      );
      return response.data;
    } catch(error) {
      throw new Error("Failed to create project: " + String(error));
    }
  }

  async updateProject(id: number, data: ProjectUpdate): Promise<ProjectResponse> {
    try {
      const response = await axios.put<ProjectResponse>(
        `${this.baseUrl}/projects/${id}`,
        data,
        {headers: this.getAuthHeaders()},
      );
      return response.data;
    } catch(error) {
      throw new Error("Failed to update project: " + String(error));
    }
  }

  async deleteProject(id: number): Promise<void>{
    try {
      await axios.delete(`${this.baseUrl}/projects/${id}`, {
        headers: this.getAuthHeaders(),
      });
    } catch(error){
      throw new Error("Failed to delete project: " + String(error));
    }
  }

  //Tasks CRUD

  async getAllTasks(): Promise<TaskResponse> {
    try {
      const response = await axios.get<TaskResponse>(
        `${this.baseUrl}/tasks`,
        {headers: this.getAuthHeaders()},
      );
      return response.data;
    }catch(error){
      throw new Error("Failed to get all tasks: " + String(error));
    }
  }

  async getTaskById(id: number): Promise<TaskResponse> {
    try {
      const response = await axios.get<TaskResponse>(
        `${this.baseUrl}/tasks/${id}`,
        {headers: this.getAuthHeaders()},
      );
      return response.data;
    } catch(error) {
      throw new Error("Failed to get task by id: " + String(error));
    }
  }

  async getTasksByProject(projectId: number): Promise<TaskResponse[]> {
    try {
      const response = await axios.get<TaskResponse[]>(
        `${this.baseUrl}/projects/${projectId}/asks`,
        {headers: this.getAuthHeaders() },
      );
      return response.data;
    } catch(error) {
      throw new Error("Failed to fetch tasks by project: " + String(error));
    }
  }

  async getTasksByEmployee(id: number): Promise<TaskResponse[]>{
    try {
      const response = await axios.get<TaskResponse[]>(
        `${this.baseUrl}/employees/${id}/tasks`,
        {headers: this.getAuthHeaders()},
      );
      return response.data;
    } catch (error) {
      throw new Error("Failed to fetch tasks by employee: " + String(error));
    }
  }

  async createTask(data: TaskCreate, assignedByAdminId: number): Promise<TaskResponse> {
    try {
      const response = await axios.post<TaskResponse>(
        `${this.baseUrl}/tasks?assignedByAdminId=${assignedByAdminId}`,
        data,
        { headers: this.getAuthHeaders() },
      );
      return response.data;
    } catch (error) {
      throw new Error('Failed to create task: ' + String(error));
    }
  }
  
  async updateTask(id: number, data: TaskUpdate): Promise<TaskResponse>{
    try {
      const response = await axios.put<TaskResponse>(
        `${this.baseUrl}/tasks/${id}`,
        data,
        {headers: this.getAuthHeaders()},
      );
      return response.data;
    } catch(error){
      throw new Error("Failed to update task: "+ String(error));
    }
  }

  async deleteTask(id: number): Promise<void> {
    try {
      await axios.delete(`${this.baseUrl}/tasks/${id}`, {
        headers: this.getAuthHeaders(),
      });
    } catch (error) {
      throw new Error('Failed to delete task: ' + String(error));
    }
  }

  //Users CRUD

  async getAllUsers(): Promise<UserResponse>{
    try {
      const response = await axios.get<UserResponse>(
        `${this.baseUrl}/users`,
        {headers: this.getAuthHeaders()},
      );
      return response.data;
    } catch(error) {
      throw new Error("Failed to get all tasks: " + String(error));
    }
  }

  async getUserById(id: number): Promise<UserResponse>{
    try {
      const response = await axios.get<UserResponse>(
        `${this.baseUrl}/users/${id}`,
        {headers: this.getAuthHeaders()}
      );
      return response.data;
    } catch(error) {
      throw new Error("Failed to get user by id: " + String(error));
    }
  }

  async getUserByEmail(email: string): Promise<UserResponse>{
    try {
      const response = await axios.get<UserResponse>(
        `${this.baseUrl}/users/email/${email}`,
        {headers: this.getAuthHeaders()}
      );
      return response.data;
    } catch (error) {
      throw new Error("Failed to get user: " + String(error));
    }
  }

  async createUser(data: UserCreate): Promise<UserResponse> {
    try {
      const response = await axios.post<UserResponse>(
        `${this.baseUrl}/users`,
        data,
        { headers: this.getAuthHeaders() },
      );
      return response.data;
    } catch (error) {
      throw new Error('Failed to create user: ' + String(error));
    }
  }

  async updateUser(id: number, data: UserUpdate): Promise<UserResponse> {
    try{
      const response = await axios.put<UserResponse>(
        `${this.baseUrl}/users/${id}`,
        data,
        {headers: this.getAuthHeaders() },
      );
      return response.data;
    } catch(error) {
      throw new Error("Failed to update user: " + String(error));
    }
  }

  async deleteUser(id: number): Promise<void>{
    try {
      await axios.delete(`${this.baseUrl}/users/${id}`, {
        headers: this.getAuthHeaders(),
      });
    } catch(error) {
      throw new Error("Failed to delete user: " + String(error));
    }
  }

  async updatePassword(id: number, newPassword: string): Promise<void>{
    try {
      await axios.patch(`${this.baseUrl}/users/${id}/password`,
        {newPassword},
        {headers: this.getAuthHeaders()},
      );
    } catch (error) {
      throw new Error("Failed to update password: " + String(error));
    }
  }

  //Project Comments CRUD
  
  async getAllProjectComments(id: number): Promise<CommentResponse[]> {
    try {
      const response = await axios.get<CommentResponse[]>(
        `${this.baseUrl}/projects/${id}/comments`,
        {headers: this.getAuthHeaders()},
      );
      return response.data;
    } catch(error) {
      throw new Error("Failed to get all project comments: " + String(error));
    }
  }

  async createProjectComment(data: ProjectCommentCreate): Promise<CommentResponse> {
    try {
      const response = await axios.post<CommentResponse>(
        `${this.baseUrl}/project-comments`,
        data,
        { headers: this.getAuthHeaders() },
      );
      return response.data;
    } catch (error) {
      throw new Error('Failed to create project comment: ' + String(error));
    }
  }

    async updateProjectComment(id: number, data: CommentUpdate): Promise<CommentResponse> {
    try {
      // Backend uses PUT /api/project-comments (create) — update goes through the same route
      // but we target by ID; check your ProjectCommentController — it has no updateComment endpoint.
      // Adding it here for completeness; wire up when backend supports it.
      const response = await axios.put<CommentResponse>(
        `${this.baseUrl}/project-comments/${id}`,
        data,
        { headers: this.getAuthHeaders() },
      );
      return response.data;
    } catch (error) {
      throw new Error('Failed to update project comment: ' + String(error));
    }
  }

  async deleteProjectComment(id: number): Promise<void>{
    try{
      await axios.delete(`${this.baseUrl}/project-comments/${id}`, {
        headers: this.getAuthHeaders(),
      });
    } catch(error){
      throw new Error("Failed to delete project comment: " + String(error));
    }
  }

  //Task Comments
  
  async getAllTaskComments(id: number): Promise<CommentResponse[]>{
    try{
      const response = await axios.get<CommentResponse[]>(
        `${this.baseUrl}/tasks/${id}/comments`, {
          headers: this.getAuthHeaders(),
        });
        return response.data;
    } catch (error) {
      throw new Error("Failed to get all tasks: " + String(error));
    }
  }

  async createTaskComment(data: TaskCommentCreate): Promise<CommentResponse> {
    try {
      const response = await axios.post<CommentResponse>(
        `${this.baseUrl}/task-comments`,
        data,
        { headers: this.getAuthHeaders() },
      );
      return response.data;
    } catch (error) {
      throw new Error('Failed to create task comment: ' + String(error));
    }
  }

  async updateTaskComment(id: number, data: CommentUpdate): Promise<CommentResponse> {
    try {
      const response = await axios.put<CommentResponse>(
        `${this.baseUrl}/task-comments/${id}`,
        data,
        {headers: this.getAuthHeaders()},
      );
      return response.data;
    } catch(error) {
      throw new Error("Failed to update task comment: " + String(error));
    }
  }

  async deleteTaskComment(id: number): Promise<void>{
    try {
      await axios.delete(`${this.baseUrl}/task-comments/${id}`, {
        headers: this.getAuthHeaders(),
      });
    } catch(error){
      throw new Error("Failed to delete task comment: " + String(error));
    }
  }
}

export const apiService = new ApiService(API_BASE_URL);