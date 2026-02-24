import type { UserResponse } from "../../services/api";

interface ProjectTeamCardProps {
  members?: UserResponse[];
}

const ProjectTeamCard = ({ members }: ProjectTeamCardProps) => {

  if (!members || members.length === 0) {
    return (
      <p>
        No members found
      </p>
    )
  }

  return (
    <div className="card bg-base-100 border border-base-200 rounded-xl shadow-sm">
      <div className="card-body">
        <h2 className="card-title text-base font-semibold">Project Team</h2>
        <ul className="space-y-4 mt-2">
          {members.map((member) => (
            <li key={member.id} className="flex items-center gap-3">
              <div className="min-w-0">
                <h2 className="font-semibold text-red-700xl mt-1">{member.name}</h2>
                <p className="text-2xl mt-1">{member.email}</p>
                <p className="text-xs text-base-content/70">{member.position}</p>
                <p>{member.department}</p>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
};

export default ProjectTeamCard;
