import { useState, useEffect } from "react";
import { apiService, type ProjectResponse, type UserResponse } from "../services/api";
import { useParams } from "react-router-dom";
import ProjectOverviewHeader from "../components/ProjectOverview/ProjectOverviewHeader";
import ProjectTeamCard from "../components/ProjectOverview/ProjectTeamCard";
import MyTasksCard from "../components/ProjectOverview/MyTasksCard";
import ProjectCommentsCard from "../components/ProjectOverview/ProjectCommentsCard";

const ProjectOverviewPage = () => {

  const { projectId } = useParams<{ projectId: string }>();
  const id = projectId ? Number(projectId) : NaN;

  const [project, setProject] = useState<ProjectResponse | null>(null);
  const [colleagues, setColleagues] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const [projectData, colleagueData] = await Promise.all([
          apiService.getProjectById(id),
          apiService.getMyColleagues(id),
        ]);
        setProject(projectData);
        setColleagues(colleagueData);
      } catch(error) {
        setError(error instanceof Error ? error.message : "Failed to load data");
        console.log("Error fetching data: ", error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [id]);

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <span className="loading loading-spinner loading-lg"></span>
      </div>
    )
  }

  if (error){
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <p className="text-lg text-error">Error: {error}</p>
      </div>
    )
  }

  return (
    <div>
      <ProjectOverviewHeader />
      
      <main className="flex-1 container mx-auto px-4 py-6 max-w-6xl">
        <div className="bg-amber-400 w-full p-4 border-4 text-center">
          <h2 className="text-4xl p-5 sm:text-3xl font-bold text-base-content">
            {project?.title}
          </h2>
          <p className="text-base-content/70 mt-2 max-w-2xl mx-auto">
            {project?.description}
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <ProjectTeamCard members={colleagues} />
          <MyTasksCard tasks={project?.tasks} />
          <ProjectCommentsCard comments={project?.comments} />
        </div>
      </main>
    </div>
  )
}

export default ProjectOverviewPage;