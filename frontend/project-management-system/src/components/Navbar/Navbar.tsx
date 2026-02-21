// src/components/Navbar/Navbar.tsx
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const Navbar = () => {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header>
      {!isAuthenticated && (
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
      )}

      <div className="navbar bg-base-100 shadow-sm">
        <div className="flex-1">
          <p className="btn btn-ghost text-xl">Project Management System</p>
        </div>

        {isAuthenticated && (
          <div className="flex-none gap-2">
            <div className="dropdown dropdown-end">
              <div tabIndex={0} role="button" className="btn btn-ghost">
                <span className="font-semibold">{user?.username}</span>
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  className="inline-block w-5 h-5 stroke-current"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M19 9l-7 7-7-7"
                  ></path>
                </svg>
              </div>
              <ul
                tabIndex={0}
                className="mt-3 z-[1] p-2 shadow menu menu-sm dropdown-content bg-base-100 rounded-box w-52"
              >
                <li>
                  <span className="text-sm">Role: {user?.role}</span>
                </li>
                <li>
                  <button onClick={handleLogout} className="text-error">
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      fill="none"
                      viewBox="0 0 24 24"
                      strokeWidth={1.5}
                      stroke="currentColor"
                      className="w-5 h-5"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75"
                      />
                    </svg>
                    Sign Out
                  </button>
                </li>
              </ul>
            </div>
          </div>
        )}
      </div>
    </header>
  );
};

export default Navbar;
