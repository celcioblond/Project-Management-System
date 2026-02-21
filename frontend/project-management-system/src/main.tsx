// src/main.tsx
import { BrowserRouter } from 'react-router-dom';
import { createRoot } from 'react-dom/client';
import Navbar from './components/Navbar/Navbar.tsx';
import './index.css';
import App from './App.tsx';
import { AuthProvider } from './context/AuthContext.tsx';
import ErrorBoundary from './components/ErrorBoundary.tsx';

createRoot(document.getElementById('root')!).render(
  <BrowserRouter>
    <ErrorBoundary>
      <AuthProvider>
        <div className="w-full max-w-[1000px] mx-auto px-3.5">
          <Navbar />
          <App />
        </div>
      </AuthProvider>
    </ErrorBoundary>
  </BrowserRouter>,
);
