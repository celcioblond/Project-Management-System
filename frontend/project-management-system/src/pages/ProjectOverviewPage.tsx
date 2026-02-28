// src/pages/ProjectOverviewPage.tsx
import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Calendar,
  Users,
  CheckSquare,
  MessageSquare,
  Clock,
  ChevronDown,
  ChevronUp,
  Pencil,
  Trash2,
  Plus,
  X,
  AlertTriangle,
  Check,
  ShieldCheck,
  Flag,
  CircleDot,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import {
  apiService,
  type ProjectResponse,
  type TaskResponse,
  type UserResponse,
  type TaskCreate,
  type TaskUpdate,
} from '../services/api';
import ProjectCommentSection from '../components/ProjectOverview/ProjectCommentSection';

// ─── Helpers ──────────────────────────────────────────────────────────────────

const fmt = (d: string | null | undefined) => {
  if (!d) return '—';
  try {
    return new Date(d).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  } catch {
    return d;
  }
};

const PRIORITY_STYLES: Record<
  string,
  { bar: string; badge: string; label: string }
> = {
  CRITICAL: {
    bar: 'bg-red-500',
    badge: 'text-red-600 bg-red-50 border-red-200',
    label: 'Critical',
  },
  HIGH: {
    bar: 'bg-orange-400',
    badge: 'text-orange-600 bg-orange-50 border-orange-200',
    label: 'High',
  },
  MEDIUM: {
    bar: 'bg-yellow-400',
    badge: 'text-yellow-600 bg-yellow-50 border-yellow-200',
    label: 'Medium',
  },
  LOW: {
    bar: 'bg-emerald-400',
    badge: 'text-emerald-600 bg-emerald-50 border-emerald-200',
    label: 'Low',
  },
};

const STATUS_STYLES: Record<
  string,
  { dot: string; badge: string; label: string }
> = {
  NOT_STARTED: {
    dot: 'bg-slate-400',
    badge: 'text-slate-600 bg-slate-50 border-slate-200',
    label: 'Not Started',
  },
  TODO: {
    dot: 'bg-slate-400',
    badge: 'text-slate-600 bg-slate-50 border-slate-200',
    label: 'To Do',
  },
  IN_PROGRESS: {
    dot: 'bg-blue-500',
    badge: 'text-blue-600 bg-blue-50 border-blue-200',
    label: 'In Progress',
  },
  IN_REVIEW: {
    dot: 'bg-violet-500',
    badge: 'text-violet-600 bg-violet-50 border-violet-200',
    label: 'In Review',
  },
  ON_HOLD: {
    dot: 'bg-amber-400',
    badge: 'text-amber-600 bg-amber-50 border-amber-200',
    label: 'On Hold',
  },
  DONE: {
    dot: 'bg-emerald-500',
    badge: 'text-emerald-600 bg-emerald-50 border-emerald-200',
    label: 'Done',
  },
  COMPLETED: {
    dot: 'bg-emerald-500',
    badge: 'text-emerald-600 bg-emerald-50 border-emerald-200',
    label: 'Completed',
  },
};

const TASK_STATUSES = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'];
const TASK_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

// ─── Avatar ───────────────────────────────────────────────────────────────────

const AVATAR_COLORS = [
  'bg-blue-500',
  'bg-violet-500',
  'bg-emerald-500',
  'bg-rose-500',
  'bg-amber-500',
  'bg-cyan-500',
  'bg-pink-500',
];
function Avatar({ name, size = 'sm' }: { name: string; size?: 'sm' | 'md' }) {
  const idx = (name?.charCodeAt(0) ?? 0) % AVATAR_COLORS.length;
  const sz = size === 'md' ? 'w-9 h-9 text-sm' : 'w-7 h-7 text-xs';
  return (
    <div
      className={`${sz} ${AVATAR_COLORS[idx]} rounded-full flex items-center justify-center text-white font-bold shrink-0`}
    >
      {name?.[0]?.toUpperCase() ?? '?'}
    </div>
  );
}

// ─── Toast ────────────────────────────────────────────────────────────────────

function Toast({
  show,
  message,
  type,
  onClose,
}: {
  show: boolean;
  message: string;
  type: 'success' | 'error';
  onClose: () => void;
}) {
  useEffect(() => {
    if (show) {
      const t = setTimeout(onClose, 3000);
      return () => clearTimeout(t);
    }
  }, [show, onClose]);
  if (!show) return null;
  return (
    <div
      className={`fixed bottom-6 right-6 z-50 flex items-center gap-3 px-4 py-3 rounded-xl shadow-2xl text-white text-sm font-medium ${type === 'success' ? 'bg-emerald-600' : 'bg-red-600'}`}
    >
      {type === 'success' ? (
        <Check className="w-4 h-4" />
      ) : (
        <AlertTriangle className="w-4 h-4" />
      )}
      {message}
      <button onClick={onClose}>
        <X className="w-3.5 h-3.5 opacity-70 hover:opacity-100" />
      </button>
    </div>
  );
}

// ─── Task Modal ───────────────────────────────────────────────────────────────

interface TaskFormModal {
  open: boolean;
  mode: 'create' | 'edit';
  task: TaskResponse | null;
}

function TaskModal({
  modal,
  onClose,
  onSave,
  employees,
  projectId,
  adminId,
}: {
  modal: TaskFormModal;
  onClose: () => void;
  onSave: (
    data: Partial<TaskCreate & TaskUpdate>,
  ) => Promise<void>;
  employees: UserResponse[];
  projectId: number;
  adminId: number | null;
}) {
  const [form, setForm] = useState<Record<string, unknown>>({});
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!modal.open) return;
    if (modal.mode === 'edit' && modal.task) {
      const t = modal.task;
      const empIds = employees
        .filter((e) => t.assignedEmployeeNames?.includes(e.name || e.username))
        .map((e) => e.id);
      setForm({
        title: t.title,
        description: t.description,
        status: t.status,
        priority: t.priority,
        dueDate: t.dueDate?.slice(0, 16) ?? '',
        assignedEmployeeIds: empIds,
        updatedByAdminId: adminId,
      });
    } else {
      setForm({
        status: 'TODO',
        priority: 'MEDIUM',
        projectId,
        assignedEmployeeIds: [],
        assignedByAdminId: adminId,
      });
    }
    setError('');
  }, [modal.open, modal.mode, modal.task, adminId, projectId, employees]);

  const set = (k: string, v: unknown) => setForm((p) => ({ ...p, [k]: v }));
  const toggleEmp = (id: number) => {
    const arr = (form.assignedEmployeeIds as number[]) ?? [];
    set(
      'assignedEmployeeIds',
      arr.includes(id) ? arr.filter((x) => x !== id) : [...arr, id],
    );
  };
  const handleSave = async () => {
    setSaving(true);
    setError('');
    try {
      await onSave({
        ...form,
        dueDate: form.dueDate ? (form.dueDate as string) + ':00' : undefined,
      } as Partial<TaskCreate & TaskUpdate>);
      onClose();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to save task');
    } finally {
      setSaving(false);
    }
  };

  if (!modal.open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between px-6 py-5 border-b border-slate-100">
          <h3 className="font-bold text-slate-800 text-lg">
            {modal.mode === 'create' ? 'New Task' : 'Edit Task'}
          </h3>
          <button
            onClick={onClose}
            className="w-8 h-8 flex items-center justify-center rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-700 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
        <div className="px-6 py-5 space-y-4">
          <div>
            <label className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1.5 block">
              Title
            </label>
            <input
              className="w-full border border-slate-300 rounded-lg px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-400 transition-colors"
              value={(form.title as string) ?? ''}
              onChange={(e) => set('title', e.target.value)}
              placeholder="Task title"
            />
          </div>
          <div>
            <label className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1.5 block">
              Description
            </label>
            <textarea
              className="w-full border border-slate-300 rounded-lg px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-400 transition-colors resize-none min-h-[80px]"
              value={(form.description as string) ?? ''}
              onChange={(e) => set('description', e.target.value)}
              placeholder="Optional description"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            {[
              { label: 'Status', key: 'status', opts: TASK_STATUSES },
              { label: 'Priority', key: 'priority', opts: TASK_PRIORITIES },
            ].map(({ label, key, opts }) => (
              <div key={key}>
                <label className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1.5 block">
                  {label}
                </label>
                <select
                  className="w-full border border-slate-300 rounded-lg px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-400 transition-colors appearance-none bg-white cursor-pointer"
                  value={(form[key] as string) ?? ''}
                  onChange={(e) => set(key, e.target.value)}
                >
                  {opts.map((o) => (
                    <option key={o} value={o}>
                      {o.replace('_', ' ')}
                    </option>
                  ))}
                </select>
              </div>
            ))}
          </div>
          <div>
            <label className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1.5 block">
              Due Date
            </label>
            <input
              type="datetime-local"
              className="w-full border border-slate-300 rounded-lg px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-400 transition-colors"
              value={(form.dueDate as string) ?? ''}
              onChange={(e) => set('dueDate', e.target.value)}
            />
          </div>
          <div>
            <label className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1.5 block">
              Assign Employees
            </label>
            <div className="border border-slate-200 rounded-lg max-h-36 overflow-y-auto divide-y divide-slate-100">
              {employees.length === 0 && (
                <p className="text-xs text-slate-400 p-3 text-center">
                  No employees found
                </p>
              )}
              {employees.map((e) => {
                const ids = (form.assignedEmployeeIds as number[]) ?? [];
                return (
                  <label
                    key={e.id}
                    className="flex items-center gap-3 px-3 py-2.5 hover:bg-slate-50 cursor-pointer"
                  >
                    <input
                      type="checkbox"
                      className="w-4 h-4 accent-blue-600"
                      checked={ids.includes(e.id)}
                      onChange={() => toggleEmp(e.id)}
                    />
                    <Avatar name={e.name || e.username} />
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-slate-800 truncate">
                        {e.name || e.username}
                      </p>
                      <p className="text-xs text-slate-400 truncate">
                        {e.position}
                      </p>
                    </div>
                  </label>
                );
              })}
            </div>
          </div>
          {error && (
            <div className="flex items-center gap-2 text-red-600 text-sm bg-red-50 rounded-lg px-3 py-2">
              <AlertTriangle className="w-4 h-4 shrink-0" />
              {error}
            </div>
          )}
        </div>
        <div className="flex justify-end gap-3 px-6 py-4 border-t border-slate-100">
          <button
            className="px-4 py-2 rounded-lg border border-slate-300 text-sm font-medium text-slate-700 hover:bg-slate-50 transition-colors"
            onClick={onClose}
            disabled={saving}
          >
            Cancel
          </button>
          <button
            className="px-5 py-2 rounded-lg bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold transition-colors disabled:opacity-60 flex items-center gap-2"
            onClick={handleSave}
            disabled={saving}
          >
            {saving && (
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
            {modal.mode === 'create' ? 'Create Task' : 'Save Changes'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function ProjectOverviewPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();
  const isAdmin = user?.role === 'ADMIN';

  const [project, setProject] = useState<ProjectResponse | null>(null);
  const [allUsers, setAllUsers] = useState<UserResponse[]>([]);
  const [adminId, setAdminId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [expandedTask, setExpandedTask] = useState<number | null>(null);
  const [taskModal, setTaskModal] = useState<TaskFormModal>({
    open: false,
    mode: 'create',
    task: null,
  });
  const [deleteTaskId, setDeleteTaskId] = useState<number | null>(null);
  const [inlineStatusEdit, setInlineStatusEdit] = useState<number | null>(null);
  const [toast, setToast] = useState({
    show: false,
    message: '',
    type: 'success' as 'success' | 'error',
  });

  const notify = (msg: string, type: 'success' | 'error' = 'success') =>
    setToast({ show: true, message: msg, type });

  const loadProject = useCallback(async () => {
    if (!projectId) return;
    try {
      setLoading(true);
      setProject(await apiService.getProjectById(Number(projectId)));
    } catch {
      setError('Failed to load project. You may not have access.');
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    loadProject();
    if (isAdmin) {
      apiService
        .getAllUsers()
        .then((users) => {
          setAllUsers(users);
          const me = users.find((u) => u.username === user?.username);
          if (me) setAdminId(me.id);
        })
        .catch(() => {});
    }
  }, [loadProject, isAdmin, user]);

  const handleStatusChange = async (taskId: number, newStatus: string) => {
    try {
      await apiService.updateTask(taskId, {
        status: newStatus,
        updatedByAdminId: adminId ?? undefined,
      });
      setInlineStatusEdit(null);
      await loadProject();
      notify('Status updated');
    } catch {
      notify('Failed to update status', 'error');
    }
  };

  const handleTaskSave = async (
    data: Partial<TaskCreate & TaskUpdate>,
  ) => {
    if (taskModal.mode === 'create')
      await apiService.createTask(data as TaskCreate, adminId!);
    else if (taskModal.task)
      await apiService.updateTask(taskModal.task.id, data as TaskUpdate);
    await loadProject();
    notify(taskModal.mode === 'create' ? 'Task created' : 'Task updated');
  };

  const handleDeleteTask = async () => {
    if (!deleteTaskId) return;
    try {
      await apiService.deleteTask(deleteTaskId);
      setDeleteTaskId(null);
      await loadProject();
      notify('Task deleted');
    } catch {
      notify('Failed to delete task', 'error');
    }
  };

  const employees = allUsers.filter((u) => u.role === 'EMPLOYEE');

  // ── Loading / Error ───────────────────────────────────────────────────────

  if (loading)
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
          <p className="text-sm text-slate-400 font-medium">Loading project…</p>
        </div>
      </div>
    );

  if (error || !project)
    return (
      <div className="min-h-screen flex items-center justify-center flex-col gap-4 bg-slate-50">
        <p className="text-red-500 font-medium">
          {error || 'Project not found'}
        </p>
        <button
          className="flex items-center gap-2 text-sm text-slate-600 hover:text-slate-900 transition-colors"
          onClick={() => navigate(-1)}
        >
          <ArrowLeft className="w-4 h-4" /> Go back
        </button>
      </div>
    );

  const completedTasks =
    project.tasks?.filter(
      (t) => t.status === 'DONE' || t.status === 'COMPLETED',
    ).length ?? 0;
  const totalTasks = project.tasks?.length ?? 0;
  const progress =
    totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0;
  const statusStyle =
    STATUS_STYLES[project.status] ?? STATUS_STYLES['NOT_STARTED'];
  const memberCount = project.assignedEmployeeNames?.length ?? 0;

  return (
    <div className="min-h-screen bg-slate-50 pb-20">
      {/* ── Top bar ──────────────────────────────────────────────────────────── */}
      <div className="bg-white border-b border-slate-200 px-6 py-3 flex items-center justify-between sticky top-0 z-20 shadow-sm">
        <button
          onClick={() =>
            navigate(isAdmin ? '/admin/dashboard' : '/employee/dashboard')
          }
          className="flex items-center gap-2 text-sm font-medium text-slate-500 hover:text-slate-900 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          {isAdmin ? 'Admin Console' : 'My Projects'}
        </button>
        {isAdmin && (
          <div className="flex items-center gap-1.5 text-xs font-semibold text-amber-700 bg-amber-50 border border-amber-200 px-2.5 py-1 rounded-full">
            <ShieldCheck className="w-3.5 h-3.5" /> Admin View
          </div>
        )}
      </div>

      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-8 space-y-6">
        {/* ── Hero card ────────────────────────────────────────────────────────── */}
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
          <div className="h-1.5 bg-gradient-to-r from-blue-500 via-violet-500 to-cyan-400" />
          <div className="p-6 sm:p-8">
            <div className="flex items-start justify-between gap-4 flex-wrap">
              <div className="flex-1 min-w-0">
                <span
                  className={`inline-flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1 rounded-full border mb-3 ${statusStyle.badge}`}
                >
                  <span
                    className={`w-1.5 h-1.5 rounded-full ${statusStyle.dot}`}
                  />
                  {statusStyle.label}
                </span>
                <h1 className="text-3xl font-black text-slate-900 tracking-tight">
                  {project.title}
                </h1>
                {project.description && (
                  <p className="text-slate-500 mt-2 text-sm leading-relaxed max-w-2xl">
                    {project.description}
                  </p>
                )}
              </div>
              {isAdmin && (
                <button
                  onClick={() =>
                    setTaskModal({ open: true, mode: 'create', task: null })
                  }
                  className="flex items-center gap-2 px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold transition-colors shadow-sm shrink-0"
                >
                  <Plus className="w-4 h-4" /> Add Task
                </button>
              )}
            </div>

            {/* Stats grid */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-6">
              {[
                {
                  icon: Calendar,
                  label: 'Start',
                  value: fmt(project.startDate),
                },
                {
                  icon: Calendar,
                  label: 'Deadline',
                  value: fmt(project.endDate),
                },
                {
                  icon: Users,
                  label: 'Members',
                  value: `${memberCount} assigned`,
                },
                {
                  icon: CheckSquare,
                  label: 'Tasks',
                  value: `${completedTasks} / ${totalTasks} done`,
                },
              ].map(({ icon: Icon, label, value }) => (
                <div
                  key={label}
                  className="bg-slate-50 rounded-xl px-4 py-3 border border-slate-100"
                >
                  <div className="flex items-center gap-1.5 text-slate-400 mb-1">
                    <Icon className="w-3.5 h-3.5" />
                    <span className="text-xs font-medium uppercase tracking-wide">
                      {label}
                    </span>
                  </div>
                  <p className="text-sm font-semibold text-slate-800">
                    {value}
                  </p>
                </div>
              ))}
            </div>

            {/* Progress */}
            {totalTasks > 0 && (
              <div className="mt-5">
                <div className="flex justify-between text-xs text-slate-400 mb-1.5">
                  <span>Overall progress</span>
                  <span className="font-semibold text-slate-600">
                    {progress}%
                  </span>
                </div>
                <div className="w-full h-2 bg-slate-100 rounded-full overflow-hidden">
                  <div
                    className="h-full rounded-full transition-all duration-700"
                    style={{
                      width: `${progress}%`,
                      background: progress === 100 ? '#10b981' : '#3b82f6',
                    }}
                  />
                </div>
              </div>
            )}

            {project.createdByAdminName && (
              <p className="text-xs text-slate-400 mt-4 flex items-center gap-1.5">
                <ShieldCheck className="w-3.5 h-3.5" />
                Created by{' '}
                <span className="font-semibold text-slate-500 ml-1">
                  {project.createdByAdminName}
                </span>
              </p>
            )}
          </div>
        </div>

        {/* ── Body: Tasks (wide) + Sidebar (narrow) ────────────────────────────── */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Tasks column — takes 2/3 */}
          <div className="lg:col-span-2 space-y-3">
            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
              <div className="flex items-center gap-2 px-5 py-4 border-b border-slate-100">
                <CheckSquare className="w-4 h-4 text-blue-500" />
                <h2 className="font-bold text-sm text-slate-800">Tasks</h2>
                <span className="ml-auto text-xs bg-blue-50 text-blue-600 border border-blue-200 px-2 py-0.5 rounded-full font-mono">
                  {totalTasks}
                </span>
              </div>

              {!project.tasks || project.tasks.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-16 gap-2 text-slate-400">
                  <CheckSquare className="w-8 h-8 opacity-30" />
                  <p className="text-sm">
                    No tasks yet{isAdmin ? ' — click Add Task above' : ''}.
                  </p>
                </div>
              ) : (
                <div className="divide-y divide-slate-100">
                  {project.tasks.map((task) => {
                    const pStyle =
                      PRIORITY_STYLES[task.priority] ?? PRIORITY_STYLES['LOW'];
                    const sStyle =
                      STATUS_STYLES[task.status] ?? STATUS_STYLES['TODO'];
                    const isExpanded = expandedTask === task.id;
                    const isDone =
                      task.status === 'DONE' || task.status === 'COMPLETED';

                    return (
                      <div
                        key={task.id}
                        className={`transition-colors ${isExpanded ? 'bg-slate-50' : 'hover:bg-slate-50/60'}`}
                      >
                        <div
                          className="flex items-start gap-0 cursor-pointer"
                          onClick={() =>
                            setExpandedTask(isExpanded ? null : task.id)
                          }
                        >
                          {/* Priority colour bar */}
                          <div
                            className={`w-1 self-stretch rounded-bl-none rounded-tl-none shrink-0 ${pStyle.bar}`}
                          />

                          <div className="flex-1 flex items-start gap-3 px-4 py-4 min-w-0">
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center gap-2 flex-wrap">
                                <span
                                  className={`text-sm font-semibold ${isDone ? 'line-through text-slate-400' : 'text-slate-800'}`}
                                >
                                  {task.title}
                                </span>
                                <span
                                  className={`inline-flex items-center gap-1 text-xs font-semibold px-2 py-0.5 rounded-full border ${pStyle.badge}`}
                                >
                                  <Flag className="w-2.5 h-2.5" />
                                  {pStyle.label}
                                </span>
                              </div>

                              <div className="flex items-center gap-3 mt-1.5 flex-wrap">
                                {/* Status badge — clickable for admin */}
                                {isAdmin && inlineStatusEdit === task.id ? (
                                  <div
                                    className="flex items-center gap-1"
                                    onClick={(e) => e.stopPropagation()}
                                  >
                                    <select
                                      className="text-xs border border-slate-300 rounded-lg px-2 py-1 outline-none bg-white appearance-none"
                                      defaultValue={task.status}
                                      onChange={(e) =>
                                        handleStatusChange(
                                          task.id,
                                          e.target.value,
                                        )
                                      }
                                      autoFocus
                                    >
                                      {TASK_STATUSES.map((s) => (
                                        <option key={s} value={s}>
                                          {s.replace('_', ' ')}
                                        </option>
                                      ))}
                                    </select>
                                    <button
                                      onClick={(e) => {
                                        e.stopPropagation();
                                        setInlineStatusEdit(null);
                                      }}
                                      className="w-5 h-5 flex items-center justify-center rounded hover:bg-slate-200 text-slate-400"
                                    >
                                      <X className="w-3 h-3" />
                                    </button>
                                  </div>
                                ) : (
                                  <span
                                    className={`inline-flex items-center gap-1.5 text-xs font-semibold px-2 py-0.5 rounded-full border ${sStyle.badge} ${isAdmin ? 'cursor-pointer hover:ring-1 ring-blue-400 ring-offset-1' : ''}`}
                                    onClick={
                                      isAdmin
                                        ? (e) => {
                                            e.stopPropagation();
                                            setInlineStatusEdit(task.id);
                                          }
                                        : undefined
                                    }
                                    title={
                                      isAdmin
                                        ? 'Click to change status'
                                        : undefined
                                    }
                                  >
                                    <CircleDot className="w-2.5 h-2.5" />
                                    {sStyle.label}
                                  </span>
                                )}

                                <span className="flex items-center gap-1 text-xs text-slate-400">
                                  <Clock className="w-3 h-3" />
                                  {fmt(task.dueDate)}
                                </span>

                                {(task.assignedEmployeeNames?.length ?? 0) >
                                  0 && (
                                  <span className="flex items-center gap-1 text-xs text-slate-400">
                                    <Users className="w-3 h-3" />
                                    {task.assignedEmployeeNames
                                      .slice(0, 2)
                                      .join(', ')}
                                    {task.assignedEmployeeNames.length > 2 &&
                                      ` +${task.assignedEmployeeNames.length - 2}`}
                                  </span>
                                )}

                                {(task.comments?.length ?? 0) > 0 && (
                                  <span className="flex items-center gap-1 text-xs text-slate-400">
                                    <MessageSquare className="w-3 h-3" />
                                    {task.comments.length}
                                  </span>
                                )}
                              </div>
                            </div>

                            <div className="flex items-center gap-1 shrink-0">
                              {isAdmin && (
                                <>
                                  <button
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      setTaskModal({
                                        open: true,
                                        mode: 'edit',
                                        task,
                                      });
                                    }}
                                    className="w-7 h-7 flex items-center justify-center rounded-lg hover:bg-slate-200 text-slate-400 hover:text-slate-700 transition-colors"
                                    title="Edit"
                                  >
                                    <Pencil className="w-3.5 h-3.5" />
                                  </button>
                                  <button
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      setDeleteTaskId(task.id);
                                    }}
                                    className="w-7 h-7 flex items-center justify-center rounded-lg hover:bg-red-50 text-slate-400 hover:text-red-500 transition-colors"
                                    title="Delete"
                                  >
                                    <Trash2 className="w-3.5 h-3.5" />
                                  </button>
                                </>
                              )}
                              {isExpanded ? (
                                <ChevronUp className="w-4 h-4 text-slate-400" />
                              ) : (
                                <ChevronDown className="w-4 h-4 text-slate-400" />
                              )}
                            </div>
                          </div>
                        </div>

                        {/* Expanded details */}
                        {isExpanded && (
                          <div className="ml-1 px-8 pb-5 pt-3 border-t border-slate-100 space-y-3 bg-slate-50/80">
                            {task.description && (
                              <p className="text-sm text-slate-600 leading-relaxed">
                                {task.description}
                              </p>
                            )}
                            {task.assignedByAdminName && (
                              <p className="text-xs text-slate-400 flex items-center gap-1.5">
                                <ShieldCheck className="w-3.5 h-3.5" />
                                Assigned by {task.assignedByAdminName}
                              </p>
                            )}
                            {(task.comments?.length ?? 0) > 0 && (
                              <div className="space-y-3 pt-1">
                                <p className="text-xs font-bold uppercase tracking-wide text-slate-400">
                                  Task Comments
                                </p>
                                {task.comments.map((c) => (
                                  <div key={c.id} className="flex gap-2.5">
                                    <Avatar name={c.authorName ?? '?'} />
                                    <div>
                                      <div className="flex items-baseline gap-2">
                                        <span className="text-xs font-semibold text-slate-700">
                                          {c.authorName}
                                        </span>
                                        <span className="text-xs text-slate-400">
                                          {fmt(c.createdAt)}
                                        </span>
                                      </div>
                                      <p className="text-sm text-slate-600 mt-0.5">
                                        {c.content}
                                      </p>
                                    </div>
                                  </div>
                                ))}
                              </div>
                            )}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>

          {/* Sidebar: Team + Comments */}
          <div className="space-y-6">
            {/* Team members */}
            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-5">
              <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-4 flex items-center gap-2">
                <Users className="w-3.5 h-3.5" /> Project Team
              </h2>
              {memberCount === 0 ? (
                <p className="text-sm text-slate-400 text-center py-4">
                  No members assigned.
                </p>
              ) : (
                <div className="space-y-3">
                  {project.assignedEmployeeNames.map((name) => {
                    const emp = allUsers.find((u) => u.name === name);
                    return (
                      <div key={name} className="flex items-center gap-3">
                        <Avatar name={name} size="md" />
                        <div className="min-w-0">
                          <p className="text-sm font-semibold text-slate-800 truncate">
                            {name}
                          </p>
                          <p className="text-xs text-slate-400 truncate">
                            {emp?.position ||
                              emp?.department ||
                              emp?.email ||
                              'Employee'}
                          </p>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Project Comments */}
            <ProjectCommentSection
              projectId={project.id}
              onCommentPosted={loadProject}
            />
          </div>
        </div>
      </div>

      {/* ── Task Modal ────────────────────────────────────────────────────────── */}
      {isAdmin && (
        <TaskModal
          modal={taskModal}
          onClose={() => setTaskModal((p) => ({ ...p, open: false }))}
          onSave={handleTaskSave}
          employees={employees}
          projectId={project.id}
          adminId={adminId}
        />
      )}

      {/* ── Delete confirm ────────────────────────────────────────────────────── */}
      {isAdmin && deleteTaskId !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl max-w-sm w-full p-6 space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-red-50 flex items-center justify-center shrink-0">
                <AlertTriangle className="w-5 h-5 text-red-500" />
              </div>
              <div>
                <h3 className="font-bold text-slate-800">Delete Task</h3>
                <p className="text-xs text-slate-400 mt-0.5">
                  This cannot be undone.
                </p>
              </div>
            </div>
            <p className="text-sm text-slate-600">
              This task and all its comments will be permanently removed.
            </p>
            <div className="flex justify-end gap-3">
              <button
                className="px-4 py-2 rounded-lg border border-slate-300 text-sm font-medium text-slate-700 hover:bg-slate-50 transition-colors"
                onClick={() => setDeleteTaskId(null)}
              >
                Cancel
              </button>
              <button
                className="px-4 py-2 rounded-lg bg-red-500 hover:bg-red-600 text-white text-sm font-semibold transition-colors"
                onClick={handleDeleteTask}
              >
                Yes, Delete
              </button>
            </div>
          </div>
        </div>
      )}

      <Toast
        show={toast.show}
        message={toast.message}
        type={toast.type}
        onClose={() => setToast((p) => ({ ...p, show: false }))}
      />
    </div>
  );
}
