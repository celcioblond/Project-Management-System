import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import Login from './Login';
import { apiService } from '../../services/api';
import { renderWithAuth } from '../../test/renderWithAuth';

function renderLogin() {
  return renderWithAuth(
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/admin/dashboard" element={<p>admin dashboard</p>} />
      <Route path="/dashboard" element={<p>employee dashboard</p>} />
    </Routes>,
    { route: '/login' },
  );
}

async function submit(username: string, password: string) {
  const user = userEvent.setup();
  await user.type(screen.getByPlaceholderText('Username'), username);
  await user.type(screen.getByPlaceholderText('Password'), password);
  await user.click(screen.getByRole('button', { name: /login/i }));
}

describe('Login', () => {
  it('shows the error returned by the API', async () => {
    vi.spyOn(apiService, 'login').mockRejectedValue(new Error('Login failed: Invalid username or password.'));
    renderLogin();

    await submit('alice01', 'wrongpass');

    expect(await screen.findByText('Login failed: Invalid username or password.')).toBeInTheDocument();
  });

  it('stores the session and routes admins to the admin dashboard', async () => {
    const session = { token: 'jwt', username: 'admin1', role: 'ADMIN' };
    vi.spyOn(apiService, 'login').mockResolvedValue(session);
    const auth = renderLogin();

    await submit('admin1', 'Passw0rd!x');

    expect(await screen.findByText('admin dashboard')).toBeInTheDocument();
    expect(auth.login).toHaveBeenCalledWith(session);
  });

  it('validates the form before calling the API', async () => {
    const login = vi.spyOn(apiService, 'login');
    renderLogin();

    await submit('bad!', 'x');

    expect(await screen.findByText(/Username must be 5-15 characters/)).toBeInTheDocument();
    expect(screen.getByText('Password must be at least 6 characters')).toBeInTheDocument();
    expect(login).not.toHaveBeenCalled();
  });
});
