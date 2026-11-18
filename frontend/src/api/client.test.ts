/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError, getAccessToken, login, logout, setAccessToken, setAuthFailureHandler } from './client';
import { json, mockFetch } from '../test/mockFetch';

const AUTH_OK = { accessToken: 'new', expiresIn: 900, user: { id: 1 } };

beforeEach(() => {
  setAccessToken('old');
  setAuthFailureHandler(null);
});

describe('api client', () => {
  it('sends the bearer token', async () => {
    const calls = mockFetch(() => json(200, []));
    await api('/subjects');
    expect(calls[0].url).toBe('/api/subjects');
    expect(calls[0].headers.get('Authorization')).toBe('Bearer old');
  });

  it('refreshes on 401 and retries the request with the new token', async () => {
    const calls = mockFetch(({ url, headers }) => {
      if (url === '/api/auth/refresh') return json(200, AUTH_OK);
      if (url === '/api/teachers') {
        return headers.get('Authorization') === 'Bearer new' ? json(200, { ok: true }) : json(401, {});
      }
    });

    await expect(api('/teachers')).resolves.toEqual({ ok: true });
    expect(calls.map((c) => `${c.method} ${c.url}`)).toEqual([
      'GET /api/teachers',
      'POST /api/auth/refresh',
      'GET /api/teachers',
    ]);
    expect(getAccessToken()).toBe('new');
  });

  it('shares one in-flight refresh between parallel 401s', async () => {
    const calls = mockFetch(async ({ url, headers }) => {
      if (url === '/api/auth/refresh') {
        await new Promise((r) => setTimeout(r, 20));
        return json(200, AUTH_OK);
      }
      return headers.get('Authorization') === 'Bearer new' ? json(200, { url }) : json(401, {});
    });

    const results = await Promise.all([api('/teachers'), api('/students'), api('/subjects')]);
    expect(results).toEqual([{ url: '/api/teachers' }, { url: '/api/students' }, { url: '/api/subjects' }]);
    expect(calls.filter((c) => c.url === '/api/auth/refresh')).toHaveLength(1);
  });

  it('calls the auth failure handler and throws when refresh fails', async () => {
    const onFail = vi.fn();
    setAuthFailureHandler(onFail);
    mockFetch(({ url }) => (url === '/api/auth/refresh' ? json(401, {}) : json(401, { detail: 'Expired' })));

    await expect(api('/teachers')).rejects.toMatchObject({ status: 401 });
    expect(onFail).toHaveBeenCalledTimes(1);
    expect(getAccessToken()).toBeNull();
  });

  it('does not try to refresh when login returns 401', async () => {
    const calls = mockFetch(() => json(401, { detail: 'Invalid username or password' }));
    const err = await login('x', 'y').catch((e) => e);
    expect(err).toBeInstanceOf(ApiError);
    expect(err.message).toBe('Invalid username or password');
    expect(calls).toHaveLength(1);
  });

  it('parses ProblemDetail field errors', async () => {
    mockFetch(() => json(400, { status: 400, detail: 'Validation failed', errors: { code: 'must not be blank' } }));
    const err = await api('/subjects', { method: 'POST', json: {} }).catch((e) => e);
    expect(err).toMatchObject({ status: 400, message: 'Validation failed', errors: { code: 'must not be blank' } });
  });

  it('logout calls the endpoint and clears the token', async () => {
    const calls = mockFetch(() => new Response(null, { status: 204 }));
    await logout();
    expect(calls[0]).toMatchObject({ method: 'POST', url: '/api/auth/logout' });
    expect(getAccessToken()).toBeNull();
  });
});
