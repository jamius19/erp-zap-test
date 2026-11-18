/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { Navigate, Route, Routes } from 'react-router-dom';
import RequireAuth from './auth/RequireAuth';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import TeachersPage from './pages/TeachersPage';
import StudentsPage from './pages/StudentsPage';
import SubjectsPage from './pages/SubjectsPage';
import ClassesPage from './pages/ClassesPage';
import ClassDetailPage from './pages/ClassDetailPage';
import MyAttendancePage from './pages/MyAttendancePage';
import MyGradesPage from './pages/MyGradesPage';
import AnnouncementsPage from './pages/AnnouncementsPage';
import ProfilePage from './pages/ProfilePage';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route
          path="teachers"
          element={
            <RequireAuth roles={['ADMIN', 'TEACHER']}>
              <TeachersPage />
            </RequireAuth>
          }
        />
        <Route
          path="students"
          element={
            <RequireAuth roles={['ADMIN', 'TEACHER']}>
              <StudentsPage />
            </RequireAuth>
          }
        />
        <Route path="subjects" element={<SubjectsPage />} />
        <Route path="classes" element={<ClassesPage />} />
        <Route path="classes/:id" element={<ClassDetailPage />} />
        <Route
          path="my/attendance"
          element={
            <RequireAuth roles={['STUDENT']}>
              <MyAttendancePage />
            </RequireAuth>
          }
        />
        <Route
          path="my/grades"
          element={
            <RequireAuth roles={['STUDENT']}>
              <MyGradesPage />
            </RequireAuth>
          }
        />
        <Route path="announcements" element={<AnnouncementsPage />} />
        <Route path="profile" element={<ProfilePage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
