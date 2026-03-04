// src/components/AdminDashboard/CreateProjectModal.tsx
import { useState, useEffect } from 'react';
import {
  X,
  FolderOpen,
  AlignLeft,
  Activity,
  Calendar,
  Search,
  Tag,
  Sparkles,
  ImageIcon,
} from 'lucide-react';
import { sileo } from 'sileo';
import {
  apiService,
  type ProjectCreate,
  type UserResponse,
} from '../../services/api';

// ─── Constants ────────────────────────────────────────────────────────────────

const STATUSES = ['NOT_STARTED', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED'];

const STATUS_LABELS: Record<string, string> = {
  NOT_STARTED: 'Not Started',
  IN_PROGRESS: 'In Progress',
  ON_HOLD: 'On Hold',
  COMPLETED: 'Completed',
};

// ─── Types ────────────────────────────────────────────────────────────────────

interface CreateProjectModalProps {
  open: boolean;
  adminId: number | null;
  employees: UserResponse[];
  onClose: () => void;
  onCreated: () => void;
}

interface FormState {
  name: string;
  type: string;
  description: string;
  status: string;
  startDate: string;
  endDate: string;
}

const EMPTY_FORM: FormState = {
  name: '',
  type: '',
  description: '',
  status: '',
  startDate: '',
  endDate: '',
};

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

function Spinner({ className = 'w-3 h-3' }: { className?: string }) {
  return (
    <svg
      className={`animate-spin ${className}`}
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
  );
}

// ─── Modal ────────────────────────────────────────────────────────────────────

export default function CreateProjectModal({
  open,
  adminId,
  employees,
  onClose,
  onCreated,
}: CreateProjectModalProps) {
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [errors, setErrors] = useState<
    Partial<FormState & { assignees: string }>
  >({});
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [search, setSearch] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // ── AI description ───────────────────────────────────────────────────────
  const [generating, setGenerating] = useState(false);

  // ── AI diagram ───────────────────────────────────────────────────────────
  const [generatingDiagram, setGeneratingDiagram] = useState(false);
  const [diagramBlob, setDiagramBlob] = useState<Blob | null>(null);
  const [diagramPreviewUrl, setDiagramPreviewUrl] = useState<string | null>(
    null,
  );

  // Reset on open
  useEffect(() => {
    if (open) {
      setForm(EMPTY_FORM);
      setErrors({});
      setSelectedIds([]);
      setSearch('');
      setDiagramBlob(null);
      setDiagramPreviewUrl(null);
    }
  }, [open]);

  // Revoke object URL on cleanup to avoid memory leaks
  useEffect(() => {
    return () => {
      if (diagramPreviewUrl) URL.revokeObjectURL(diagramPreviewUrl);
    };
  }, [diagramPreviewUrl]);

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
    setErrors((p) => ({ ...p, assignees: undefined }));
  };

  const filteredEmployees = employees.filter(
    (e) =>
      e.role === 'EMPLOYEE' &&
      (e.name.toLowerCase().includes(search.toLowerCase()) ||
        e.username.toLowerCase().includes(search.toLowerCase())),
  );

  // ── Gate conditions (same pattern as canGenerateDescription / canGenerateImage in AddProduct) ──
  const canGenerateDescription = !!form.name.trim() && !!form.type.trim();
  const canGenerateDiagram =
    !!form.name.trim() && !!form.type.trim() && !!form.description.trim();

  // ── AI description handler ────────────────────────────────────────────────

  const handleGenerateDescription = async () => {
    if (!canGenerateDescription) {
      sileo.error({
        title: 'Missing fields',
        description: 'Enter a project name and type before generating.',
      });
      return;
    }
    setGenerating(true);
    try {
      const desc = await apiService.generateProject(
        form.name.trim(),
        form.type.trim(),
      );
      set('description', desc);
      sileo.success({
        title: 'Description generated',
        description: 'Feel free to edit it before submitting.',
      });
    } catch (e) {
      sileo.error({ title: 'Generation failed', description: String(e) });
    } finally {
      setGenerating(false);
    }
  };

  // ── AI diagram handler ────────────────────────────────────────────────────

  const handleGenerateDiagram = async () => {
    if (!canGenerateDiagram) {
      sileo.error({
        title: 'Missing fields',
        description:
          'Name, type, and description are all required to generate a diagram.',
      });
      return;
    }
    setGeneratingDiagram(true);
    try {
      const blob = await apiService.generateDiagram(
        form.name.trim(),
        form.type.trim(),
        form.description.trim(),
      );
      if (diagramPreviewUrl) URL.revokeObjectURL(diagramPreviewUrl);
      const url = URL.createObjectURL(blob);
      setDiagramBlob(blob);
      setDiagramPreviewUrl(url);
      sileo.success({ title: 'Diagram generated' });
    } catch (e) {
      sileo.error({
        title: 'Failed to generate diagram',
        description: String(e),
      });
    } finally {
      setGeneratingDiagram(false);
    }
  };

  const handleRemoveDiagram = () => {
    if (diagramPreviewUrl) URL.revokeObjectURL(diagramPreviewUrl);
    setDiagramBlob(null);
    setDiagramPreviewUrl(null);
  };

  // ── Validation ────────────────────────────────────────────────────────────

  const validate = (): boolean => {
    const e: Partial<FormState & { assignees: string }> = {};
    if (!form.name.trim()) e.name = 'Project name is required.';
    if (!form.type.trim()) e.type = 'Project type is required.';
    if (!form.status) e.status = 'Select a status.';
    if (!form.startDate) e.startDate = 'Start date is required.';
    if (!form.endDate) e.endDate = 'End date is required.';
    else if (
      form.startDate &&
      new Date(form.endDate) < new Date(form.startDate)
    )
      e.endDate = 'End date must be after start date.';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  // ── Submit ────────────────────────────────────────────────────────────────

  const handleSubmit = async () => {
    if (!validate() || adminId === null) return;

    const toISO = (d: string) => `${d}T00:00:00`;

    // Convert blob → base64 to send as JSON field (same as aiGeneratedImage in AddProduct)
    let diagramBase64: string | null = null;
    if (diagramBlob) {
      const buffer = await diagramBlob.arrayBuffer();
      const bytes = new Uint8Array(buffer);
      diagramBase64 = btoa(
        bytes.reduce((acc, byte) => acc + String.fromCharCode(byte), ''),
      );
    }

    const payload: ProjectCreate = {
      name: form.name.trim(),
      type: form.type.trim(),
      description: form.description.trim(),
      status: form.status,
      startDate: toISO(form.startDate),
      endDate: toISO(form.endDate),
      assignedEmployeeIds: selectedIds,
      createdByAdminId: adminId,
      projectDiagram: diagramBase64,
    };

    setSubmitting(true);
    try {
      await apiService.addProject(payload);
      sileo.success({
        title: 'Project created',
        description: `"${payload.name}" has been added.`,
      });
      onCreated();
      onClose();
    } catch (e) {
      sileo.error({
        title: 'Failed to create project',
        description: String(e),
      });
    } finally {
      setSubmitting(false);
    }
  };

  if (!open) return null;

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
          <h2 className="text-xl font-bold text-slate-800">Add Project</h2>
          <button
            onClick={onClose}
            className="w-8 h-8 flex items-center justify-center rounded-full hover:bg-slate-100 transition-colors text-slate-500 hover:text-slate-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form */}
        <div className="px-6 py-5 space-y-4">
          <Field label="Project Name:" error={errors.name}>
            <IconInput
              icon={FolderOpen}
              placeholder="Project name"
              value={form.name}
              onChange={(v) => set('name', v)}
              disabled={submitting}
              hasError={!!errors.name}
            />
          </Field>

          <Field label="Project Type:" error={errors.type}>
            <IconInput
              icon={Tag}
              placeholder="e.g. Construction Engineering"
              value={form.type}
              onChange={(v) => set('type', v)}
              disabled={submitting}
              hasError={!!errors.type}
            />
          </Field>

          {/* Description with AI generate button */}
          <Field label="Description:">
            <div className="flex items-start gap-2.5 border border-slate-300 rounded-lg px-3 py-2.5 bg-white focus-within:ring-2 focus-within:ring-blue-500/30 focus-within:border-blue-400 transition-colors">
              <AlignLeft className="w-4 h-4 text-slate-400 shrink-0 mt-0.5" />
              <textarea
                placeholder="Brief description (optional)"
                value={form.description}
                onChange={(e) => set('description', e.target.value)}
                disabled={submitting || generating}
                rows={3}
                className="flex-1 text-sm bg-transparent outline-none text-slate-800 placeholder:text-slate-400 disabled:opacity-50 resize-none"
              />
            </div>
            <button
              onClick={handleGenerateDescription}
              disabled={submitting || generating || !canGenerateDescription}
              className="self-start flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-violet-50 hover:bg-violet-100 text-violet-700 text-xs font-medium transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
            >
              {generating ? <Spinner /> : <Sparkles className="w-3 h-3" />}
              {generating ? 'Generating…' : 'AI Generate'}
            </button>
            {!canGenerateDescription && (
              <p className="text-xs text-slate-400">
                Fill in name and type to enable AI description generation.
              </p>
            )}
          </Field>

          {/* Diagram with AI generate button + preview */}
          <Field label="Diagram:">
            <button
              onClick={handleGenerateDiagram}
              disabled={submitting || generatingDiagram || !canGenerateDiagram}
              className="self-start flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-50 hover:bg-emerald-100 text-emerald-700 text-xs font-medium transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
            >
              {generatingDiagram ? (
                <Spinner />
              ) : (
                <ImageIcon className="w-3 h-3" />
              )}
              {generatingDiagram ? 'Generating…' : 'AI Generate Diagram'}
            </button>

            {!canGenerateDiagram && (
              <p className="text-xs text-slate-400">
                Fill in name, type, and description to enable diagram
                generation.
              </p>
            )}

            {/* Preview — mirrors the image preview block in AddProduct */}
            {diagramPreviewUrl && (
              <div className="mt-2">
                <div className="flex items-center justify-between mb-1.5">
                  <span className="text-xs text-slate-500">
                    AI Generated Diagram Preview:
                  </span>
                  <button
                    type="button"
                    onClick={handleRemoveDiagram}
                    disabled={submitting}
                    className="text-xs text-red-500 hover:text-red-700 font-medium transition-colors disabled:opacity-40"
                  >
                    Remove
                  </button>
                </div>
                <div className="border border-slate-200 rounded-lg p-2 bg-slate-50">
                  <img
                    src={diagramPreviewUrl}
                    alt="AI Generated Project Diagram"
                    className="w-full rounded object-contain max-h-52"
                  />
                </div>
              </div>
            )}
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

          <Field label="Start Date:" error={errors.startDate}>
            <IconInput
              icon={Calendar}
              type="date"
              placeholder=""
              value={form.startDate}
              onChange={(v) => set('startDate', v)}
              disabled={submitting}
              hasError={!!errors.startDate}
            />
          </Field>

          <Field label="End Date:" error={errors.endDate}>
            <IconInput
              icon={Calendar}
              type="date"
              placeholder=""
              value={form.endDate}
              onChange={(v) => set('endDate', v)}
              disabled={submitting}
              hasError={!!errors.endDate}
            />
          </Field>

          {/* Employee assignment */}
          <Field label="Assign To:" error={errors.assignees}>
            <div className="border border-slate-300 rounded-lg overflow-hidden focus-within:ring-2 focus-within:ring-blue-500/30 focus-within:border-blue-400">
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

              <div className="max-h-40 overflow-y-auto divide-y divide-slate-100">
                {filteredEmployees.length === 0 ? (
                  <p className="text-xs text-slate-400 px-3 py-3 text-center">
                    {employees.filter((e) => e.role === 'EMPLOYEE').length === 0
                      ? 'No employees found.'
                      : 'No matches for your search.'}
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
            {submitting && <Spinner className="w-4 h-4" />}
            {submitting ? 'Creating…' : 'Create Project'}
          </button>
        </div>
      </div>
    </div>
  );
}
