interface ProjectOverviewFooterProps {
  deadline: string;
  status: string;
}

const ProjectOverviewFooter = ({ deadline, status }: ProjectOverviewFooterProps) => {
  return (
    <footer className="mt-auto py-4 border-t border-base-200 bg-base-100">
      <div className="flex flex-wrap justify-center items-center gap-x-6 gap-y-1 text-sm text-base-content/80">
        <span>
          <span className="font-medium text-base-content">Deadline:</span> {deadline}
        </span>
        <span className="hidden sm:inline">|</span>
        <span>
          <span className="font-medium text-base-content">Status:</span> {status}
        </span>
      </div>
    </footer>
  );
};

export default ProjectOverviewFooter;
