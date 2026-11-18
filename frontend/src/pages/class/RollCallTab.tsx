/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import { ATTENDANCE_STATUSES, type AttendanceRecord, type AttendanceStatus, type Student } from '../../api/types';
import ErrorBox from '../../components/ErrorBox';
import { useAction } from '../../components/useAction';

interface Props {
  classId: number;
  canManage: boolean;
}

/** Today's date as YYYY-MM-DD in local time. */
function today() {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export default function RollCallTab({ classId, canManage }: Props) {
  const [date, setDate] = useState(today);
  const [students, setStudents] = useState<Student[]>([]);
  const [statuses, setStatuses] = useState<Record<number, AttendanceStatus>>({});
  const [loadError, setLoadError] = useState<unknown>(null);
  const [saved, setSaved] = useState(false);
  const save = useAction();

  useEffect(() => {
    setSaved(false);
    setLoadError(null);
    Promise.all([
      api<Student[]>(`/classes/${classId}/students`),
      api<AttendanceRecord[]>(`/classes/${classId}/attendance?date=${encodeURIComponent(date)}`),
    ])
      .then(([studentList, records]) => {
        setStudents(studentList);
        // Default everyone to PRESENT, then apply whatever is already recorded for that day.
        const map: Record<number, AttendanceStatus> = {};
        studentList.forEach((s) => (map[s.id] = 'PRESENT'));
        records.forEach((r) => (map[r.studentId] = r.status));
        setStatuses(map);
      })
      .catch(setLoadError);
  }, [classId, date]);

  async function handleSave() {
    setSaved(false);
    const records = students.map((s) => ({ studentId: s.id, status: statuses[s.id] }));
    const ok = await save.run(() =>
      api(`/classes/${classId}/attendance`, { method: 'PUT', json: { date, records } }),
    );
    setSaved(ok);
  }

  return (
    <div>
      <div className="toolbar">
        <label>
          Date
          <input type="date" value={date} max={today()} onChange={(e) => setDate(e.target.value)} required />
        </label>
      </div>
      <ErrorBox error={loadError} />
      <table>
        <thead>
          <tr>
            <th>Student</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {students.map((s) => (
            <tr key={s.id}>
              <td>{s.fullName}</td>
              <td>
                <select
                  aria-label={`Status for ${s.fullName}`}
                  value={statuses[s.id] ?? 'PRESENT'}
                  disabled={!canManage}
                  onChange={(e) => setStatuses({ ...statuses, [s.id]: e.target.value as AttendanceStatus })}
                >
                  {ATTENDANCE_STATUSES.map((st) => (
                    <option key={st} value={st}>
                      {st}
                    </option>
                  ))}
                </select>
              </td>
            </tr>
          ))}
          {students.length === 0 && (
            <tr>
              <td colSpan={2} className="muted">
                No students enrolled.
              </td>
            </tr>
          )}
        </tbody>
      </table>
      <ErrorBox error={save.error} />
      {saved && <div className="success">Attendance saved.</div>}
      {canManage && (
        <p>
          <button onClick={handleSave} disabled={save.busy || students.length === 0 || !date}>
            {save.busy ? 'Saving...' : 'Save'}
          </button>
        </p>
      )}
    </div>
  );
}
