import { Link } from 'react-router-dom';

const Navbar = () => {
  return (
    <header>
      <div className="py-8 flex justify-center items-center gap-4">
        <Link
          to={'/register'}
          className="font-semibold py-2 px-4 border hover:scale-[1.1]
         transition-transform cursor-pointer"
        >
          Register
        </Link>
        <div className="divider">|</div>
        <Link
          to={'/login'}
          className="font-semibold py-2 px-4 border hover:scale-[1.1]
         transition-transform cursor-pointer"
        >
          Login
        </Link>
      </div>
      <div className="navbar bg-base-100 shadow-sm">
        <p className="btn btn-ghost text-xl">Project Management System</p>
      </div>
    </header>
  );
};

export default Navbar;
