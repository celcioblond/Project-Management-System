// src/components/EmployeeDashboard/ChatBotHost.tsx
import { useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import AiChatBot from './AiChatBot';

// Mounted once above the routes so the assistant stays visible (and keeps its
// state) while the user moves between the dashboard, projects and tasks.
const CHAT_ROUTES = ['/employee', '/dashboard'];

export default function ChatBotHost() {
  const { user, isAuthenticated } = useAuth();
  const { pathname } = useLocation();

  const onChatRoute = CHAT_ROUTES.some(
    (r) => pathname === r || pathname.startsWith(`${r}/`),
  );
  if (!isAuthenticated || !user || !onChatRoute) return null;

  return <AiChatBot key={user.username} username={user.username} />;
}
