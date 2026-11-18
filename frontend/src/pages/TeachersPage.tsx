/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { api } from '../api/client';
import type { Page, Teacher } from '../api/types';
import { useUser } from '../auth/AuthContext';
import ErrorBox from '../components/ErrorBox';
import Pager from '../components/Pager';
import { useAction } from '../components/useAction';

const EMPTY_FORM = { username: '', password: '', fullName: '', email: '', employeeNo: '', phone: '' };

export default function TeachersPage() {
  const user = useUser();
  const isAdmin = user.role === 'ADMIN';
  const [data, setData] = useState<Page<Teacher> | null>(null);
  const [q, setQ] = useState('');
  const [search, setSearch] = useState('');
  const [pageNo, setPageNo] = useState(0);
  const [loadError, setLoadError] = useState<unknown>(null);
  // null = form hidden, 'new' = create, Teacher = edit
  const [editing, setEditing] = useState<Teacher | 'new' | null>(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const save = useAction();
  const remove = useAction();

  const load = useCallback(() => {
    const params = new URLSearchParams({ page: String(pageNo), size: '20', sort: 'fullName,asc', q: search });
    api<Page<Teacher>>(`/teachers?${params}`).then(setData).catch(setLoadError);
  }, [pageNo, search]);

  useEffect(load, [load]);

  function startEdit(t: Teacher | 'new') {
    setEditing(t);
    save.setError(null);
    setForm(t === 'new' ? EMPTY_FORM : { ...EMPTY_FORM, ...pick(t) });
  }

  async function handleSave(e: FormEvent) {
    e.preventDefault();
    const ok = await save.run(() =>
      editing === 'new'
        ? api('/teachers', { method: 'POST', json: form })
        : api(`/teachers/${(editing as Teacher).id}`, {
            method: 'PUT',
            json: { fullName: form.fullName, email: form.email, employeeNo: form.employeeNo, phone: form.phone },
          }),
    );
    if (ok) {
      setEditing(null);
      load();
    }
  }

  async function handleDelete(t: Teacher) {
    if (!confirm(`Delete teacher ${t.fullName}? This also deletes their user account.`)) return;
    if (await remove.run(() => api(`/teachers/${t.id}`, { method: 'DELETE' }))) load();
  }

  const set = (field: keyof typeof EMPTY_FORM) => (e: { target: { value: string } }) =>
    setForm({ ...form, [field]: e.target.value });

  return (
    <div>
      <h1>Teachers</h1>
      <form
        className="toolbar"
        onSubmit={(e) => {
          e.preventDefault();
          setPageNo(0);
          setSearch(q);
        }}
      >
        <input placeholder="Search name, username, employee no" value={q} onChange={(e) => setQ(e.target.value)} />
        <button type="submit">Search</button>
        {isAdmin && (
          <button type="button" className="secondary" onClick={() => startEdit('new')}>
            Add teacher
          </button>
        )}
      </form>

      {editing && (
        <div className="card">
          <h2>{editing === 'new' ? 'New teacher' : `Edit ${editing.fullName}`}</h2>
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
              Employee no
              <input value={form.employeeNo} onChange={set('employeeNo')} />
            </label>
            <label>
              Phone
              <input value={form.phone} onChange={set('phone')} />
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
                <th>Employee no</th>
                <th>Email</th>
                <th>Phone</th>
                {isAdmin && <th />}
              </tr>
            </thead>
            <tbody>
              {data.content.map((t) => (
                <tr key={t.id}>
                  <td>{t.fullName}</td>
                  <td>{t.username}</td>
                  <td>{t.employeeNo}</td>
                  <td>{t.email}</td>
                  <td>{t.phone}</td>
                  {isAdmin && (
                    <td className="actions">
                      <button className="secondary" onClick={() => startEdit(t)}>
                        Edit
                      </button>
                      <button className="danger" disabled={remove.busy} onClick={() => handleDelete(t)}>
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

function pick(t: Teacher) {
  return { fullName: t.fullName, email: t.email ?? '', employeeNo: t.employeeNo ?? '', phone: t.phone ?? '' };
}
