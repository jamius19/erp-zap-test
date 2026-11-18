/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import * as client from '../api/client';
import type { Me } from '../api/types';

interface AuthState {
  user: Me | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  reloadUser: () => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);

  const reloadUser = useCallback(async () => {
    setUser(await client.api<Me>('/auth/me'));
  }, []);

  useEffect(() => {
    // If a refresh fails after a 401, drop the user so protected routes redirect to /login.
    client.setAuthFailureHandler(() => setUser(null));

    // Restore the session from the refresh cookie on app load.
    client
      .refreshSession()
      .then((res) => (res ? reloadUser() : undefined))
      .catch(() => setUser(null))
      .finally(() => setLoading(false));

    return () => client.setAuthFailureHandler(null);
  }, [reloadUser]);

  const login = useCallback(
    async (username: string, password: string) => {
      await client.login(username, password);
      await reloadUser();
    },
    [reloadUser],
  );

  const logout = useCallback(async () => {
    await client.logout();
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, reloadUser }}>{children}</AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}

/** Same as useAuth().user but asserts the user is logged in (use only under RequireAuth). */
export function useUser(): Me {
  const { user } = useAuth();
  if (!user) throw new Error('No logged-in user');
  return user;
}
