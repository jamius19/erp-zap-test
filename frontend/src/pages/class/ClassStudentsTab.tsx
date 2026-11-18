/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { api } from '../../api/client';
import type { Page, Student } from '../../api/types';
import ErrorBox from '../../components/ErrorBox';
import { useAction } from '../../components/useAction';

interface Props {
  classId: number;
  canManage: boolean;
}

export default function ClassStudentsTab({ classId, canManage }: Props) {
  const [enrolled, setEnrolled] = useState<Student[]>([]);
  const [allStudents, setAllStudents] = useState<Student[]>([]);
  const [selected, setSelected] = useState('');
  const [loadError, setLoadError] = useState<unknown>(null);
  const action = useAction();

  const load = useCallback(() => {
    api<Student[]>(`/classes/${classId}/students`).then(setEnrolled).catch(setLoadError);
  }, [classId]);

  useEffect(load, [load]);

  useEffect(() => {
    if (!canManage) return;
    // Candidates for the "add student" picker (first 100 by name).
    api<Page<Student>>('/students?size=100&sort=fullName,asc')
      .then((p) => setAllStudents(p.content))
      .catch(setLoadError);
  }, [canManage]);

  async function handleAdd(e: FormEvent) {
    e.preventDefault();
    const ok = await action.run(() =>
      api(`/classes/${classId}/students`, { method: 'POST', json: { studentId: Number(selected) } }),
    );
    if (ok) {
      setSelected('');
      load();
    }
  }

  async function handleRemove(s: Student) {
    if (!confirm(`Remove ${s.fullName} from this class?`)) return;
    if (await action.run(() => api(`/classes/${classId}/students/${s.id}`, { method: 'DELETE' }))) load();
  }

  const enrolledIds = new Set(enrolled.map((s) => s.id));
  const candidates = allStudents.filter((s) => !enrolledIds.has(s.id));

  return (
    <div>
      {canManage && (
        <form className="toolbar" onSubmit={handleAdd}>
          <select value={selected} onChange={(e) => setSelected(e.target.value)} required>
            <option value="">Select a student...</option>
            {candidates.map((s) => (
              <option key={s.id} value={s.id}>
                {s.fullName} ({s.studentNo})
              </option>
            ))}
          </select>
          <button type="submit" disabled={action.busy || !selected}>
            Add to class
          </button>
        </form>
      )}
      <ErrorBox error={loadError || action.error} />
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Student no</th>
            <th>Username</th>
            {canManage && <th />}
          </tr>
        </thead>
        <tbody>
          {enrolled.map((s) => (
            <tr key={s.id}>
              <td>{s.fullName}</td>
              <td>{s.studentNo}</td>
              <td>{s.username}</td>
              {canManage && (
                <td>
                  <button className="danger" disabled={action.busy} onClick={() => handleRemove(s)}>
                    Remove
                  </button>
                </td>
              )}
            </tr>
          ))}
          {enrolled.length === 0 && (
            <tr>
              <td colSpan={4} className="muted">
                No students enrolled.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
