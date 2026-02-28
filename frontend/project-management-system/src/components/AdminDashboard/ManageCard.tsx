// src/components/Admin/ManageCard.tsx
import { Pencil, Trash2, Plus } from "lucide-react";
import { cn } from "@/lib/utils";

// ─── Types ────────────────────────────────────────────────────────────────────

export interface ColumnDef<T> {
  /** Column header label */
  header: string;
  /** Key of the row object OR a custom render function */
  accessor: keyof T | ((row: T) => React.ReactNode);
  className?: string;
}

export interface ManageCardProps<T> {
  /** Card heading, e.g. "Manage Users" */
  title: string;
  /** Button label, e.g. "+ Create User" */
  addLabel: string;
  /** Called when the add/create button is clicked */
  onAdd: () => void;
  /** Column definitions */
  columns: ColumnDef<T>[];
  /** Row data */
  rows: T[];
  /** Called with the row when Edit is clicked */
  onEdit: (row: T) => void;
  /** Called with the row when Delete is clicked */
  onDelete: (row: T) => void;
  /** Optional: show a loading skeleton */
  loading?: boolean;
  /** Optional: message when rows is empty */
  emptyMessage?: string;
}

// ─── Badge helper (for status/role/priority values) ──────────────────────────

const BADGE_MAP: Record<string, string> = {
  // Status
  "In Progress":  "bg-blue-100 text-blue-700 border border-blue-200",
  "Completed":    "bg-emerald-100 text-emerald-700 border border-emerald-200",
  "Not Started":  "bg-slate-100 text-slate-600 border border-slate-200",
  "On Hold":      "bg-yellow-100 text-yellow-700 border border-yellow-200",
  // Priority
  "CRITICAL":     "bg-red-100 text-red-700 border border-red-200",
  "HIGH":         "bg-orange-100 text-orange-700 border border-orange-200",
  "High":         "bg-orange-100 text-orange-700 border border-orange-200",
  "MEDIUM":       "bg-yellow-100 text-yellow-700 border border-yellow-200",
  "Medium":       "bg-yellow-100 text-yellow-700 border border-yellow-200",
  "LOW":          "bg-green-100 text-green-700 border border-green-200",
  "Low":          "bg-green-100 text-green-700 border border-green-200",
  // Role
  "ADMIN":        "bg-violet-100 text-violet-700 border border-violet-200",
  "Admin":        "bg-violet-100 text-violet-700 border border-violet-200",
  "EMPLOYEE":     "bg-slate-100 text-slate-600 border border-slate-200",
  "User":         "bg-slate-100 text-slate-600 border border-slate-200",
  // Task status
  "TODO":         "bg-slate-100 text-slate-600 border border-slate-200",
  "IN_PROGRESS":  "bg-blue-100 text-blue-700 border border-blue-200",
  "IN_REVIEW":    "bg-purple-100 text-purple-700 border border-purple-200",
  "DONE":         "bg-emerald-100 text-emerald-700 border border-emerald-200",
};

export function Badge({ value }: { value: string }) {
  const cls = BADGE_MAP[value];
  if (!cls) return <span>{value}</span>;
  return (
    <span className={cn("text-xs font-semibold px-2.5 py-1 rounded-full", cls)}>
      {value}
    </span>
  );
}

// ─── Skeleton row ─────────────────────────────────────────────────────────────

function SkeletonRow({ cols }: { cols: number }) {
  return (
    <tr>
      {Array.from({ length: cols + 1 }).map((_, i) => (
        <td key={i} className="px-4 py-3">
          <div className="h-4 bg-base-200 rounded animate-pulse w-full max-w-[120px]" />
        </td>
      ))}
    </tr>
  );
}

// ─── ManageCard ───────────────────────────────────────────────────────────────

function ManageCard<T>({
  title,
  addLabel,
  onAdd,
  columns,
  rows,
  onEdit,
  onDelete,
  loading = false,
  emptyMessage = "No records found.",
}: ManageCardProps<T>) {
  const getCellValue = (row: T, col: ColumnDef<T>): React.ReactNode => {
    if (typeof col.accessor === "function") {
      return col.accessor(row);
    }
    const val = row[col.accessor];
    if (typeof val === "string" && BADGE_MAP[val] !== undefined) {
      return <Badge value={val} />;
    }
    return val as React.ReactNode;
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
      {/* ── Header ── */}
      <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
        <h2 className="text-base font-bold text-slate-800 tracking-tight">
          {title}
        </h2>
        <button
          onClick={onAdd}
          className="flex items-center gap-1.5 bg-blue-600 hover:bg-blue-700 active:scale-95 text-white text-xs font-semibold px-3.5 py-2 rounded-lg transition-all duration-150 shadow-sm"
        >
          <Plus className="w-3.5 h-3.5" />
          {addLabel}
        </button>
      </div>

      {/* ── Table ── */}
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-slate-100 bg-slate-50/60">
              {columns.map((col, i) => (
                <th
                  key={i}
                  className={cn(
                    "px-4 py-2.5 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider whitespace-nowrap",
                    col.className
                  )}
                >
                  {col.header}
                </th>
              ))}
              <th className="px-4 py-2.5 text-right text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>

          <tbody className="divide-y divide-slate-100">
            {loading ? (
              Array.from({ length: 4 }).map((_, i) => (
                <SkeletonRow key={i} cols={columns.length} />
              ))
            ) : rows.length === 0 ? (
              <tr>
                <td
                  colSpan={columns.length + 1}
                  className="px-4 py-10 text-center text-sm text-slate-400"
                >
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              rows.map((row, rowIdx) => (
                <tr
                  key={rowIdx}
                  className="hover:bg-slate-50/70 transition-colors duration-100"
                >
                  {columns.map((col, colIdx) => (
                    <td
                      key={colIdx}
                      className={cn(
                        "px-4 py-3 text-slate-700 whitespace-nowrap",
                        colIdx === 0 && "font-medium text-slate-900",
                        col.className
                      )}
                    >
                      {getCellValue(row, col)}
                    </td>
                  ))}

                  {/* Actions */}
                  <td className="px-4 py-3">
                    <div className="flex items-center justify-end gap-1.5">
                      <button
                        onClick={() => onEdit(row)}
                        className="flex items-center gap-1 bg-blue-600 hover:bg-blue-700 active:scale-95 text-white text-xs font-semibold px-2.5 py-1.5 rounded-md transition-all duration-150"
                        title="Edit"
                      >
                        <Pencil className="w-3 h-3" />
                        Edit
                      </button>
                      <button
                        onClick={() => onDelete(row)}
                        className="flex items-center gap-1 bg-red-500 hover:bg-red-600 active:scale-95 text-white text-xs font-semibold px-2.5 py-1.5 rounded-md transition-all duration-150"
                        title="Delete"
                      >
                        <Trash2 className="w-3 h-3" />
                        Del
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default ManageCard;