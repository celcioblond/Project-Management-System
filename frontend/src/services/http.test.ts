import { AxiosError, type AxiosAdapter, type InternalAxiosRequestConfig } from 'axios';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { errorMessage, http } from './http';

// Answers every request with the Authorization header it received
const echoAuthHeader: AxiosAdapter = async (config) => ({
  data: config.headers.Authorization ?? null,
  status: 200,
  statusText: 'OK',
  headers: {},
  config,
});

const failWith = (status: number, data: unknown): AxiosAdapter => async (config) => {
  throw new AxiosError('Request failed', 'ERR_BAD_RESPONSE', config, null, {
    data,
    status,
    statusText: '',
    headers: {},
    config: config as InternalAxiosRequestConfig,
  });
};

describe('http client', () => {
  const assign = vi.fn();

  beforeEach(() => {
    vi.stubGlobal('location', { ...window.location, pathname: '/employee/dashboard', assign });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    assign.mockReset();
  });

  it('attaches the stored JWT to API requests', async () => {
    localStorage.setItem('token', 'abc123');

    const response = await http.get('/projects/my-projects', { adapter: echoAuthHeader });

    expect(response.data).toBe('Bearer abc123');
  });

  it('does not send the token to the public auth endpoints', async () => {
    localStorage.setItem('token', 'stale-token');

    const response = await http.post('/auth/login', {}, { adapter: echoAuthHeader });

    expect(response.data).toBeNull();
  });

  it('clears the session and redirects to login on 401', async () => {
    localStorage.setItem('token', 'expired');
    localStorage.setItem('user', '{}');

    await expect(http.get('/projects/my-projects', { adapter: failWith(401, {}) })).rejects.toThrow();

    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
    expect(assign).toHaveBeenCalledWith('/login');
  });

  it('keeps the session on 403', async () => {
    localStorage.setItem('token', 'valid');

    await expect(http.get('/users', { adapter: failWith(403, {}) })).rejects.toThrow();

    expect(localStorage.getItem('token')).toBe('valid');
    expect(assign).not.toHaveBeenCalled();
  });

  it('prefers the problem detail sent by the API in error messages', async () => {
    const error = await http
      .post('/auth/register', {}, { adapter: failWith(409, { detail: 'Username already in use' }) })
      .catch((e: unknown) => e);

    expect(errorMessage(error)).toBe('Username already in use');
    expect(errorMessage(new Error('boom'))).toBe('boom');
  });
});
