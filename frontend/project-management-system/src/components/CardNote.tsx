// src/components/CardNote.tsx
import { ChevronUp, SquarePen } from 'lucide-react';

interface CardNoteProps {
  title: string;
  started: string;
  due: string;
  admin: string;
  createdAt: string;
}

const CardNote = ({ title, started, due, admin, createdAt }: CardNoteProps) => {
  // Format dates
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  return (
    <div className="card card-border bg-base-300 w-full">
      <div className="card-body">
        <h2 className="card-title text-accent font-bold lg:text-2xl">
          {title}
        </h2>
        <p className="text-sm text-gray-600 mb-2">Created by: {admin}</p>

        <div className="space-y-2 text-sm mt-4">
          <div className="flex justify-between">
            <span className="font-semibold">Started:</span>
            <span>{formatDate(started)}</span>
          </div>
          <div className="flex justify-between">
            <span className="font-semibold">Due:</span>
            <span>{formatDate(due)}</span>
          </div>
        </div>

        <div className="flex justify-between items-center mt-6">
          <time className="text-xs text-gray-500">
            Created: {formatDate(createdAt)}
          </time>
          <div className="flex gap-4">
            <ChevronUp
              size={32}
              color="#ff0000"
              className="cursor-pointer hover:scale-110 transition-transform"
            />
            <SquarePen
              size={28}
              className="cursor-pointer hover:scale-110 transition-transform"
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default CardNote;
