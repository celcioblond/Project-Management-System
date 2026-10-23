// src/components/EmployeeDashboard/CardNote.tsx
import { Calendar, Users, CheckSquare, CircleDot } from 'lucide-react';

interface CardNoteProps {
  title: string;
  description?: string | null;
  status: string;
  started: string;
  due: string;
  memberCount: number;
  taskCount: number;
  completedTaskCount: number;
  admin?: string | null;
}

const STATUS_LABELS: Record<string, string> = {
  NOT_STARTED: 'Not Started',
  TODO: 'To Do',
  IN_PROGRESS: 'In Progress',
  IN_REVIEW: 'In Review',
  ON_HOLD: 'On Hold',
  DONE: 'Done',
  COMPLETED: 'Completed',
};

const STATUS_COLORS: Record<string, string> = {
  NOT_STARTED: 'text-slate-600 bg-slate-50 border-slate-200',
  TODO: 'text-slate-600 bg-slate-50 border-slate-200',
  IN_PROGRESS: 'text-blue-600 bg-blue-50 border-blue-200',
  IN_REVIEW: 'text-violet-600 bg-violet-50 border-violet-200',
  ON_HOLD: 'text-amber-600 bg-amber-50 border-amber-200',
  DONE: 'text-emerald-600 bg-emerald-50 border-emerald-200',
  COMPLETED: 'text-emerald-600 bg-emerald-50 border-emerald-200',
};

const CardNote = ({
  title,
  description,
  status,
  started,
  due,
  memberCount,
  taskCount,
  completedTaskCount,
  admin,
}: CardNoteProps) => {
  const formatDate = (dateString: string) => {
    if (!dateString) return '—';
    const date = new Date(dateString);
    if (Number.isNaN(date.getTime())) return '—';
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  const statusLabel = STATUS_LABELS[status] ?? status;
  const statusClasses =
    STATUS_COLORS[status] ?? 'text-slate-600 bg-slate-50 border-slate-200';

  const progress =
    taskCount > 0 ? Math.round((completedTaskCount / taskCount) * 100) : 0;

  return (
    <div className="w-full rounded-2xl border border-slate-200 bg-white shadow-sm hover:shadow-md transition-shadow duration-200">
      <div className="p-5 flex flex-col gap-3">
        {/* Header */}
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <h2 className="text-base font-semibold text-slate-900 truncate">
              {title}
            </h2>
            {admin && (
              <p className="text-xs text-slate-400 mt-0.5">
                Assigned by <span className="font-medium">{admin}</span>
              </p>
            )}
          </div>
          <span
            className={`inline-flex items-center gap-1 text-[11px] font-semibold px-2 py-0.5 rounded-full border ${statusClasses}`}
          >
            <CircleDot className="w-2.5 h-2.5" />
            {statusLabel}
          </span>
        </div>

        {/* Description */}
        {description && (
          <p className="text-xs text-slate-500 leading-relaxed line-clamp-2">
            {description}
          </p>
        )}

        {/* Meta grid */}
        <div className="grid grid-cols-2 gap-3 text-[11px] text-slate-500 mt-1">
          <div className="flex flex-col gap-0.5">
            <span className="inline-flex items-center gap-1 text-slate-400 uppercase tracking-wide text-[10px] font-semibold">
              <Calendar className="w-3 h-3" />
              Start
            </span>
            <span className="font-medium text-slate-700">
              {formatDate(started)}
            </span>
          </div>
          <div className="flex flex-col gap-0.5">
            <span className="inline-flex items-center gap-1 text-slate-400 uppercase tracking-wide text-[10px] font-semibold">
              <Calendar className="w-3 h-3" />
              Deadline
            </span>
            <span className="font-medium text-slate-700">
              {formatDate(due)}
            </span>
          </div>
          <div className="flex flex-col gap-0.5">
            <span className="inline-flex items-center gap-1 text-slate-400 uppercase tracking-wide text-[10px] font-semibold">
              <Users className="w-3 h-3" />
              Team
            </span>
            <span className="font-medium text-slate-700">
              {memberCount} member{memberCount === 1 ? '' : 's'}
            </span>
          </div>
          <div className="flex flex-col gap-0.5">
            <span className="inline-flex items-center gap-1 text-slate-400 uppercase tracking-wide text-[10px] font-semibold">
              <CheckSquare className="w-3 h-3" />
              Tasks
            </span>
            <span className="font-medium text-slate-700">
              {taskCount > 0
                ? `${completedTaskCount} / ${taskCount} done`
                : 'No tasks yet'}
            </span>
          </div>
        </div>

        {/* Progress bar */}
        {taskCount > 0 && (
          <div className="mt-1.5">
            <div className="flex justify-between items-center text-[10px] text-slate-400 mb-1">
              <span>Progress</span>
              <span className="font-semibold text-slate-600">
                {progress}
                %
              </span>
            </div>
            <div className="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden">
              <div
                className="h-full rounded-full transition-all duration-500"
                style={{
                  width: `${progress}%`,
                  background: progress === 100 ? '#10b981' : '#3b82f6',
                }}
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default CardNote;
