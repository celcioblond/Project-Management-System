import type { ReactElement } from 'react';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { AuthContext, type AuthContextType } from '../context/auth-context';
import type { AuthResponse } from '../types/api';

/** Renders a component inside a router and a stubbed auth context. */
export function renderWithAuth(
  ui: ReactElement,
  { user = null, route = '/' }: { user?: AuthResponse | null; route?: string } = {},
) {
  const auth: AuthContextType = {
    user,
    isAuthenticated: user !== null,
    login: vi.fn(),
    logout: vi.fn(),
  };

  render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
    </AuthContext.Provider>,
  );
  return auth;
}
