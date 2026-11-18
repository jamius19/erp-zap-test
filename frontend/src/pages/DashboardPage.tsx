/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { useUser } from '../auth/AuthContext';
import ErrorBox from '../components/ErrorBox';

const LABELS: Record<string, string> = {
  teachers: 'Teachers',
  students: 'Students',
  classes: 'Classes',
  subjects: 'Subjects',
  myClasses: 'My classes',
  myStudents: 'My students',
  attendanceRate: 'Attendance rate (%)',
  averageScore: 'Average score',
};

export default function DashboardPage() {
  const user = useUser();
  const [stats, setStats] = useState<Record<string, number | null> | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    api<Record<string, number | null>>('/dashboard').then(setStats).catch(setError);
  }, []);

  return (
    <div>
      <h1>Welcome, {user.fullName}</h1>
      <ErrorBox error={error} />
      {stats && (
        <div className="cards">
          {Object.entries(stats).map(([key, value]) => (
            <div className="card stat" key={key}>
              <div className="muted">{LABELS[key] ?? key}</div>
              <div className="value">{value === null ? '-' : Number.isInteger(value) ? value : value.toFixed(1)}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
