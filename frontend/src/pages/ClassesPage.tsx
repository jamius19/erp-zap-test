/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import type { Page, SchoolClass, Teacher } from '../api/types';
import { useUser } from '../auth/AuthContext';
import ErrorBox from '../components/ErrorBox';
import { useAction } from '../components/useAction';

const EMPTY_FORM = { name: '', gradeLevel: '1', section: '', academicYear: '2026-2027', teacherId: '' };

export default function ClassesPage() {
  const user = useUser();
  const isAdmin = user.role === 'ADMIN';
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [mine, setMine] = useState(false);
  const [teachers, setTeachers] = useState<Teacher[]>([]);
  const [loadError, setLoadError] = useState<unknown>(null);
  // null = form hidden, 'new' = create, SchoolClass = edit
  const [editing, setEditing] = useState<SchoolClass | 'new' | null>(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const save = useAction();
  const remove = useAction();

  const load = useCallback(() => {
    api<SchoolClass[]>(mine ? '/classes?mine=true' : '/classes')
      .then(setClasses)
      .catch(setLoadError);
  }, [mine]);

  useEffect(load, [load]);

  useEffect(() => {
    if (!isAdmin) return;
    api<Page<Teacher>>('/teachers?size=100&sort=fullName,asc')
      .then((p) => setTeachers(p.content))
      .catch(setLoadError);
  }, [isAdmin]);

  function startEdit(c: SchoolClass | 'new') {
    setEditing(c);
    save.setError(null);
    setForm(
      c === 'new'
        ? EMPTY_FORM
        : {
            name: c.name,
            gradeLevel: String(c.gradeLevel),
            section: c.section ?? '',
            academicYear: c.academicYear ?? '',
            teacherId: c.teacherId ? String(c.teacherId) : '',
          },
    );
  }

  async function handleSave(e: FormEvent) {
    e.preventDefault();
    const body = {
      ...form,
      gradeLevel: Number(form.gradeLevel),
      teacherId: form.teacherId ? Number(form.teacherId) : null,
    };
    const ok = await save.run(() =>
      editing === 'new'
        ? api('/classes', { method: 'POST', json: body })
        : api(`/classes/${(editing as SchoolClass).id}`, { method: 'PUT', json: body }),
    );
    if (ok) {
      setEditing(null);
      load();
    }
  }

  async function handleDelete(c: SchoolClass) {
    if (!confirm(`Delete class ${c.name}?`)) return;
    if (await remove.run(() => api(`/classes/${c.id}`, { method: 'DELETE' }))) load();
  }

  const set = (field: keyof typeof EMPTY_FORM) => (e: { target: { value: string } }) =>
    setForm({ ...form, [field]: e.target.value });

  return (
    <div>
      <h1>Classes</h1>
      <div className="toolbar">
        {user.role === 'TEACHER' && (
          <label style={{ flexDirection: 'row', alignItems: 'center' }}>
            <input type="checkbox" checked={mine} onChange={(e) => setMine(e.target.checked)} />
            Only my classes
          </label>
        )}
        {isAdmin && (
          <button className="secondary" onClick={() => startEdit('new')}>
            Add class
          </button>
        )}
      </div>

      {editing && (
        <div className="card">
          <h2>{editing === 'new' ? 'New class' : `Edit ${editing.name}`}</h2>
          <form className="grid" onSubmit={handleSave}>
            <label>
              Name
              <input value={form.name} onChange={set('name')} required />
            </label>
            <label>
              Grade level
              <input type="number" min={1} max={12} value={form.gradeLevel} onChange={set('gradeLevel')} required />
            </label>
            <label>
              Section
              <input value={form.section} onChange={set('section')} />
            </label>
            <label>
              Academic year
              <input value={form.academicYear} onChange={set('academicYear')} placeholder="2026-2027" required />
            </label>
            <label>
              Class teacher
              <select value={form.teacherId} onChange={set('teacherId')}>
                <option value="">(none)</option>
                {teachers.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.fullName}
                  </option>
                ))}
              </select>
            </label>
            <div className="actions">
              <button type="submit" disabled={save.busy}>
                {save.busy ? 'Saving...' : 'Save'}
              </button>
              <button type="button" className="secondary" onClick={() => setEditing(null)}>
                Cancel
              </button>
            </div>
          </form>
          <ErrorBox error={save.error} />
        </div>
      )}

      <ErrorBox error={loadError || remove.error} />
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <th>Section</th>
            <th>Year</th>
            <th>Teacher</th>
            <th>Students</th>
            {isAdmin && <th />}
          </tr>
        </thead>
        <tbody>
          {classes.map((c) => (
            <tr key={c.id}>
              <td>
                <Link to={`/classes/${c.id}`}>{c.name}</Link>
              </td>
              <td>{c.gradeLevel}</td>
              <td>{c.section}</td>
              <td>{c.academicYear}</td>
              <td>{c.teacherName ?? '-'}</td>
              <td>{c.studentCount}</td>
              {isAdmin && (
                <td className="actions">
                  <button className="secondary" onClick={() => startEdit(c)}>
                    Edit
                  </button>
                  <button className="danger" disabled={remove.busy} onClick={() => handleDelete(c)}>
                    Delete
                  </button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
