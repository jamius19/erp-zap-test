/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { api } from '../api/client';
import type { Page, Student } from '../api/types';
import { useUser } from '../auth/AuthContext';
import ErrorBox from '../components/ErrorBox';
import Pager from '../components/Pager';
import { useAction } from '../components/useAction';

const EMPTY_FORM = {
  username: '',
  password: '',
  fullName: '',
  email: '',
  studentNo: '',
  dateOfBirth: '',
  guardianName: '',
  guardianPhone: '',
};

export default function StudentsPage() {
  const user = useUser();
  const isAdmin = user.role === 'ADMIN';
  const [data, setData] = useState<Page<Student> | null>(null);
  const [q, setQ] = useState('');
  const [search, setSearch] = useState('');
  const [pageNo, setPageNo] = useState(0);
  const [loadError, setLoadError] = useState<unknown>(null);
  // null = form hidden, 'new' = create, Student = edit
  const [editing, setEditing] = useState<Student | 'new' | null>(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const save = useAction();
  const remove = useAction();

  const load = useCallback(() => {
    const params = new URLSearchParams({ page: String(pageNo), size: '20', sort: 'fullName,asc', q: search });
    api<Page<Student>>(`/students?${params}`).then(setData).catch(setLoadError);
  }, [pageNo, search]);

  useEffect(load, [load]);

  function startEdit(s: Student | 'new') {
    setEditing(s);
    save.setError(null);
    setForm(
      s === 'new'
        ? EMPTY_FORM
        : {
            ...EMPTY_FORM,
            fullName: s.fullName,
            email: s.email ?? '',
            studentNo: s.studentNo ?? '',
            dateOfBirth: s.dateOfBirth ?? '',
            guardianName: s.guardianName ?? '',
            guardianPhone: s.guardianPhone ?? '',
          },
    );
  }

  async function handleSave(e: FormEvent) {
    e.preventDefault();
    const { username, password, ...rest } = form;
    const body = { ...rest, dateOfBirth: rest.dateOfBirth || null };
    const ok = await save.run(() =>
      editing === 'new'
        ? api('/students', { method: 'POST', json: { username, password, ...body } })
        : api(`/students/${(editing as Student).id}`, { method: 'PUT', json: body }),
    );
    if (ok) {
      setEditing(null);
      load();
    }
  }

  async function handleDelete(s: Student) {
    if (!confirm(`Delete student ${s.fullName}?`)) return;
    if (await remove.run(() => api(`/students/${s.id}`, { method: 'DELETE' }))) load();
  }

  const set = (field: keyof typeof EMPTY_FORM) => (e: { target: { value: string } }) =>
    setForm({ ...form, [field]: e.target.value });

  return (
    <div>
      <h1>Students</h1>
      <form
        className="toolbar"
        onSubmit={(e) => {
          e.preventDefault();
          setPageNo(0);
          setSearch(q);
        }}
      >
        <input placeholder="Search name, username, student no" value={q} onChange={(e) => setQ(e.target.value)} />
        <button type="submit">Search</button>
        {isAdmin && (
          <button type="button" className="secondary" onClick={() => startEdit('new')}>
            Add student
          </button>
        )}
      </form>

      {editing && (
        <div className="card">
          <h2>{editing === 'new' ? 'New student' : `Edit ${editing.fullName}`}</h2>
          <form className="grid" onSubmit={handleSave}>
            {editing === 'new' && (
              <>
                <label>
                  Username
                  <input value={form.username} onChange={set('username')} required />
                </label>
                <label>
                  Password
                  <input type="password" value={form.password} onChange={set('password')} required />
                </label>
              </>
            )}
            <label>
              Full name
              <input value={form.fullName} onChange={set('fullName')} required />
            </label>
            <label>
              Email
              <input type="email" value={form.email} onChange={set('email')} />
            </label>
            <label>
              Student no
              <input value={form.studentNo} onChange={set('studentNo')} />
            </label>
            <label>
              Date of birth
              <input type="date" value={form.dateOfBirth} onChange={set('dateOfBirth')} />
            </label>
            <label>
              Guardian name
              <input value={form.guardianName} onChange={set('guardianName')} />
            </label>
            <label>
              Guardian phone
              <input value={form.guardianPhone} onChange={set('guardianPhone')} />
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
      {data && (
        <>
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Username</th>
                <th>Student no</th>
                <th>Date of birth</th>
                <th>Guardian</th>
                {isAdmin && <th />}
              </tr>
            </thead>
            <tbody>
              {data.content.map((s) => (
                <tr key={s.id}>
                  <td>{s.fullName}</td>
                  <td>{s.username}</td>
                  <td>{s.studentNo}</td>
                  <td>{s.dateOfBirth}</td>
                  <td>
                    {s.guardianName} {s.guardianPhone && `(${s.guardianPhone})`}
                  </td>
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
          <Pager page={data.page} onChange={setPageNo} />
        </>
      )}
    </div>
  );
}
