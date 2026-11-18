/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Grade } from '../api/types';
import { useUser } from '../auth/AuthContext';
import ErrorBox from '../components/ErrorBox';

export default function MyGradesPage() {
  const user = useUser();
  const [grades, setGrades] = useState<Grade[]>([]);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    if (!user.studentId) return;
    api<Grade[]>(`/students/${user.studentId}/grades`).then(setGrades).catch(setError);
  }, [user.studentId]);

  if (!user.studentId) return <p className="muted">No student profile is linked to this account.</p>;

  return (
    <div>
      <h1>My Grades</h1>
      <ErrorBox error={error} />
      <table>
        <thead>
          <tr>
            <th>Class</th>
            <th>Subject</th>
            <th>Term</th>
            <th>Score</th>
            <th>Remarks</th>
          </tr>
        </thead>
        <tbody>
          {grades.map((g) => (
            <tr key={g.id}>
              <td>{g.className}</td>
              <td>{g.subjectName}</td>
              <td>{g.term}</td>
              <td>{g.score}</td>
              <td>{g.remarks}</td>
            </tr>
          ))}
          {grades.length === 0 && (
            <tr>
              <td colSpan={5} className="muted">
                No grades yet.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
