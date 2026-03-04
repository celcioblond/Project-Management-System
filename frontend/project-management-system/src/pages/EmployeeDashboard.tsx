// src/pages/EmployeeDashboard.tsx
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import CardNote from '../components/EmployeeDashboard/CardNote';
import AiChatBot from '../components/EmployeeDashboard/AiChatBot';
import { useAuth } from '../context/AuthContext';
import { apiService } from '../services/api';
import type { ProjectResponse } from '../services/api';

const EmployeeDashboard = () => {
  const { user } = useAuth();

  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchProjects = async () => {
      try {
        setLoading(true);
        const data = await apiService.getMyProjects();
        setProjects(data);
        console.log('Fetched projects:', data);
      } catch (err) {
        console.error('Error fetching projects:', err);
        setError(
          err instanceof Error ? err.message : 'Failed to load projects',
        );
      } finally {
        setLoading(false);
      }
    };

    fetchProjects();
  }, []);

  return (
    <div className="min-h-screen bg-slate-50 pb-20">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 pt-8 space-y-6">
        {/* Header */}
        <div className="flex items-start justify-between gap-4 flex-wrap">
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">
              Welcome back
            </p>
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
              {user?.username || 'Employee'}
            </h1>
            <p className="text-sm text-slate-500 mt-1">
              Here are the projects you are currently involved in.
            </p>
          </div>
        </div>

        {/* Loading / error states */}
        {loading ? (
          <div className="flex flex-col items-center justify-center py-16 gap-3">
            <span className="loading loading-spinner loading-lg text-blue-500" />
            <p className="text-sm text-slate-400 font-medium">
              Loading your projects…
            </p>
          </div>
        ) : error ? (
          <div className="flex flex-col items-center justify-center py-16 gap-3">
            <p className="text-sm font-medium text-red-500">
              Failed to load your projects.
            </p>
            <p className="text-xs text-slate-400 max-w-sm text-center">
              {error}
            </p>
          </div>
        ) : (
          <>
            {/* Overview stats */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              {(() => {
                const total = projects.length;
                const completed = projects.filter(
                  (p) => p.status === 'DONE' || p.status === 'COMPLETED',
                ).length;
                const active = total - completed;
                const nextDeadline = projects
                  .map((p) => p.endDate)
                  .filter(Boolean)
                  .map((d) => new Date(d))
                  .filter((d) => !Number.isNaN(d.getTime()))
                  .sort((a, b) => a.getTime() - b.getTime())[0];
                const fmtDate = (d?: Date) =>
                  d
                    ? d.toLocaleDateString('en-US', {
                        year: 'numeric',
                        month: 'short',
                        day: 'numeric',
                      })
                    : '—';

                return (
                  <>
                    <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3">
                      <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wide">
                        Total Projects
                      </p>
                      <p className="mt-1 text-2xl font-bold text-slate-900">
                        {total}
                      </p>
                    </div>
                    <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3">
                      <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wide">
                        Active
                      </p>
                      <p className="mt-1 text-2xl font-bold text-blue-600">
                        {active}
                      </p>
                    </div>
                    <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3">
                      <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wide">
                        Next Deadline
                      </p>
                      <p className="mt-1 text-sm font-semibold text-slate-800">
                        {fmtDate(nextDeadline)}
                      </p>
                    </div>
                  </>
                );
              })()}
            </div>

            {/* Projects grid */}
            {projects.length === 0 ? (
              <div className="mt-8 flex flex-col items-center justify-center py-16 gap-3 rounded-2xl border border-dashed border-slate-300 bg-slate-50">
                <p className="text-sm font-medium text-slate-500">
                  No projects assigned yet
                </p>
                <p className="text-xs text-slate-400 max-w-sm text-center">
                  Once you are added to a project, it will appear here with key
                  details and progress.
                </p>
              </div>
            ) : (
              <div className="mt-6 grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
                {projects.map((project) => {
                  const totalTasks = project.tasks?.length ?? 0;
                  const completedTasks =
                    project.tasks?.filter(
                      (t) => t.status === 'DONE' || t.status === 'COMPLETED',
                    ).length ?? 0;
                  const memberCount = project.assignedEmployeeNames?.length ?? 0;

                  return (
                    <Link
                      key={project.id}
                      to={`/employee/dashboard/projects/${project.id}`}
                      className="block h-full"
                    >
                      <CardNote
                        title={project.title}
                        description={project.description}
                        status={project.status}
                        started={project.startDate}
                        due={project.endDate}
                        memberCount={memberCount}
                        taskCount={totalTasks}
                        completedTaskCount={completedTasks}
                        admin={project.createdByAdminName}
                      />
                    </Link>
                  );
                })}
              </div>
            )}
          </>
        )}
      </div>

      {/* AI Project Assistant — floats bottom-right over the dashboard */}
      <AiChatBot />
    </div>
  );
};

export default EmployeeDashboard;
