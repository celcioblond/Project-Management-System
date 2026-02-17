import CardNote from "../components/CardNote";

interface CommentResponse {
  id: number;
  content: string;
  authorName: string;
  createdAt: string;
  updatedAt: string;
}

interface TaskResponse {
  id: number;
  title: string;
  description: string;
  priority: string;
  status: string;
  dueDate: string;
  projectName: string;
  assignedEmployeeName: string;
  assignedByAdminName: string;
  comments: CommentResponse[];
  createdAt: string;
}


interface ProjectResponse {
  title: string;
  description: string;
  status: string;
  startDate: string;
  endDate: string;
  assignedEmployeeName: string;
  createdByAdminName: string;
  tasks: TaskResponse[];
  comments: CommentResponse[];
  createdAt: string;
}

const EmployeeDashboard = () => {

  return (
    <div
      className="grid grid-cols-[repeat(auto-fit, minmax(280px, _1fr))] gap-4
     mt-16 xl:grid-cols-[repeat(auto-fit, minmax(350px, _1fr))]"
    >
      <h1>EmployeeDashboard</h1>
      <CardNote />
      <CardNote />
    </div>
  );
};

export default EmployeeDashboard;
