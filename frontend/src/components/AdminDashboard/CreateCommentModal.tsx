// src/components/AdminDashboard/CreateCommentModal.tsx
import { Folder, MessageSquare, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import { sileo } from 'sileo';
import {
  apiService,
  type ProjectCommentCreate,
  type ProjectResponse,
} from '../../services/api';

// ─── Constants ────────────────────────────────────────────────────────────────

const MAX_CONTENT_LENGTH = 2000;

// ─── Types ────────────────────────────────────────────────────────────────────

interface CreateCommentModalProps {
  open: boolean;
  projects: ProjectResponse[];
  onClose: () => void;
  onCreated: () => void;
}

interface FormState {
  projectId: string;
  content: string;
}

const EMPTY_FORM: FormState = { projectId: '', content: '' };

// Shared sub-components

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

// Modal

export default function CreateCommentModal({
  open,
  projects,
  onClose,
  onCreated,
}: CreateCommentModalProps) {
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [errors, setErrors] = useState<Partial<FormState>>({});
  const [submitting, setSubmitting] = useState(false);

  // Reset on open
  useEffect(() => {
    if (open) {
      setForm(EMPTY_FORM);
      setErrors({});
    }
  }, [open]);

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

  // Validation

  const validate = (): boolean => {
    const e: Partial<FormState> = {};

    if (!form.projectId) e.projectId = 'Select a project.';
    if (!form.content.trim()) e.content = 'Comment cannot be empty.';
    else if (form.content.length > MAX_CONTENT_LENGTH)
      e.content = `Comment must be at most ${MAX_CONTENT_LENGTH} characters.`;

    setErrors(e);
    return Object.keys(e).length === 0;
  };

  // Submit

  const handleSubmit = async () => {
    if (!validate()) return;

    const payload: ProjectCommentCreate = {
      content: form.content.trim(),
      projectId: Number(form.projectId),
    };

    setSubmitting(true);
    try {
      await apiService.createProjectComment(payload);
      sileo.success({ title: 'Comment added' });
      onCreated();
      onClose();
    } catch (e) {
      sileo.error({ title: 'Failed to add comment', description: String(e) });
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
          <h2 className="text-xl font-bold text-slate-800">Add Comment</h2>
          <button
            onClick={onClose}
            className="w-8 h-8 flex items-center justify-center rounded-full hover:bg-slate-100 transition-colors text-slate-500 hover:text-slate-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form */}
        <div className="px-6 py-5 space-y-4">
          <Field label="Project:" error={errors.projectId}>
            <div
              className={`flex items-center gap-2.5 border rounded-lg px-3 py-2.5 bg-white transition-colors focus-within:ring-2 focus-within:ring-blue-500/30 focus-within:border-blue-400 ${errors.projectId ? 'border-red-400' : 'border-slate-300'}`}
            >
              <Folder className="w-4 h-4 text-slate-400 shrink-0" />
              <select
                value={form.projectId}
                onChange={(e) => set('projectId', e.target.value)}
                disabled={submitting}
                className="flex-1 text-sm bg-transparent outline-none text-slate-800 disabled:opacity-50 cursor-pointer"
              >
                <option value="">Select project</option>
                {projects.map((p) => (
                  <option key={p.id} value={String(p.id)}>
                    {p.title}
                  </option>
                ))}
              </select>
            </div>
          </Field>

          <Field label="Comment:" error={errors.content}>
            <div
              className={`flex items-start gap-2.5 border rounded-lg px-3 py-2.5 bg-white transition-colors focus-within:ring-2 focus-within:ring-blue-500/30 focus-within:border-blue-400 ${errors.content ? 'border-red-400' : 'border-slate-300'}`}
            >
              <MessageSquare className="w-4 h-4 text-slate-400 shrink-0 mt-0.5" />
              <textarea
                placeholder="Write your comment"
                value={form.content}
                onChange={(e) => set('content', e.target.value)}
                disabled={submitting}
                rows={5}
                maxLength={MAX_CONTENT_LENGTH}
                className="flex-1 text-sm bg-transparent outline-none text-slate-800 placeholder:text-slate-400 disabled:opacity-50 resize-none"
              />
            </div>
            <p className="text-xs text-slate-400 text-right">
              {form.content.length}/{MAX_CONTENT_LENGTH}
            </p>
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
            disabled={submitting}
            className="px-6 py-2.5 rounded-lg bg-blue-600 hover:bg-blue-700 active:scale-95 text-white text-sm font-semibold transition-all duration-150 disabled:opacity-60 flex items-center gap-2"
          >
            {submitting ? 'Adding…' : 'Add Comment'}
          </button>
        </div>
      </div>
    </div>
  );
}
