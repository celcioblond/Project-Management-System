import { format } from "date-fns";
import type { TaskResponse } from "../../services/api";

interface MyTasksCardProps {
  tasks?: TaskResponse[];
}

const MyTasksCard = ({tasks} : MyTasksCardProps) => {

  if (!tasks || tasks.length === 0) {
    return (
      <div className="bg-amber-950 text-white p-3">
        <p>No tasks available</p>
      </div>
    );
  }

  return (
    <div>
      <ul className="space-y-3 mt-2">
        {tasks.map((task) => (
          <li key={task.id} className="flex items-center gap-3 py-1 group">
            <h1>{task.title}</h1>
            <p className="flex-1 text-sm min-w-0">{task.description}</p>
            <p className="flex-1 text-sm min-w-0">{task.priority}</p>
            <p className="flex-1 text-sm min-w-0">{task.status}</p>
            <p className="flex-1 text-sm min-w-0">{format(new Date(task.dueDate), "MM/dd/yyyy")}</p>
            <p className="flex-1 text-sm min-w-0">{format(new Date(task.createdAt), "MM/dd/yyyy")}</p>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default MyTasksCard;