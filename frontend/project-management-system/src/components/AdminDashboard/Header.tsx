// src/components/AdminDashboard/Header.tsx
import { useState, useEffect, useRef, useCallback } from 'react';
import {
  Home,
  Users,
  FolderOpen,
  CheckSquare,
  MessageSquare,
} from 'lucide-react';
import { sileo, Toaster } from 'sileo';
import ManageCard, { type ColumnDef } from './ManageCard';
import CreateUserModal from './CreateUserModal';
import EditUserModal from './EditUserModal';
import DeleteUserModal from './DeleteUserModal';
import CreateProjectModal from './CreateProjectModal';
import EditProjectModal from './EditProjectModal';
import DeleteProjectModal from './DeleteProjectModal';
import CreateTaskModal from './CreateTaskModal';
import EditTaskModal from './EditTaskModal';
import DeleteTaskModal from './DeleteTaskModal';
import {
  apiService,
  type UserResponse,
  type ProjectResponse,
  type TaskResponse,
  type CommentResponse,
} from '../../services/api';
import { useAuth } from '../../context/AuthContext';

// ─── Column definitions ───────────────────────────────────────────────────────

const userColumns: ColumnDef<UserResponse>[] = [
  { header: 'Name', accessor: 'name' },
  { header: 'Email', accessor: 'email' },
  { header: 'Position', accessor: (r) => r.position || '—' },
  { header: 'Department', accessor: (r) => r.department || '—' },
  { header: 'Role', accessor: 'role' },
];

const projectColumns: ColumnDef<ProjectResponse>[] = [
  { header: 'Project', accessor: 'title' },
  {
    header: 'Deadline',
    accessor: (r) =>
      r.endDate
        ? new Date(r.endDate).toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
          })
        : '—',
  },
  { header: 'Status', accessor: 'status' },
  {
    header: 'Members',
    accessor: (r) => `${r.assignedEmployeeNames?.length ?? 0} assigned`,
  },
];

const taskColumns: ColumnDef<TaskResponse>[] = [
  { header: 'Task', accessor: 'title' },
  { header: 'Project', accessor: (r) => r.projectName || '—' },
  {
    header: 'Assigned To',
    accessor: (r) => r.assignedEmployeeNames?.join(', ') || '—',
  },
  {
    header: 'Due Date',
    accessor: (r) =>
      r.dueDate
        ? new Date(r.dueDate).toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
          })
        : '—',
  },
  { header: 'Priority', accessor: 'priority' },
  { header: 'Status', accessor: 'status' },
];

const commentColumns: ColumnDef<CommentResponse>[] = [
  {
    header: 'Comment',
    accessor: (r) => (
      <span className="line-clamp-2 max-w-xs block">{r.content}</span>
    ),
  },
  { header: 'Author', accessor: 'authorName' },
  {
    header: 'Date',
    accessor: (r) =>
      r.createdAt
        ? new Date(r.createdAt).toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
          })
        : '—',
  },
];

// ─── Sidebar nav items ────────────────────────────────────────────────────────

const NAV_ITEMS = [
  { label: 'Overview', tip: 'Overview', icon: Home, sectionId: 'overview' },
  { label: 'Users', tip: 'Users', icon: Users, sectionId: 'users' },
  {
    label: 'Projects',
    tip: 'Projects',
    icon: FolderOpen,
    sectionId: 'projects',
  },
  { label: 'Tasks', tip: 'Tasks', icon: CheckSquare, sectionId: 'tasks' },
  {
    label: 'Comments',
    tip: 'Comments',
    icon: MessageSquare,
    sectionId: 'comments',
  },
];

// ─── Component ────────────────────────────────────────────────────────────────

const Header = () => {
  const { user } = useAuth();

  // ── Modal state ───────────────────────────────────────────────────────────
  const [createUserOpen, setCreateUserOpen] = useState(false);
  const [editUserTarget, setEditUserTarget] = useState<UserResponse | null>(
    null,
  );
  const [deleteUserTarget, setDeleteUserTarget] = useState<UserResponse | null>(
    null,
  );
  const [createProjectOpen, setCreateProjectOpen] = useState(false);
  const [editProjectTarget, setEditProjectTarget] =
    useState<ProjectResponse | null>(null);
  const [deleteProjectTarget, setDeleteProjectTarget] =
    useState<ProjectResponse | null>(null);
  const [createTaskOpen, setCreateTaskOpen] = useState(false);
  const [editTaskTarget, setEditTaskTarget] = useState<TaskResponse | null>(
    null,
  );
  const [deleteTaskTarget, setDeleteTaskTarget] = useState<TaskResponse | null>(
    null,
  );

  // ── Data ─────────────────────────────────────────────────────────────────
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [tasks, setTasks] = useState<TaskResponse[]>([]);
  const [comments, setComments] = useState<CommentResponse[]>([]);

  // ── Loading flags ─────────────────────────────────────────────────────────
  const [loadingUsers, setLoadingUsers] = useState(true);
  const [loadingProjects, setLoadingProjects] = useState(true);
  const [loadingTasks, setLoadingTasks] = useState(true);
  const [loadingComments, setLoadingComments] = useState(true);

  // ── Current admin id (needed for create/update calls) ─────────────────────
  const [adminId, setAdminId] = useState<number | null>(null);

  // ── Scroll-to-section refs ────────────────────────────────────────────────
  const sectionRefs = useRef<Record<string, HTMLDivElement | null>>({});
  const scrollTo = (id: string) =>
    sectionRefs.current[id]?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    });

  // ─── Loaders ──────────────────────────────────────────────────────────────

  const loadUsers = useCallback(async () => {
    setLoadingUsers(true);
    try {
      const data = await apiService.getAllUsers();
      setUsers(data);
      if (user?.username) {
        const me = data.find((u) => u.username === user.username);
        if (me) setAdminId(me.id);
      }
    } catch (e) {
      sileo.error({ title: 'Failed to load users', description: String(e) });
    } finally {
      setLoadingUsers(false);
    }
  }, [user]);

  const loadProjects = useCallback(async () => {
    setLoadingProjects(true);
    try {
      setProjects(await apiService.getAllProjects());
    } catch (e) {
      sileo.error({ title: 'Failed to load projects', description: String(e) });
    } finally {
      setLoadingProjects(false);
    }
  }, []);

  const loadTasks = useCallback(async () => {
    setLoadingTasks(true);
    try {
      setTasks(await apiService.getAllTasks());
    } catch (e) {
      sileo.error({ title: 'Failed to load tasks', description: String(e) });
    } finally {
      setLoadingTasks(false);
    }
  }, []);

  const loadComments = useCallback(async (projectList: ProjectResponse[]) => {
    setLoadingComments(true);
    try {
      const nested = await Promise.all(
        projectList.map((p) => apiService.getAllProjectComments(p.id)),
      );
      setComments(nested.flat());
    } catch (e) {
      sileo.error({ title: 'Failed to load comments', description: String(e) });
    } finally {
      setLoadingComments(false);
    }
  }, []);

  // Initial load
  useEffect(() => {
    loadUsers();
    loadProjects();
    loadTasks();
  }, [loadUsers, loadProjects, loadTasks]);

  // Load comments once projects are available
  useEffect(() => {
    if (projects.length > 0) loadComments(projects);
  }, [projects, loadComments]);

  // ─── User CRUD ────────────────────────────────────────────────────────────

  const handleAddUser = () => setCreateUserOpen(true);

  const handleEditUser = (u: UserResponse) => setEditUserTarget(u);

  const handleDeleteUser = (u: UserResponse) => setDeleteUserTarget(u);

  // ─── Project CRUD ─────────────────────────────────────────────────────────

  const handleAddProject = () => setCreateProjectOpen(true);

  const handleEditProject = (p: ProjectResponse) => setEditProjectTarget(p);

  const handleDeleteProject = (p: ProjectResponse) => setDeleteProjectTarget(p);

  // ─── Task CRUD ────────────────────────────────────────────────────────────

  const handleAddTask = () => setCreateTaskOpen(true);

  const handleEditTask = (t: TaskResponse) => setEditTaskTarget(t);

  const handleDeleteTask = (t: TaskResponse) => setDeleteTaskTarget(t);

  // ─── Comment CRUD ─────────────────────────────────────────────────────────

  const handleAddComment = () => {
    // TODO: open CreateCommentModal
    console.log('open create comment modal — wire up your modal here');
  };

  const handleEditComment = (c: CommentResponse) => {
    // TODO: open EditCommentModal pre-populated with `c`
    console.log('edit comment', c);
  };

  const handleDeleteComment = async (c: CommentResponse) => {
    if (!window.confirm('Delete this comment? This cannot be undone.')) return;
    try {
      await sileo.promise(apiService.deleteProjectComment(c.id), {
        loading: { title: 'Deleting comment…' },
        success: { title: 'Comment deleted' },
        error: { title: 'Failed to delete comment' },
      });
      await loadComments(projects);
    } catch {
      // handled by sileo.promise
    }
  };

  // ─── Render ───────────────────────────────────────────────────────────────

  return (
    <>
      <Toaster position="bottom-right" />

      <div className="drawer lg:drawer-open">
        <input id="my-drawer-4" type="checkbox" className="drawer-toggle" />

        {/* ── Main content ─────────────────────────────────────────────── */}
        <div className="drawer-content flex flex-col min-h-screen">
          <div className="flex-1 p-6 bg-slate-100 overflow-y-auto">
            <div className="max-w-7xl mx-auto space-y-6">
              <div
                ref={(el) => {
                  sectionRefs.current['users'] = el;
                }}
              >
                <ManageCard
                  title="Manage Users"
                  addLabel="Create User"
                  columns={userColumns}
                  rows={users}
                  loading={loadingUsers}
                  onAdd={handleAddUser}
                  onEdit={handleEditUser}
                  onDelete={handleDeleteUser}
                  emptyMessage="No users found."
                />
              </div>

              <div
                ref={(el) => {
                  sectionRefs.current['projects'] = el;
                }}
              >
                <ManageCard
                  title="Manage Projects"
                  addLabel="Add Project"
                  columns={projectColumns}
                  rows={projects}
                  loading={loadingProjects}
                  onAdd={handleAddProject}
                  onEdit={handleEditProject}
                  onDelete={handleDeleteProject}
                  emptyMessage="No projects found."
                />
              </div>

              <div
                ref={(el) => {
                  sectionRefs.current['tasks'] = el;
                }}
              >
                <ManageCard
                  title="Manage Tasks"
                  addLabel="Add Task"
                  columns={taskColumns}
                  rows={tasks}
                  loading={loadingTasks}
                  onAdd={handleAddTask}
                  onEdit={handleEditTask}
                  onDelete={handleDeleteTask}
                  emptyMessage="No tasks found."
                />
              </div>

              <div
                ref={(el) => {
                  sectionRefs.current['comments'] = el;
                }}
              >
                <ManageCard
                  title="Project Comments"
                  addLabel="Add Comment"
                  columns={commentColumns}
                  rows={comments}
                  loading={loadingComments}
                  onAdd={handleAddComment}
                  onEdit={handleEditComment}
                  onDelete={handleDeleteComment}
                  emptyMessage="No comments found."
                />
              </div>
            </div>
          </div>
        </div>

        {/* ── Sidebar ──────────────────────────────────────────────────── */}
        <div className="drawer-side z-10">
          <label
            htmlFor="my-drawer-4"
            aria-label="close sidebar"
            className="drawer-overlay"
          />
          <div className="flex min-h-full flex-col bg-base-200 is-drawer-close:w-14 is-drawer-open:w-64 transition-all duration-200">
            <ul className="menu w-full grow py-3 gap-0.5">
              {NAV_ITEMS.map(({ label, tip, icon: Icon, sectionId }) => (
                <li key={sectionId}>
                  <button
                    onClick={() => scrollTo(sectionId)}
                    className="is-drawer-close:tooltip is-drawer-close:tooltip-right flex items-center gap-3 w-full rounded-lg"
                    data-tip={tip}
                  >
                    <Icon className="w-4 h-4 shrink-0" />
                    <span className="is-drawer-close:hidden text-sm font-medium">
                      {label}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>

      {/* ── Modals ───────────────────────────────────────────────────────── */}
      <CreateUserModal
        open={createUserOpen}
        onClose={() => setCreateUserOpen(false)}
        onCreated={loadUsers}
      />
      <EditUserModal
        open={editUserTarget !== null}
        user={editUserTarget}
        onClose={() => setEditUserTarget(null)}
        onUpdated={loadUsers}
      />
      <DeleteUserModal
        open={deleteUserTarget !== null}
        user={deleteUserTarget}
        onClose={() => setDeleteUserTarget(null)}
        onDeleted={loadUsers}
      />
      <CreateProjectModal
        open={createProjectOpen}
        adminId={adminId}
        employees={users}
        onClose={() => setCreateProjectOpen(false)}
        onCreated={loadProjects}
      />
      <EditProjectModal
        open={editProjectTarget !== null}
        project={editProjectTarget}
        adminId={adminId}
        employees={users}
        onClose={() => setEditProjectTarget(null)}
        onUpdated={loadProjects}
      />
      <DeleteProjectModal
        open={deleteProjectTarget !== null}
        project={deleteProjectTarget}
        onClose={() => setDeleteProjectTarget(null)}
        onDeleted={loadProjects}
      />
      <CreateTaskModal
        open={createTaskOpen}
        adminId={adminId}
        projects={projects}
        employees={users}
        onClose={() => setCreateTaskOpen(false)}
        onCreated={loadTasks}
      />
      <EditTaskModal
        open={editTaskTarget !== null}
        task={editTaskTarget}
        adminId={adminId}
        employees={users}
        onClose={() => setEditTaskTarget(null)}
        onUpdated={loadTasks}
      />
      <DeleteTaskModal
        open={deleteTaskTarget !== null}
        task={deleteTaskTarget}
        onClose={() => setDeleteTaskTarget(null)}
        onDeleted={loadTasks}
      />
    </>
  );
};

export default Header;
