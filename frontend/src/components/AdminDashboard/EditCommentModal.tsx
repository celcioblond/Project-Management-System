// src/components/AdminDashboard/EditCommentModal.tsx
import { MessageSquare, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import { sileo } from 'sileo';
import { apiService, type CommentResponse } from '../../services/api';

// ─── Constants ────────────────────────────────────────────────────────────────

const MAX_CONTENT_LENGTH = 2000;

// ─── Types ────────────────────────────────────────────────────────────────────

interface EditCommentModalProps {
  open: boolean;
  comment: CommentResponse | null;
  onClose: () => void;
  onUpdated: () => void;
}

// Modal

export default function EditCommentModal({
  open,
  comment,
  onClose,
  onUpdated,
}: EditCommentModalProps) {
  const [content, setContent] = useState('');
  const [error, setError] = useState<string | undefined>();
  const [submitting, setSubmitting] = useState(false);

  // Pre-populate on open
  useEffect(() => {
    if (open && comment) {
      setContent(comment.content);
      setError(undefined);
    }
  }, [open, comment]);

  // Close on Escape
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, onClose]);

  // Submit

  const handleSubmit = async () => {
    if (!comment) return;
    if (!content.trim()) {
      setError('Comment cannot be empty.');
      return;
    }

    setSubmitting(true);
    try {
      await apiService.updateProjectComment(comment.id, { content: content.trim() });
      sileo.success({ title: 'Comment updated' });
      onUpdated();
      onClose();
    } catch (e) {
      sileo.error({ title: 'Failed to update comment', description: String(e) });
    } finally {
      setSubmitting(false);
    }
  };

  if (!open || !comment) return null;

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
          <h2 className="text-xl font-bold text-slate-800">Edit Comment</h2>
          <button
            onClick={onClose}
            className="w-8 h-8 flex items-center justify-center rounded-full hover:bg-slate-100 transition-colors text-slate-500 hover:text-slate-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form */}
        <div className="px-6 py-5 space-y-1">
          <div
            className={`flex items-start gap-2.5 border rounded-lg px-3 py-2.5 bg-white transition-colors focus-within:ring-2 focus-within:ring-blue-500/30 focus-within:border-blue-400 ${error ? 'border-red-400' : 'border-slate-300'}`}
          >
            <MessageSquare className="w-4 h-4 text-slate-400 shrink-0 mt-0.5" />
            <textarea
              placeholder="Write your comment"
              value={content}
              onChange={(e) => {
                setContent(e.target.value);
                setError(undefined);
              }}
              disabled={submitting}
              rows={5}
              maxLength={MAX_CONTENT_LENGTH}
              className="flex-1 text-sm bg-transparent outline-none text-slate-800 placeholder:text-slate-400 disabled:opacity-50 resize-none"
            />
          </div>
          {error && <p className="text-xs text-red-500">{error}</p>}
          <p className="text-xs text-slate-400 text-right">
            {content.length}/{MAX_CONTENT_LENGTH}
          </p>
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
            {submitting ? 'Saving…' : 'Save Changes'}
          </button>
        </div>
      </div>
    </div>
  );
}
