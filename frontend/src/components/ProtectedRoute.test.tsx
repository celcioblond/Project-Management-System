import { screen } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import ProtectedRoute from './ProtectedRoute';
import { renderWithAuth } from '../test/renderWithAuth';
import type { AuthResponse } from '../types/api';

const employee: AuthResponse = { token: 't', username: 'alice', role: 'EMPLOYEE' };
const admin: AuthResponse = { token: 't', username: 'root', role: 'ADMIN' };

function renderAdminRoute(user: AuthResponse | null) {
  renderWithAuth(
    <Routes>
      <Route
        path="/admin"
        element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <p>admin area</p>
          </ProtectedRoute>
        }
      />
      <Route path="/login" element={<p>login page</p>} />
      <Route path="/unauthorized" element={<p>unauthorized page</p>} />
    </Routes>,
    { user, route: '/admin' },
  );
}

describe('ProtectedRoute', () => {
  it('redirects anonymous visitors to login', () => {
    renderAdminRoute(null);
    expect(screen.getByText('login page')).toBeInTheDocument();
  });

  it('sends users without the required role to /unauthorized', () => {
    renderAdminRoute(employee);
    expect(screen.getByText('unauthorized page')).toBeInTheDocument();
  });

  it('renders the page for users with the required role', () => {
    renderAdminRoute(admin);
    expect(screen.getByText('admin area')).toBeInTheDocument();
  });
});
