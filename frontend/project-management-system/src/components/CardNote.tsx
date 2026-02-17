import {SquarePen} from "lucide-react";

const CardNote = () => {
  return (
    <div className="card card-border bg-base-300 w-full">
      <div className="card-body">
        <h2 className="card-title text-accent font-bold lg:text-2xl">Card Title</h2>
        <p className="text-red-950">
          A card component has a figure, a body part, and inside body there are
          title and actions parts
        </p>
        <div className="flex justify-between items-center mt-6">
          <time datetime="">16 de febrero de 2026</time>
          <div className="flex-gap-4">
            <SquarePen className="text-white cursor-pointer" />
          </div>
        </div>
      </div>
    </div>
  );
}

export default CardNote;