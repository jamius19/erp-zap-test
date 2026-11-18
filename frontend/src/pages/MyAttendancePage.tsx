/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { AttendanceRecord } from '../api/types';
import { useUser } from '../auth/AuthContext';
import ErrorBox from '../components/ErrorBox';

export default function MyAttendancePage() {
  const user = useUser();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [records, setRecords] = useState<AttendanceRecord[]>([]);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    if (!user.studentId) return;
    const params = new URLSearchParams();
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    setError(null);
    api<AttendanceRecord[]>(`/students/${user.studentId}/attendance?${params}`).then(setRecords).catch(setError);
  }, [user.studentId, from, to]);

  if (!user.studentId) return <p className="muted">No student profile is linked to this account.</p>;

  return (
    <div>
      <h1>My Attendance</h1>
      <div className="toolbar">
        <label>
          From
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </label>
        <label>
          To
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </label>
      </div>
      <ErrorBox error={error} />
      <table>
        <thead>
          <tr>
            <th>Date</th>
            <th>Class</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {records.map((r) => (
            <tr key={r.id}>
              <td>{r.date}</td>
              <td>{r.className}</td>
              <td>{r.status}</td>
            </tr>
          ))}
          {records.length === 0 && (
            <tr>
              <td colSpan={3} className="muted">
                No attendance records.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
