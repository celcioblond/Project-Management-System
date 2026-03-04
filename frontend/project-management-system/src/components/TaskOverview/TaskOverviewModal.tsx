// src/components/TaskOverview/TaskOverviewModal.tsx
import {
  X,
  Flag,
  CircleDot,
  Calendar,
  Users,
  MessageSquare,
} from 'lucide-react';
import type { TaskResponse } from '../../services/api';
import TaskCommentSection from './TaskCommentSection';

const PRIORITY_LABELS: Record<string, string> = {
  CRITICAL: 'Critical',
  HIGH: 'High',
  MEDIUM: 'Medium',
  LOW: 'Low',
};

const PRIORITY_COLORS: Record<string, string> = {
  CRITICAL: 'bg-red-50 text-red-700 border-red-200',
  HIGH: 'bg-orange-50 text-orange-700 border-orange-200',
  MEDIUM: 'bg-amber-50 text-amber-700 border-amber-200',
  LOW: 'bg-emerald-50 text-emerald-700 border-emerald-200',
};

const STATUS_LABELS: Record<string, string> = {
  TODO: 'To Do',
  IN_PROGRESS: 'In Progress',
  IN_REVIEW: 'In Review',
  DONE: 'Done',
  COMPLETED: 'Completed',
};

const STATUS_COLORS: Record<string, string> = {
  TODO: 'bg-slate-50 text-slate-700 border-slate-200',
  IN_PROGRESS: 'bg-blue-50 text-blue-700 border-blue-200',
  IN_REVIEW: 'bg-violet-50 text-violet-700 border-violet-200',
  DONE: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  COMPLETED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
};

interface TaskOverviewModalProps {
  task: TaskResponse;
  onClose: () => void;
}

function fmtDateTime(iso?: string | null) {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

function fmtDate(iso?: string | null) {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

export default function TaskOverviewModal({
  task,
  onClose,
}: TaskOverviewModalProps) {
  const priorityLabel = PRIORITY_LABELS[task.priority] ?? task.priority;
  const priorityClasses =
    PRIORITY_COLORS[task.priority] ??
    'bg-slate-50 text-slate-700 border-slate-200';

  const statusLabel = STATUS_LABELS[task.status] ?? task.status;
  const statusClasses =
    STATUS_COLORS[task.status] ??
    'bg-slate-50 text-slate-700 border-slate-200';

  const assignees = task.assignedEmployeeNames ?? [];

  return (
    <div
      className="relative w-full max-w-4xl bg-white rounded-3xl shadow-2xl border border-slate-200 overflow-hidden"
      role="dialog"
      aria-modal="true"
    >
      {/* Header bar */}
      <div className="h-1.5 bg-gradient-to-r from-blue-500 via-violet-500 to-cyan-400" />

      <div className="p-6 sm:p-8">
        {/* Title row */}
        <div className="flex items-start justify-between gap-4">
          <div className="flex-1 min-w-0">
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">
              Task Overview
            </p>
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900 leading-tight">
              {task.title}
            </h1>
            {task.projectName && (
              <p className="text-xs text-slate-400 mt-1">
                Project:{' '}
                <span className="font-medium text-slate-600">
                  {task.projectName}
                </span>
              </p>
            )}
          </div>

          <button
            onClick={onClose}
            className="w-9 h-9 rounded-full flex items-center justify-center border border-slate-200 text-slate-400 hover:text-slate-700 hover:bg-slate-50 transition-colors shrink-0"
            aria-label="Close"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Description */}
        {task.description && (
          <p className="mt-4 text-sm text-slate-600 leading-relaxed">
            {task.description}
          </p>
        )}

        {/* Meta grid + comments */}
        <div className="mt-6 grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Left: meta */}
          <div className="space-y-4">
            {/* Priority / status row */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wide mb-1">
                  Priority
                </p>
                <span
                  className={`inline-flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1 rounded-full border ${priorityClasses}`}
                >
                  <Flag className="w-3 h-3" />
                  {priorityLabel}
                </span>
              </div>
              <div>
                <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wide mb-1">
                  Status
                </p>
                <span
                  className={`inline-flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1 rounded-full border ${statusClasses}`}
                >
                  <CircleDot className="w-3 h-3" />
                  {statusLabel}
                </span>
              </div>
            </div>

            {/* Dates & project */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
              <div>
                <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wide mb-1 flex items-center gap-1.5">
                  <Calendar className="w-3 h-3" />
                  Due Date
                </p>
                <p className="font-medium text-slate-800">
                  {fmtDate(task.dueDate)}
                </p>
              </div>
              <div>
                <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wide mb-1">
                  Created
                </p>
                <p className="font-medium text-slate-800">
                  {fmtDateTime(task.createdAt)}
                </p>
              </div>
            </div>

            {/* Assignment */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
              <div>
                <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wide mb-1 flex items-center gap-1.5">
                  <Users className="w-3 h-3" />
                  Assigned To
                </p>
                {assignees.length === 0 ? (
                  <p className="text-slate-400 text-sm">No assignees yet.</p>
                ) : (
                  <ul className="space-y-1">
                    {assignees.map((name) => (
                      <li
                        key={name}
                        className="text-sm font-medium text-slate-800"
                      >
                        {name}
                      </li>
                    ))}
                  </ul>
                )}
              </div>
              {task.assignedByAdminName && (
                <div>
                  <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wide mb-1">
                    Assigned By
                  </p>
                  <p className="font-medium text-slate-800">
                    {task.assignedByAdminName}
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* Right: comments */}
          <div className="space-y-3">
            <div className="flex items-center gap-2 text-xs font-semibold text-slate-500 uppercase tracking-wide">
              <MessageSquare className="w-3.5 h-3.5 text-blue-500" />
              Activity & Comments
            </div>
            <TaskCommentSection taskId={task.id} />
          </div>
        </div>
      </div>
    </div>
  );
}

