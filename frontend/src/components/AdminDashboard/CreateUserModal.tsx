// src/components/AdminDashboard/CreateUserModal.tsx
import { useState, useEffect } from 'react';
import {
  X,
  User,
  AtSign,
  Hash,
  Mail,
  Lock,
  Briefcase,
  Building2,
  ShieldCheck,
} from 'lucide-react';
import { sileo } from 'sileo';
import { apiService, type UserCreate } from '../../services/api';

// ─── Constants ────────────────────────────────────────────────────────────────

const POSITIONS = [
  'Software Engineer',
  'Senior Software Engineer',
  'Frontend Developer',
  'Backend Developer',
  'Full Stack Developer',
  'DevOps Engineer',
  'Product Manager',
  'Project Manager',
  'UI/UX Designer',
  'Data Analyst',
  'QA Engineer',
  'Team Lead',
  'Other',
];

const DEPARTMENTS = [
  'Engineering',
  'Product',
  'Design',
  'Marketing',
  'Sales',
  'Human Resources',
  'Finance',
  'Operations',
  'Customer Support',
  'Management',
];

const ROLES = ['EMPLOYEE', 'ADMIN'];

// ─── Types ────────────────────────────────────────────────────────────────────

interface CreateUserModalProps {
  open: boolean;
  onClose: () => void;
  onCreated: () => void; // called after successful creation so parent can reload
}

interface FormState {
  name: string;
  username: string;
  age: string;
  email: string;
  password: string;
  position: string;
  department: string;
  role: string;
}

const EMPTY_FORM: FormState = {
  name: '',
  username: '',
  age: '',
  email: '',
  password: '',
  position: '',
  department: '',
  role: '',
};

// ─── Field wrapper ────────────────────────────────────────────────────────────

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

// ─── Input with icon ──────────────────────────────────────────────────────────

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

// ─── Select with icon ─────────────────────────────────────────────────────────

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
  options: string[];
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
          <option key={o} value={o}>
            {o}
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

// ─── Modal ────────────────────────────────────────────────────────────────────

export default function CreateUserModal({
  open,
  onClose,
  onCreated,
}: CreateUserModalProps) {
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [errors, setErrors] = useState<Partial<FormState>>({});
  const [submitting, setSubmitting] = useState(false);

  // Reset form whenever modal opens
  useEffect(() => {
    if (open) {
      setForm(EMPTY_FORM);
      setErrors({});
    }
  }, [open]);

  // Close on Escape key
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
    // Clear the error for this field as soon as user types
    setErrors((p) => ({ ...p, [key]: undefined }));
  };

  // ── Validation ────────────────────────────────────────────────────────────

  const validate = (): boolean => {
    const e: Partial<FormState> = {};

    if (!form.name.trim()) e.name = 'Full name is required.';
    if (!form.username.trim()) e.username = 'Username is required.';
    else if (!/^[a-zA-Z0-9_-]{3,20}$/.test(form.username))
      e.username = '3–20 chars, letters/numbers/underscore/hyphen only.';
    if (!form.email.trim()) e.email = 'Email is required.';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email))
      e.email = 'Enter a valid email address.';
    if (!form.password) e.password = 'Password is required.';
    else if (form.password.length < 6)
      e.password = 'Password must be at least 6 characters.';
    if (!form.position) e.position = 'Select a position.';
    if (!form.department) e.department = 'Select a department.';
    if (!form.role) e.role = 'Select a role.';

    const ageNum = Number(form.age);
    if (form.age && (isNaN(ageNum) || ageNum < 16 || ageNum > 100))
      e.age = 'Enter a valid age (16–100).';

    setErrors(e);
    return Object.keys(e).length === 0;
  };

  // ── Submit ────────────────────────────────────────────────────────────────

  const handleSubmit = async () => {
    if (!validate()) return;

    const payload: UserCreate = {
      name: form.name.trim(),
      username: form.username.trim(),
      age: form.age ? Number(form.age) : 0,
      email: form.email.trim(),
      password: form.password,
      position: form.position,
      department: form.department,
      role: form.role,
    };

    setSubmitting(true);
    try {
      await apiService.createUser(payload);
      sileo.success({
        title: 'User created',
        description: `${payload.name} has been added.`,
      });
      onCreated(); // triggers parent to reload the users list
      onClose();
    } catch (e) {
      sileo.error({ title: 'Failed to create user', description: String(e) });
    } finally {
      setSubmitting(false);
    }
  };

  if (!open) return null;

  return (
    /* Backdrop */
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      {/* Panel */}
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto animate-in fade-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-5 border-b border-slate-100">
          <h2 className="text-xl font-bold text-slate-800">Create User</h2>
          <button
            onClick={onClose}
            className="w-8 h-8 flex items-center justify-center rounded-full hover:bg-slate-100 transition-colors text-slate-500 hover:text-slate-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form */}
        <div className="px-6 py-5 space-y-4">
          <Field label="Full Name:" error={errors.name}>
            <IconInput
              icon={User}
              placeholder="Full Name"
              value={form.name}
              onChange={(v) => set('name', v)}
              disabled={submitting}
              hasError={!!errors.name}
            />
          </Field>

          <Field label="Username:" error={errors.username}>
            <IconInput
              icon={AtSign}
              placeholder="Username"
              value={form.username}
              onChange={(v) => set('username', v)}
              disabled={submitting}
              hasError={!!errors.username}
            />
          </Field>

          <Field label="Age:" error={errors.age}>
            <div
              className={`flex items-center gap-2.5 border rounded-lg px-3 py-2.5 bg-white w-24 transition-colors focus-within:ring-2 focus-within:ring-blue-500/30 focus-within:border-blue-400 ${errors.age ? 'border-red-400' : 'border-slate-300'}`}
            >
              <Hash className="w-4 h-4 text-slate-400 shrink-0" />
              <input
                type="number"
                min={16}
                max={100}
                placeholder="—"
                value={form.age}
                onChange={(e) => set('age', e.target.value)}
                disabled={submitting}
                className="w-full text-sm bg-transparent outline-none text-slate-800 placeholder:text-slate-400 disabled:opacity-50"
              />
            </div>
          </Field>

          <Field label="Email:" error={errors.email}>
            <IconInput
              icon={Mail}
              type="email"
              placeholder="Email"
              value={form.email}
              onChange={(v) => set('email', v)}
              disabled={submitting}
              hasError={!!errors.email}
            />
          </Field>

          <Field label="Password:" error={errors.password}>
            <IconInput
              icon={Lock}
              type="password"
              placeholder="••••••••••"
              value={form.password}
              onChange={(v) => set('password', v)}
              disabled={submitting}
              hasError={!!errors.password}
            />
          </Field>

          <Field label="Position:" error={errors.position}>
            <IconSelect
              icon={Briefcase}
              placeholder="Select position"
              value={form.position}
              onChange={(v) => set('position', v)}
              options={POSITIONS}
              disabled={submitting}
              hasError={!!errors.position}
            />
          </Field>

          <Field label="Department:" error={errors.department}>
            <IconSelect
              icon={Building2}
              placeholder="Select department"
              value={form.department}
              onChange={(v) => set('department', v)}
              options={DEPARTMENTS}
              disabled={submitting}
              hasError={!!errors.department}
            />
          </Field>

          <Field label="Role:" error={errors.role}>
            <IconSelect
              icon={ShieldCheck}
              placeholder="Select role"
              value={form.role}
              onChange={(v) => set('role', v)}
              options={ROLES}
              disabled={submitting}
              hasError={!!errors.role}
            />
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
            {submitting ? 'Creating…' : 'Create'}
          </button>
        </div>
      </div>
    </div>
  );
}
