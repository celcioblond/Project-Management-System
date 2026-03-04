// src/pages/TaskOverviewPage.tsx
import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { apiService, type TaskResponse } from '../services/api';
import TaskOverviewModal from '../components/TaskOverview/TaskOverviewModal';

export default function TaskOverviewPage() {
  const { taskId } = useParams<{ taskId: string }>();
  const navigate = useNavigate();

  const [task, setTask] = useState<TaskResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadTask = useCallback(async () => {
    if (!taskId) return;
    try {
      setLoading(true);
      setTask(await apiService.getTaskById(Number(taskId)));
    } catch {
      setError('Failed to load task. You may not have access.');
    } finally {
      setLoading(false);
    }
  }, [taskId]);

  useEffect(() => {
    loadTask();
  }, [loadTask]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50">
        <div className="flex flex-col items-center gap-3">
          <svg
            className="animate-spin w-8 h-8 text-blue-500"
            viewBox="0 0 24 24"
            fill="none"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            />
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8v8z"
            />
          </svg>
          <p className="text-sm text-slate-400 font-medium">
            Loading task details…
          </p>
        </div>
      </div>
    );
  }

  if (error || !task) {
    return (
      <div className="min-h-screen flex items-center justify-center flex-col gap-4 bg-slate-50">
        <p className="text-red-500 font-medium">
          {error || 'Task not found'}
        </p>
        <button
          className="flex items-center gap-2 text-sm text-slate-600 hover:text-slate-900 transition-colors"
          onClick={() => navigate(-1)}
        >
          <ArrowLeft className="w-4 h-4" /> Go back
        </button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-100">
      {/* Dimmed background and centered modal */}
      <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center px-4 py-6">
        <TaskOverviewModal
          task={task}
          onClose={() => navigate(-1)}
        />
      </div>
    </div>
  );
}

