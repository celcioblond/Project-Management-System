import { ArrowLeft, Bell } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

function getInitials(name: string): string {
  if (!name?.trim()) return '?';
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

const TABS = [
  { id: 'overview', label: 'Overview' },
  { id: 'files', label: 'Files' },
  { id: 'timeline', label: 'Timeline' },
  { id: 'notes', label: 'Notes' },
] as const;

const ProjectOverviewHeader = () => {
  const { projectId } = useParams();
  const { user } = useAuth();
  const dashboardPath = '/employee/dashboard';

  return (
    <header className="bg-gradient-to-r from-blue-600 to-blue-700 text-white shadow-md">
      <div className="flex items-center justify-between gap-4 px-4 py-3 sm:px-6">
        <div className="flex items-center gap-3 min-w-0">
          <Link
            to={dashboardPath}
            className="flex items-center justify-center w-9 h-9 rounded-lg hover:bg-white/15 transition-colors shrink-0"
            aria-label="Back to dashboard"
          >
            <ArrowLeft className="w-5 h-5" />
          </Link>
          <h1 className="text-lg font-semibold truncate">Project Overview</h1>
        </div>

        <nav className="hidden sm:flex items-center gap-1 rounded-lg bg-white/10 p-1">
          {TABS.map((tab) => (
            <Link
              key={tab.id}
              to={`${dashboardPath}/projects/${projectId}`}
              className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
                tab.id === 'overview'
                  ? 'bg-white/25 text-white'
                  : 'text-white/90 hover:bg-white/15 hover:text-white'
              }`}
            >
              {tab.label}
            </Link>
          ))}
        </nav>

        <div className="flex items-center gap-2 shrink-0">
          <button
            type="button"
            className="flex items-center justify-center w-9 h-9 rounded-full hover:bg-white/15 transition-colors"
            aria-label="Notifications"
          >
            <Bell className="w-5 h-5" />
          </button>
          <div
            className="w-9 h-9 rounded-full bg-white/20 flex items-center justify-center border-2 border-white/30 text-sm font-semibold"
            title={user?.username ?? ''}
          >
            {user?.username ? getInitials(user.username) : '?'}
          </div>
        </div>
      </div>
    </header>
  );
};

export default ProjectOverviewHeader;
