// src/components/TaskOverview/TaskCommentSection.tsx
import { useState, useEffect, useCallback } from 'react';
import {
  MessageSquare,
  Send,
  Trash2,
  Pencil,
  X,
  AlertTriangle,
} from 'lucide-react';
import { apiService, type CommentResponse } from '../../services/api';
import { useAuth } from '../../context/AuthContext';

// ─── Helpers ──────────────────────────────────────────────────────────────────

const fmtDate = (iso: string | null | undefined) => {
  if (!iso) return '';
  try {
    return new Date(iso).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  } catch {
    return '';
  }
};

const fmtTime = (iso: string | null | undefined) => {
  if (!iso) return '';
  try {
    return new Date(iso).toLocaleTimeString('en-US', {
      hour: 'numeric',
      minute: '2-digit',
    });
  } catch {
    return '';
  }
};

// Safely parse any date-like value — handles null/undefined/NaN without throwing
const safeDate = (val: unknown): number => {
  if (!val) return 0;
  const d = new Date(val as string);
  return isNaN(d.getTime()) ? 0 : d.getTime();
};

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function normalise(raw: any): CommentResponse {
  return {
    id: raw.id ?? raw.commentId ?? Math.random(),
    content: raw.content ?? raw.text ?? '',
    authorName:
      raw.authorName ??
      raw.author ??
      raw.username ??
      raw.authorUsername ??
      raw.userName ??
      null,
    createdAt: raw.createdAt ?? raw.created_at ?? raw.createdDate ?? null,
    updatedAt: raw.updatedAt ?? raw.updated_at ?? raw.updatedDate ?? null,
    projectId: raw.projectId ?? raw.project_id ?? null,
    taskId: raw.taskId ?? raw.task_id ?? null,
  };
}

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

function Avatar({ name }: { name: string }) {
  const idx = (name?.charCodeAt(0) ?? 0) % AVATAR_COLORS.length;
  return (
    <div
      className={`w-8 h-8 ${AVATAR_COLORS[idx]} rounded-full flex items-center justify-center text-white text-xs font-bold shrink-0`}
    >
      {name?.[0]?.toUpperCase() ?? '?'}
    </div>
  );
}

// ─── Props ────────────────────────────────────────────────────────────────────

interface Props {
  taskId: number;
}

// ─── Component ────────────────────────────────────────────────────────────────

export default function TaskCommentSection({ taskId }: Props) {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  const [comments, setComments] = useState<CommentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [text, setText] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [authorId, setAuthorId] = useState<number | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<CommentResponse | null>(
    null,
  );
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Resolve current user's ID
  useEffect(() => {
    if (!user?.username) return;
    apiService
      .getAllUsers()
      .then((users) => {
        const me = users.find((u) => u.username === user.username);
        if (me) setAuthorId(me.id);
      })
      .catch(() => {});
  }, [user]);

  // Load & normalise comments
  const loadComments = useCallback(async () => {
    try {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const raw: any[] = await apiService.getAllTaskComments(taskId);

      const normalised = raw.map(normalise);

      const forThisTask = normalised.filter(
        (c) => c.taskId == null || c.taskId === taskId,
      );

      forThisTask.sort(
        (a, b) => safeDate(a.createdAt) - safeDate(b.createdAt),
      );

      setComments(forThisTask);
    } catch (e) {
      console.error('[TaskCommentSection] loadComments failed:', e);
    }
  }, [taskId]);

  useEffect(() => {
    setLoading(true);
    loadComments().finally(() => setLoading(false));
  }, [loadComments]);

  const handleSubmit = async () => {
    if (!text.trim()) return;

    let aid = authorId;
    if (!aid) {
      try {
        const users = await apiService.getAllUsers();
        const me = users.find((u) => u.username === user?.username);
        aid = me?.id ?? null;
        if (aid) setAuthorId(aid);
      } catch {
        /* ignore */
      }
    }
    if (!aid) {
      setError('Could not identify your account.');
      return;
    }

    const optimistic: CommentResponse = {
      id: Date.now(),
      content: text.trim(),
      authorName: user?.name ?? user?.username ?? 'You',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      taskId,
    };

    setComments((prev) => [...prev, optimistic]);
    setText('');
    setSubmitting(true);
    setError(null);

    try {
      await apiService.createTaskComment({
        content: optimistic.content,
        taskId,
        authorId: aid,
      });
      await loadComments();
    } catch (e) {
      console.error('[TaskCommentSection] createTaskComment failed:', e);
      setComments((prev) => prev.filter((c) => c.id !== optimistic.id));
      setText(optimistic.content);
      setError('Failed to post comment. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeletingId(deleteTarget.id);
    try {
      await apiService.deleteTaskComment(deleteTarget.id);
      setDeleteTarget(null);
      await loadComments();
    } catch {
      setError('Failed to delete comment.');
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <>
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
        {/* Header */}
        <div className="flex items-center gap-2 px-5 py-4 border-b border-slate-100">
          <MessageSquare className="w-4 h-4 text-blue-500" />
          <h2 className="font-bold text-sm text-slate-800">Task Comments</h2>
          <span className="ml-auto text-xs bg-blue-50 text-blue-600 border border-blue-200 px-2 py-0.5 rounded-full font-mono">
            {comments.length}
          </span>
        </div>

        <div className="p-4 space-y-4">
          {error && (
            <div className="flex items-center gap-2 text-red-600 text-xs bg-red-50 border border-red-200 rounded-lg px-3 py-2">
              <AlertTriangle className="w-3.5 h-3.5 shrink-0" />
              {error}
              <button
                onClick={() => setError(null)}
                className="ml-auto text-red-400 hover:text-red-600"
              >
                <X className="w-3 h-3" />
              </button>
            </div>
          )}

          {/* Feed */}
          {loading ? (
            <div className="flex items-center justify-center py-8 gap-2 text-slate-400">
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
              <span className="text-xs">Loading comments…</span>
            </div>
          ) : comments.length === 0 ? (
            <p className="text-center py-6 text-sm text-slate-400">
              No comments yet. Share an update with your team.
            </p>
          ) : (
            <div className="space-y-4 max-h-72 overflow-y-auto pr-1">
              {comments.map((c, i) => (
                <div key={`${c.id}-${i}`} className="flex gap-3 group">
                  <Avatar name={c.authorName ?? user?.username ?? '?'} />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-baseline gap-2 flex-wrap">
                      <span className="text-sm font-semibold text-slate-800">
                        {c.authorName ?? user?.username}
                      </span>
                      {c.createdAt && (
                        <>
                          <span className="text-xs text-slate-400">
                            {fmtDate(c.createdAt)}
                          </span>
                          <span className="text-xs text-slate-300">
                            {fmtTime(c.createdAt)}
                          </span>
                        </>
                      )}
                      {c.updatedAt &&
                        c.createdAt &&
                        c.updatedAt !== c.createdAt && (
                          <span className="text-xs text-slate-400 italic flex items-center gap-0.5">
                            <Pencil className="w-2.5 h-2.5" /> edited
                          </span>
                        )}
                    </div>
                    <p className="text-sm text-slate-600 mt-0.5 leading-relaxed whitespace-pre-wrap break-words">
                      {c.content}
                    </p>
                  </div>

                  {isAdmin && (
                    <button
                      onClick={() => setDeleteTarget(c)}
                      disabled={deletingId === c.id}
                      className="w-7 h-7 flex items-center justify-center rounded-lg text-slate-300 hover:text-red-500 hover:bg-red-50 transition-colors opacity-0 group-hover:opacity-100 shrink-0 self-start mt-0.5 disabled:opacity-50"
                      title="Delete comment"
                    >
                      {deletingId === c.id ? (
                        <svg
                          className="animate-spin w-3.5 h-3.5"
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
                      ) : (
                        <Trash2 className="w-3.5 h-3.5" />
                      )}
                    </button>
                  )}
                </div>
              ))}
            </div>
          )}

          {/* Input */}
          <div className="flex gap-2 pt-3 border-t border-slate-100">
            <Avatar name={user?.username ?? '?'} />
            <div className="flex-1 flex gap-2">
              <textarea
                className="flex-1 border border-slate-300 rounded-xl px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-400 transition-colors resize-none min-h-[40px] max-h-32"
                placeholder="Write a comment… (Enter to send, Shift+Enter for new line)"
                value={text}
                onChange={(e) => setText(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    handleSubmit();
                  }
                }}
                rows={1}
                disabled={submitting}
              />
              <button
                onClick={handleSubmit}
                disabled={!text.trim() || submitting}
                className="self-end w-9 h-9 rounded-xl bg-blue-600 hover:bg-blue-700 disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center text-white transition-colors shrink-0"
              >
                {submitting ? (
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
                ) : (
                  <Send className="w-4 h-4" />
                )}
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Delete modal */}
      {deleteTarget && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm"
          onClick={(e) => {
            if (e.target === e.currentTarget) setDeleteTarget(null);
          }}
        >
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm p-6 space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-red-50 flex items-center justify-center shrink-0">
                <AlertTriangle className="w-5 h-5 text-red-500" />
              </div>
              <div>
                <h3 className="font-bold text-slate-800">Delete Comment</h3>
                <p className="text-xs text-slate-400 mt-0.5">
                  This cannot be undone.
                </p>
              </div>
            </div>
            <div className="bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 space-y-1">
              <p className="text-xs font-semibold text-slate-500">
                {deleteTarget.authorName}
              </p>
              <p className="text-sm text-slate-700 line-clamp-3">
                {deleteTarget.content}
              </p>
            </div>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setDeleteTarget(null)}
                className="px-4 py-2 rounded-lg border border-slate-300 text-sm font-medium text-slate-700 hover:bg-slate-50 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleDelete}
                disabled={!!deletingId}
                className="px-4 py-2 rounded-lg bg-red-500 hover:bg-red-600 text-white text-sm font-semibold transition-colors disabled:opacity-60 flex items-center gap-2"
              >
                {deletingId && (
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
                Yes, Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

