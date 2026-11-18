/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import type { Role } from '../api/types';
import { useAuth } from './AuthContext';

interface Props {
  roles?: Role[];
  children: ReactNode;
}

/** Redirects to /login when logged out, and to / when the user's role is not allowed. */
export default function RequireAuth({ roles, children }: Props) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return <p className="muted">Loading...</p>;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;
  return <>{children}</>;
}
