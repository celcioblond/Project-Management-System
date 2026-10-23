// src/context/auth-context.ts
import { createContext } from 'react';
import type { AuthResponse } from '../types/api';

export interface AuthContextType {
  user: AuthResponse | null;
  login: (authData: AuthResponse) => void;
  logout: () => void;
  isAuthenticated: boolean;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);
