import { BrowserRouter } from 'react-router-dom';
import { createRoot } from 'react-dom/client';
import Navbar from './components/Navbar/Navbar.tsx';
import './index.css';
import App from './App.tsx';
import { AuthProvider } from './context/AuthContext.tsx';

createRoot(document.getElementById('root')!).render(
  <BrowserRouter>
    <AuthProvider>
      <div className="w-full max-w-[1000px] mx-auto px-3.5">
        <Navbar />
        <App />
      </div>
    </AuthProvider>
  </BrowserRouter>,
);
