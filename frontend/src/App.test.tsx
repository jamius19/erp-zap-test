/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';
import { setAccessToken } from './api/client';
import type { Role } from './api/types';
import { AuthProvider } from './auth/AuthContext';
import { json, mockFetch } from './test/mockFetch';
import { makeMe } from './test/session';
import App from './App';

function renderApp(path: string) {
  render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <App />
      </AuthProvider>
    </MemoryRouter>,
  );
}

/** Mocks a backend where the refresh cookie restores a session for the given role. */
function loggedInAs(role: Role) {
  mockFetch(({ url }) => {
    if (url === '/api/auth/refresh') return json(200, { accessToken: 'tok', expiresIn: 900, user: makeMe(role) });
    if (url === '/api/auth/me') return json(200, makeMe(role));
    if (url === '/api/dashboard') return json(200, {});
  });
}

beforeEach(() => setAccessToken(null));

describe('protected routes', () => {
  it('redirects to /login when there is no session', async () => {
    mockFetch(({ url }) => (url === '/api/auth/refresh' ? json(401, {}) : undefined));
    renderApp('/teachers');
    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
  });

  it('redirects a student away from an admin/teacher page', async () => {
    loggedInAs('STUDENT');
    renderApp('/teachers');
    expect(await screen.findByRole('heading', { name: /Welcome/ })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Teachers' })).not.toBeInTheDocument();
  });
});

describe('role-based nav', () => {
  it('hides Teachers/Students links from a student', async () => {
    loggedInAs('STUDENT');
    renderApp('/');
    expect(await screen.findByRole('link', { name: 'My Grades' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'My Attendance' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Teachers' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Students' })).not.toBeInTheDocument();
  });

  it('shows Teachers/Students links to an admin', async () => {
    loggedInAs('ADMIN');
    renderApp('/');
    expect(await screen.findByRole('link', { name: 'Teachers' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Students' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'My Grades' })).not.toBeInTheDocument();
  });
});
