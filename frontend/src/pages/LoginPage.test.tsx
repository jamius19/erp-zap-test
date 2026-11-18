/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';
import { setAccessToken } from '../api/client';
import { AuthProvider } from '../auth/AuthContext';
import { json, mockFetch } from '../test/mockFetch';
import { makeMe } from '../test/session';
import LoginPage from './LoginPage';

function renderLogin() {
  render(
    <MemoryRouter initialEntries={['/login']}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<div>Home page</div>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

beforeEach(() => setAccessToken(null));

describe('LoginPage', () => {
  it('submits credentials and redirects on success', async () => {
    const calls = mockFetch(({ url }) => {
      if (url === '/api/auth/refresh') return json(401, {});
      if (url === '/api/auth/login') return json(200, { accessToken: 'tok', expiresIn: 900, user: makeMe('ADMIN') });
      if (url === '/api/auth/me') return json(200, makeMe('ADMIN'));
    });
    renderLogin();

    await userEvent.type(await screen.findByLabelText('Username'), 'admin');
    await userEvent.type(screen.getByLabelText('Password'), 'Admin@1234');
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByText('Home page')).toBeInTheDocument();
    const loginCall = calls.find((c) => c.url === '/api/auth/login');
    expect(loginCall).toMatchObject({ method: 'POST', body: { username: 'admin', password: 'Admin@1234' } });
    expect(calls.find((c) => c.url === '/api/auth/me')?.headers.get('Authorization')).toBe('Bearer tok');
  });

  it('shows the API error detail on failure', async () => {
    mockFetch(({ url }) => {
      if (url === '/api/auth/refresh') return json(401, {});
      if (url === '/api/auth/login') return json(401, { status: 401, detail: 'Invalid username or password' });
    });
    renderLogin();

    await userEvent.type(await screen.findByLabelText('Username'), 'admin');
    await userEvent.type(screen.getByLabelText('Password'), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid username or password');
    expect(screen.queryByText('Home page')).not.toBeInTheDocument();
  });
});
