/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import type { Role } from '../api/types';
import { useUser, useAuth } from '../auth/AuthContext';

const NAV_ITEMS: { to: string; label: string; roles: Role[] }[] = [
  { to: '/', label: 'Dashboard', roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { to: '/teachers', label: 'Teachers', roles: ['ADMIN', 'TEACHER'] },
  { to: '/students', label: 'Students', roles: ['ADMIN', 'TEACHER'] },
  { to: '/subjects', label: 'Subjects', roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { to: '/classes', label: 'Classes', roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { to: '/my/attendance', label: 'My Attendance', roles: ['STUDENT'] },
  { to: '/my/grades', label: 'My Grades', roles: ['STUDENT'] },
  { to: '/announcements', label: 'Announcements', roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { to: '/profile', label: 'Profile', roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
];

export default function Layout() {
  const user = useUser();
  const { logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate('/login');
  }

  return (
    <div className="app">
      <header className="topbar">
        <strong>School ERP</strong>
        <nav>
          {NAV_ITEMS.filter((item) => item.roles.includes(user.role)).map((item) => (
            <NavLink key={item.to} to={item.to} end={item.to === '/'}>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <span className="user">
          {user.fullName} ({user.role})
          <button onClick={handleLogout}>Logout</button>
        </span>
      </header>
      <main>
        <Outlet />
      </main>
    </div>
  );
}
