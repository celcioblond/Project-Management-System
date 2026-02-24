import type { CommentResponse } from "../../services/api";

interface ProjectCommentCardProps {
  comments?: CommentResponse[];
}

const ProjectCommentCard = ({comments} : ProjectCommentCardProps) => {

  if (!comments || comments.length === 0) {
    return (
      <p>No comments found</p>
    )
  }

  return (
    <div className="card bg-base border border-base-200 rounded-xl shadow-sm flex flex-col">
      <div className="card-body flex-1 flex flex-col min-h-0">
        <h2 className="card-title text-base font-semibold shrink-0">Project Comments</h2>
        <ul className="space-y-4 mt-2 flex-1 overflow-auto min-h-0">
          
          {comments.map((comment) => (
            <li key={comment.id} className="flex gap-3">
              <div className="min-w-0 flex-1">
                <p className="font-semibold text-sm text-base-content">
                  {comment.authorName}
                </p>
                <p className="text-sm text-base-content/80 mt-0.5">{comment.content}</p>
                <p className="text-xs text-base-content/50 mt-1">{comment.createdAt}</p>
              </div>
            </li>
          ))}
        </ul>
      </div>

    </div>
  )
}

export default ProjectCommentCard;