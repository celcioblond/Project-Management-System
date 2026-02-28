// src/components/AdminDashboard/EditTaskModal.tsx
import { useState, useEffect } from 'react';
import {
  X,
  CheckSquare,
  AlignLeft,
  Activity,
  Flag,
  Calendar,
  Search,
} from 'lucide-react';
import { sileo } from 'sileo';
import {
  apiService,
  type TaskResponse,
  type TaskUpdate,
  type UserResponse,
} from '../../services/api';

// ─── Constants ────────────────────────────────────────────────────────────────

const STATUSES = ['NOT_STARTED', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED'];
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

const STATUS_LABELS: Record<string, string> = {
  NOT_STARTED: 'Not Started',
  IN_PROGRESS: 'In Progress',
  ON_HOLD: 'On Hold',
  COMPLETED: 'Completed',
};

const PRIORITY_LABELS: Record<string, string> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  CRITICAL: 'Critical',
};

// ─── Types ────────────────────────────────────────────────────────────────────

interface EditTaskModalProps {
  open: boolean;
  task: TaskResponse | null;
  adminId: number | null;
  employees: UserResponse[];
  onClose: () => void;
  onUpdated: () => void;
}

interface FormState {
  title: string;
  description: string;
  status: string;
  priority: string;
  dueDate: string;
}

// ─── Shared sub-components ────────────────────────────────────────────────────

function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="grid grid-cols-[140px_1fr] items-start gap-3">
      <label className="text-sm font-medium text-slate-700 pt-2.5">
        {label}
      </label>
      <div className="flex flex-col gap-1">
        {children}
        {error && <p className="text-xs text-red-500">{error}</p>}
      </div>
    </div>
  );
}

function IconInput({
  icon: Icon,
  type = 'text',
  placeholder,
  value,
  onChange,
  disabled,
  hasError,
}: {
  icon: React.ElementType;
  type?: string;
  placeholder: string;
  value: string;
  onChange: (v: string) => void;
  disabled?: boolean;
  hasError?: boolean;
}) {
  return (
    <div
      className={`flex items-center gap-2.5 border rounded-lg px-3 py-2.5 bg-white transition-colors focus-within:ring-2 focus-within:ring-blue-500/30 focus-within:border-blue-400 ${hasError ? 'border-red-400' : 'border-slate-300'}`}
    >
      <Icon className="w-4 h-4 text-slate-400 shrink-0" />
      <input
        type={type}
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
        className="flex-1 text-sm bg-transparent outline-none text-slate-800 placeholder:text-slate-400 disabled:opacity-50"
      />
    </div>
  );
}

function IconSelect({
  icon: Icon,
  placeholder,
  value,
  onChange,
  options,
  disabled,
  hasError,
}: {
  icon: React.ElementType;
  placeholder: string;
  value: string;
  onChange: (v: string) => void;
  options: { value: string; label: string }[];
  disabled?: boolean;
  hasError?: boolean;
}) {
  return (
    <div
      className={`flex items-center gap-2.5 border rounded-lg px-3 py-2.5 bg-white transition-colors focus-within:ring-2 focus-within:ring-blue-500/30 focus-within:border-blue-400 ${hasError ? 'border-red-400' : 'border-slate-300'}`}
    >
      <Icon className="w-4 h-4 text-slate-400 shrink-0" />
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
        className="flex-1 text-sm bg-transparent outline-none text-slate-800 disabled:opacity-50 cursor-pointer appearance-none"
      >
        <option value="">{placeholder}</option>
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      <svg
        className="w-4 h-4 text-slate-400 shrink-0 pointer-events-none"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M19 9l-7 7-7-7"
        />
      </svg>
    </div>
  );
}

function toDateInput(iso: string): string {
  if (!iso) return '';
  return iso.split('T')[0];
}

// ─── Modal ────────────────────────────────────────────────────────────────────

export default function EditTaskModal({
  open,
  task,
  adminId,
  employees,
  onClose,
  onUpdated,
}: EditTaskModalProps) {
  const [form, setForm] = useState<FormState>({
    title: '',
    description: '',
    status: '',
    priority: '',
    dueDate: '',
  });
  const [errors, setErrors] = useState<Partial<FormState>>({});
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [search, setSearch] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Pre-populate form when a task is passed in
  useEffect(() => {
    if (open && task) {
      setForm({
        title: task.title ?? '',
        description: task.description ?? '',
        status: task.status ?? '',
        priority: task.priority ?? '',
        dueDate: toDateInput(task.dueDate),
      });

      // Match assignedEmployeeNames back to IDs
      const preSelected = employees
        .filter((e) => task.assignedEmployeeNames?.includes(e.name))
        .map((e) => e.id);
      setSelectedIds(preSelected);

      setErrors({});
      setSearch('');
    }
  }, [open, task, employees]);

  // Close on Escape
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, onClose]);

  const set = (key: keyof FormState, value: string) => {
    setForm((p) => ({ ...p, [key]: value }));
    setErrors((p) => ({ ...p, [key]: undefined }));
  };

  const toggleEmployee = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    );
  };

  const filteredEmployees = employees
    .filter((e) => e.role === 'EMPLOYEE')
    .filter(
      (e) =>
        e.name.toLowerCase().includes(search.toLowerCase()) ||
        e.username.toLowerCase().includes(search.toLowerCase()),
    );

  // ── Validation ────────────────────────────────────────────────────────────

  const validate = (): boolean => {
    const e: Partial<FormState> = {};

    if (!form.title.trim()) e.title = 'Task title is required.';
    if (!form.status) e.status = 'Select a status.';
    if (!form.priority) e.priority = 'Select a priority.';
    if (!form.dueDate) e.dueDate = 'Due date is required.';

    setErrors(e);

    if (selectedIds.length === 0) {
      sileo.warning({
        title: 'No assignee selected',
        description: 'Please assign at least one employee to this task.',
      });
      return false;
    }

    return Object.keys(e).length === 0;
  };

  // ── Submit ────────────────────────────────────────────────────────────────

  const handleSubmit = async () => {
    if (!task || !validate() || adminId === null) return;

    const payload: TaskUpdate = {
      title: form.title.trim(),
      description: form.description.trim(),
      status: form.status,
      priority: form.priority,
      dueDate: `${form.dueDate}T00:00:00`,
      updatedByAdminId: adminId,
      assignedEmployeeIds: selectedIds,
    };

    setSubmitting(true);
    try {
      await apiService.updateTask(task.id, payload);
      sileo.success({
        title: 'Task updated',
        description: `"${payload.title}" has been saved.`,
      });
      onUpdated();
      onClose();
    } catch (e) {
      sileo.error({ title: 'Failed to update task', description: String(e) });
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
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto animate-in fade-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-5 border-b border-slate-100">
          <div>
            <h2 className="text-xl font-bold text-slate-800">Edit Task</h2>
            {/* Read-only project context */}
            <p className="text-xs text-slate-400 mt-0.5">
              Project:{' '}
              <span className="font-medium text-slate-500">
                {task.projectName || '—'}
              </span>
            </p>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 flex items-center justify-center rounded-full hover:bg-slate-100 transition-colors text-slate-500 hover:text-slate-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form */}
        <div className="px-6 py-5 space-y-4">
          <Field label="Task Title:" error={errors.title}>
            <IconInput
              icon={CheckSquare}
              placeholder="Task title"
              value={form.title}
              onChange={(v) => set('title', v)}
              disabled={submitting}
              hasError={!!errors.title}
            />
          </Field>

          <Field label="Description:">
            <div className="flex items-start gap-2.5 border border-slate-300 rounded-lg px-3 py-2.5 bg-white focus-within:ring-2 focus-within:ring-blue-500/30 focus-within:border-blue-400 transition-colors">
              <AlignLeft className="w-4 h-4 text-slate-400 shrink-0 mt-0.5" />
              <textarea
                placeholder="Brief description (optional)"
                value={form.description}
                onChange={(e) => set('description', e.target.value)}
                disabled={submitting}
                rows={3}
                className="flex-1 text-sm bg-transparent outline-none text-slate-800 placeholder:text-slate-400 disabled:opacity-50 resize-none"
              />
            </div>
          </Field>

          {/* Project is read-only on edit */}
          <Field label="Project:">
            <div className="flex items-center gap-2.5 border border-slate-200 rounded-lg px-3 py-2.5 bg-slate-50">
              <span className="flex-1 text-sm text-slate-400">
                {task.projectName || '—'}
              </span>
              <span className="text-xs text-slate-300 italic">
                cannot change
              </span>
            </div>
          </Field>

          <Field label="Status:" error={errors.status}>
            <IconSelect
              icon={Activity}
              placeholder="Select status"
              value={form.status}
              onChange={(v) => set('status', v)}
              options={STATUSES.map((s) => ({
                value: s,
                label: STATUS_LABELS[s],
              }))}
              disabled={submitting}
              hasError={!!errors.status}
            />
          </Field>

          <Field label="Priority:" error={errors.priority}>
            <IconSelect
              icon={Flag}
              placeholder="Select priority"
              value={form.priority}
              onChange={(v) => set('priority', v)}
              options={PRIORITIES.map((p) => ({
                value: p,
                label: PRIORITY_LABELS[p],
              }))}
              disabled={submitting}
              hasError={!!errors.priority}
            />
          </Field>

          <Field label="Due Date:" error={errors.dueDate}>
            <IconInput
              icon={Calendar}
              type="date"
              placeholder=""
              value={form.dueDate}
              onChange={(v) => set('dueDate', v)}
              disabled={submitting}
              hasError={!!errors.dueDate}
            />
          </Field>

          {/* Employee assignment */}
          <Field label="Assign To:">
            <div className="border border-slate-300 rounded-lg overflow-hidden">
              {/* Search */}
              <div className="flex items-center gap-2 px-3 py-2 border-b border-slate-200 bg-slate-50">
                <Search className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                <input
                  type="text"
                  placeholder="Search employees…"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  disabled={submitting}
                  className="flex-1 text-xs bg-transparent outline-none text-slate-700 placeholder:text-slate-400 disabled:opacity-50"
                />
              </div>

              {/* List */}
              <div className="max-h-40 overflow-y-auto divide-y divide-slate-100">
                {filteredEmployees.length === 0 ? (
                  <p className="text-xs text-slate-400 px-3 py-3 text-center">
                    No matches for your search.
                  </p>
                ) : (
                  filteredEmployees.map((emp) => {
                    const checked = selectedIds.includes(emp.id);
                    return (
                      <label
                        key={emp.id}
                        className={`flex items-center gap-3 px-3 py-2.5 cursor-pointer transition-colors hover:bg-slate-50 ${checked ? 'bg-blue-50/60' : ''}`}
                      >
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={() => toggleEmployee(emp.id)}
                          disabled={submitting}
                          className="w-4 h-4 accent-blue-600 shrink-0"
                        />
                        <div className="min-w-0">
                          <p className="text-sm font-medium text-slate-800 truncate">
                            {emp.name}
                          </p>
                          <p className="text-xs text-slate-400 truncate">
                            @{emp.username} ·{' '}
                            {emp.position || emp.department || 'Employee'}
                          </p>
                        </div>
                      </label>
                    );
                  })
                )}
              </div>

              {/* Selected count */}
              {selectedIds.length > 0 && (
                <div className="px-3 py-2 bg-blue-50 border-t border-blue-100">
                  <p className="text-xs text-blue-600 font-medium">
                    {selectedIds.length} employee
                    {selectedIds.length > 1 ? 's' : ''} selected
                  </p>
                </div>
              )}
            </div>
          </Field>
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
            onClick={handleSubmit}
            disabled={submitting || adminId === null}
            className="px-6 py-2.5 rounded-lg bg-blue-600 hover:bg-blue-700 active:scale-95 text-white text-sm font-semibold transition-all duration-150 disabled:opacity-60 flex items-center gap-2"
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
            {submitting ? 'Saving…' : 'Save Changes'}
          </button>
        </div>
      </div>
    </div>
  );
}
