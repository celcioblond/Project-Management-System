// Request / response contracts shared with the Spring Boot API

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
  type: string;
  status: string;
  startDate: string;
  endDate: string;
  assignedEmployeeNames: string[];
  createdByAdminName: string;
  tasks: TaskResponse[];
  comments: CommentResponse[];
  createdAt: string;
  projectDiagram?: string | null;
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
  type: string;
  status: string;
  startDate: string;     // ISO LocalDateTime string e.g. "2025-01-15T00:00:00"
  endDate: string;
  assignedEmployeeIds: number[];
  tasks?: TaskCreate[];
  comments?: string[];
  projectDiagram?: string | null;
}

export interface ProjectUpdate{
  name?: string;
  description?: string;
  type?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  assignedEmployeeIds?: number[];
  newTasks?: TaskCreate[];
  projectDiagram?: string | null;
}

export interface TaskCreate{
  title: string;
  description: string;
  status: string;
  priority: string;
  dueDate: string;       // ISO LocalDateTime string
  projectId: number;
  assignedEmployeeIds: number[];
}

export interface TaskUpdate{
  title?: string;
  description?: string;
  status?: string;
  priority?: string;
  dueDate?: string;
  assignedEmployeeIds?: number[];
}

export interface ProjectCommentCreate{
  content: string;
  projectId: number;
}

export interface TaskCommentCreate {
  content: string;
  taskId: number;
}

export interface CommentUpdate {
  content: string;
}
