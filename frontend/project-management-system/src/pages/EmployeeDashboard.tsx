// src/pages/EmployeeDashboard.tsx
import { useEffect, useState } from 'react';
import CardNote from '../components/CardNote';
import { useAuth } from '../context/AuthContext';
import { apiService } from '../services/api';
import type {ProjectResponse} from '../services/api';

const EmployeeDashboard = () => {
  const { user } = useAuth();

  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchProjects = async () => {
      try {
        setLoading(true); // Changed: Should be true while fetching
        const data = await apiService.getMyProjects();
        setProjects(data);
        console.log('Fetched projects:', data);
      } catch (err) {
        console.error('Error fetching projects:', err);
        setError(
          err instanceof Error ? err.message : 'Failed to load projects',
        );
      } finally {
        setLoading(false);
      }
    };

    fetchProjects();
  }, []);

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <span className="loading loading-spinner loading-lg"></span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="text">
        <h1>Error displaying: {error}</h1>
      </div>
    );
  }

  return (
    <div className="mt-8">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold">Employee Dashboard</h1>
          <p className="text-gray-600 mt-2">Welcome back, {user?.username}!</p>
        </div>
      </div>

      {projects.length === 0 ? (
        <div className="alert alert-info">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            className="stroke-current shrink-0 w-6 h-6"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="2"
              d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
            ></path>
          </svg>
          <span>No projects assigned to you yet.</span>
        </div>
      ) : (
        <div className="grid grid-cols-[repeat(auto-fit,_minmax(280px,_1fr))] gap-4 xl:grid-cols-[repeat(auto-fit,_minmax(350px,_1fr))]">
          {projects.map((project, index) => (
            <CardNote
              key={index}
              title={project.title}
              started={project.startDate}
              due={project.endDate}
              admin={project.createdByAdminName}
              createdAt={project.createdAt}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default EmployeeDashboard;
