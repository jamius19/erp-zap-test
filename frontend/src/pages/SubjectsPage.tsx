/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { api } from '../api/client';
import type { Subject } from '../api/types';
import { useUser } from '../auth/AuthContext';
import ErrorBox from '../components/ErrorBox';
import { useAction } from '../components/useAction';

export default function SubjectsPage() {
  const user = useUser();
  const isAdmin = user.role === 'ADMIN';
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loadError, setLoadError] = useState<unknown>(null);
  // null = form hidden, 'new' = create, Subject = edit
  const [editing, setEditing] = useState<Subject | 'new' | null>(null);
  const [form, setForm] = useState({ code: '', name: '' });
  const save = useAction();
  const remove = useAction();

  const load = useCallback(() => {
    api<Subject[]>('/subjects').then(setSubjects).catch(setLoadError);
  }, []);

  useEffect(load, [load]);

  function startEdit(s: Subject | 'new') {
    setEditing(s);
    save.setError(null);
    setForm(s === 'new' ? { code: '', name: '' } : { code: s.code, name: s.name });
  }

  async function handleSave(e: FormEvent) {
    e.preventDefault();
    const ok = await save.run(() =>
      editing === 'new'
        ? api('/subjects', { method: 'POST', json: form })
        : api(`/subjects/${(editing as Subject).id}`, { method: 'PUT', json: form }),
    );
    if (ok) {
      setEditing(null);
      load();
    }
  }

  async function handleDelete(s: Subject) {
    if (!confirm(`Delete subject ${s.name}?`)) return;
    if (await remove.run(() => api(`/subjects/${s.id}`, { method: 'DELETE' }))) load();
  }

  return (
    <div>
      <h1>Subjects</h1>
      {isAdmin && (
        <div className="toolbar">
          <button className="secondary" onClick={() => startEdit('new')}>
            Add subject
          </button>
        </div>
      )}

      {editing && (
        <div className="card">
          <h2>{editing === 'new' ? 'New subject' : `Edit ${editing.name}`}</h2>
          <form className="grid" onSubmit={handleSave}>
            <label>
              Code
              <input value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} required />
            </label>
            <label>
              Name
              <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
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
            <th>Code</th>
            <th>Name</th>
            {isAdmin && <th />}
          </tr>
        </thead>
        <tbody>
          {subjects.map((s) => (
            <tr key={s.id}>
              <td>{s.code}</td>
              <td>{s.name}</td>
              {isAdmin && (
                <td className="actions">
                  <button className="secondary" onClick={() => startEdit(s)}>
                    Edit
                  </button>
                  <button className="danger" disabled={remove.busy} onClick={() => handleDelete(s)}>
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
