// src/components/AdminDashboard/DeleteTaskModal.tsx
import { useEffect, useState } from 'react';
import { X, TriangleAlert } from 'lucide-react';
import { sileo } from 'sileo';
import { apiService, type TaskResponse } from '../../services/api';

interface DeleteTaskModalProps {
  open: boolean;
  task: TaskResponse | null;
  onClose: () => void;
  onDeleted: () => void;
}

export default function DeleteTaskModal({
  open,
  task,
  onClose,
  onDeleted,
}: DeleteTaskModalProps) {
  const [submitting, setSubmitting] = useState(false);

  // Close on Escape
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, onClose]);

  const handleDelete = async () => {
    if (!task) return;
    setSubmitting(true);
    try {
      await apiService.deleteTask(task.id);
      sileo.success({
        title: 'Task deleted',
        description: `"${task.title}" has been removed.`,
      });
      onDeleted();
      onClose();
    } catch (e) {
      sileo.error({ title: 'Failed to delete task', description: String(e) });
    } finally {
      setSubmitting(false);
    }
  };

  if (!open || !task) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md animate-in fade-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-5 border-b border-slate-100">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-full bg-red-50 flex items-center justify-center shrink-0">
              <TriangleAlert className="w-5 h-5 text-red-500" />
            </div>
            <h2 className="text-xl font-bold text-slate-800">Delete Task</h2>
          </div>
          <button
            onClick={onClose}
            disabled={submitting}
            className="w-8 h-8 flex items-center justify-center rounded-full hover:bg-slate-100 transition-colors text-slate-500 hover:text-slate-800 disabled:opacity-50"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="px-6 py-6 space-y-4">
          <p className="text-sm text-slate-600 leading-relaxed">
            Are you sure you want to delete this task? This action{' '}
            <span className="font-semibold text-slate-800">
              cannot be undone
            </span>
            .
          </p>

          {/* Task info card */}
          <div className="bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 space-y-1.5">
            <p className="text-sm font-semibold text-slate-800">{task.title}</p>
            {task.description && (
              <p className="text-xs text-slate-500 line-clamp-2">
                {task.description}
              </p>
            )}
            <div className="flex items-center gap-3 pt-0.5">
              {task.projectName && (
                <>
                  <span className="text-xs text-slate-400">
                    Project: {task.projectName}
                  </span>
                  <span className="text-xs text-slate-300">·</span>
                </>
              )}
              <span className="text-xs text-slate-400">{task.priority}</span>
              <span className="text-xs text-slate-300">·</span>
              <span className="text-xs text-slate-400">
                {task.status?.replace('_', ' ')}
              </span>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-slate-100">
          <button
            onClick={onClose}
            disabled={submitting}
            className="px-5 py-2.5 rounded-lg border border-slate-300 text-sm font-medium text-slate-700 hover:bg-slate-50 active:scale-95 transition-all duration-150 disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            onClick={handleDelete}
            disabled={submitting}
            className="px-6 py-2.5 rounded-lg bg-red-500 hover:bg-red-600 active:scale-95 text-white text-sm font-semibold transition-all duration-150 disabled:opacity-60 flex items-center gap-2"
          >
            {submitting && (
              <svg
                className="animate-spin w-4 h-4"
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
            )}
            {submitting ? 'Deleting…' : 'Yes, Delete'}
          </button>
        </div>
      </div>
    </div>
  );
}
