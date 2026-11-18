/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { api } from '../../api/client';
import { TERMS, type Grade, type Student, type Subject } from '../../api/types';
import ErrorBox from '../../components/ErrorBox';
import { useAction } from '../../components/useAction';

interface Props {
  classId: number;
  canManage: boolean;
}

const EMPTY_FORM = { studentId: '', subjectId: '', term: TERMS[0], score: '', remarks: '' };

export default function GradesTab({ classId, canManage }: Props) {
  const [grades, setGrades] = useState<Grade[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [students, setStudents] = useState<Student[]>([]);
  const [filterSubject, setFilterSubject] = useState('');
  const [filterTerm, setFilterTerm] = useState('');
  const [loadError, setLoadError] = useState<unknown>(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState({ score: '', remarks: '' });
  const create = useAction();
  const update = useAction();
  const remove = useAction();

  const load = useCallback(() => {
    const params = new URLSearchParams();
    if (filterSubject) params.set('subjectId', filterSubject);
    if (filterTerm) params.set('term', filterTerm);
    api<Grade[]>(`/classes/${classId}/grades?${params}`).then(setGrades).catch(setLoadError);
  }, [classId, filterSubject, filterTerm]);

  useEffect(load, [load]);

  useEffect(() => {
    api<Subject[]>('/subjects').then(setSubjects).catch(setLoadError);
    api<Student[]>(`/classes/${classId}/students`).then(setStudents).catch(setLoadError);
  }, [classId]);

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    const body = {
      classId,
      studentId: Number(form.studentId),
      subjectId: Number(form.subjectId),
      term: form.term,
      score: Number(form.score),
      remarks: form.remarks,
    };
    if (await create.run(() => api('/grades', { method: 'POST', json: body }))) {
      setForm(EMPTY_FORM);
      load();
    }
  }

  function startEdit(g: Grade) {
    update.setError(null);
    setEditingId(g.id);
    setEditForm({ score: String(g.score), remarks: g.remarks ?? '' });
  }

  async function handleUpdate(id: number) {
    const body = { score: Number(editForm.score), remarks: editForm.remarks };
    if (await update.run(() => api(`/grades/${id}`, { method: 'PUT', json: body }))) {
      setEditingId(null);
      load();
    }
  }

  async function handleDelete(g: Grade) {
    if (!confirm(`Delete ${g.subjectName} ${g.term} grade for ${g.studentName}?`)) return;
    if (await remove.run(() => api(`/grades/${g.id}`, { method: 'DELETE' }))) load();
  }

  const setField = (field: keyof typeof EMPTY_FORM) => (e: { target: { value: string } }) =>
    setForm({ ...form, [field]: e.target.value });

  return (
    <div>
      <div className="toolbar">
        <label>
          Subject
          <select value={filterSubject} onChange={(e) => setFilterSubject(e.target.value)}>
            <option value="">All</option>
            {subjects.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Term
          <select value={filterTerm} onChange={(e) => setFilterTerm(e.target.value)}>
            <option value="">All</option>
            {TERMS.map((t) => (
              <option key={t}>{t}</option>
            ))}
          </select>
        </label>
      </div>

      {canManage && (
        <div className="card">
          <h2>Add grade</h2>
          <form className="grid" onSubmit={handleCreate}>
            <label>
              Student
              <select value={form.studentId} onChange={setField('studentId')} required>
                <option value="">Select...</option>
                {students.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.fullName}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Subject
              <select value={form.subjectId} onChange={setField('subjectId')} required>
                <option value="">Select...</option>
                {subjects.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Term
              <select value={form.term} onChange={setField('term')}>
                {TERMS.map((t) => (
                  <option key={t}>{t}</option>
                ))}
              </select>
            </label>
            <label>
              Score
              <input type="number" min={0} max={100} step="0.01" value={form.score} onChange={setField('score')} required />
            </label>
            <label>
              Remarks
              <input value={form.remarks} onChange={setField('remarks')} />
            </label>
            <button type="submit" disabled={create.busy}>
              {create.busy ? 'Saving...' : 'Add'}
            </button>
          </form>
          <ErrorBox error={create.error} />
        </div>
      )}

      <ErrorBox error={loadError || update.error || remove.error} />
      <table>
        <thead>
          <tr>
            <th>Student</th>
            <th>Subject</th>
            <th>Term</th>
            <th>Score</th>
            <th>Remarks</th>
            {canManage && <th />}
          </tr>
        </thead>
        <tbody>
          {grades.map((g) =>
            editingId === g.id ? (
              <tr key={g.id}>
                <td>{g.studentName}</td>
                <td>{g.subjectName}</td>
                <td>{g.term}</td>
                <td>
                  <input
                    type="number"
                    min={0}
                    max={100}
                    step="0.01"
                    value={editForm.score}
                    onChange={(e) => setEditForm({ ...editForm, score: e.target.value })}
                  />
                </td>
                <td>
                  <input value={editForm.remarks} onChange={(e) => setEditForm({ ...editForm, remarks: e.target.value })} />
                </td>
                <td className="actions">
                  <button disabled={update.busy} onClick={() => handleUpdate(g.id)}>
                    {update.busy ? 'Saving...' : 'Save'}
                  </button>
                  <button className="secondary" onClick={() => setEditingId(null)}>
                    Cancel
                  </button>
                </td>
              </tr>
            ) : (
              <tr key={g.id}>
                <td>{g.studentName}</td>
                <td>{g.subjectName}</td>
                <td>{g.term}</td>
                <td>{g.score}</td>
                <td>{g.remarks}</td>
                {canManage && (
                  <td className="actions">
                    <button className="secondary" onClick={() => startEdit(g)}>
                      Edit
                    </button>
                    <button className="danger" disabled={remove.busy} onClick={() => handleDelete(g)}>
                      Delete
                    </button>
                  </td>
                )}
              </tr>
            ),
          )}
          {grades.length === 0 && (
            <tr>
              <td colSpan={6} className="muted">
                No grades.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
