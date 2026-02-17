// src/components/PublicRoute.tsx
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

interface PublicRouteProps {
  children: React.ReactNode;
}

const PublicRoute = ({ children }: PublicRouteProps) => {
  const { isAuthenticated, user } = useAuth();

  // If already authenticated, redirect to appropriate dashboard
  if (isAuthenticated && user) {
    if (user.role === 'ADMIN') {
      return <Navigate to="/admin/dashboard" replace />;
    }
    return <Navigate to="/employee/dashboard" replace />;
  }

  // Not authenticated, show the public page
  return <>{children}</>;
};

export default PublicRoute;
