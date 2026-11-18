/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { api } from '../api/client';
import type { Announcement, Audience, Page, SchoolClass } from '../api/types';
import { useUser } from '../auth/AuthContext';
import ErrorBox from '../components/ErrorBox';
import Pager from '../components/Pager';
import { useAction } from '../components/useAction';

const AUDIENCES: Audience[] = ['ALL', 'TEACHERS', 'STUDENTS'];
const EMPTY_FORM = { title: '', body: '', audience: 'ALL' as Audience, classId: '' };

export default function AnnouncementsPage() {
  const user = useUser();
  const canCreate = user.role === 'ADMIN' || user.role === 'TEACHER';
  const [data, setData] = useState<Page<Announcement> | null>(null);
  const [pageNo, setPageNo] = useState(0);
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [loadError, setLoadError] = useState<unknown>(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const create = useAction();
  const remove = useAction();

  const load = useCallback(() => {
    api<Page<Announcement>>(`/announcements?page=${pageNo}&size=10`).then(setData).catch(setLoadError);
  }, [pageNo]);

  useEffect(load, [load]);

  useEffect(() => {
    if (!canCreate) return;
    // A teacher may only target classes they teach.
    api<SchoolClass[]>(user.role === 'TEACHER' ? '/classes?mine=true' : '/classes')
      .then(setClasses)
      .catch(setLoadError);
  }, [canCreate, user.role]);

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    const body = { ...form, classId: form.classId ? Number(form.classId) : null };
    if (await create.run(() => api('/announcements', { method: 'POST', json: body }))) {
      setForm(EMPTY_FORM);
      setPageNo(0);
      load();
    }
  }

  async function handleDelete(a: Announcement) {
    if (!confirm(`Delete announcement "${a.title}"?`)) return;
    if (await remove.run(() => api(`/announcements/${a.id}`, { method: 'DELETE' }))) load();
  }

  return (
    <div>
      <h1>Announcements</h1>

      {canCreate && (
        <div className="card">
          <h2>New announcement</h2>
          <form className="grid" onSubmit={handleCreate}>
            <label style={{ gridColumn: '1 / -1' }}>
              Title
              <input
                value={form.title}
                maxLength={150}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                required
              />
            </label>
            <label style={{ gridColumn: '1 / -1' }}>
              Body
              <textarea
                value={form.body}
                maxLength={5000}
                onChange={(e) => setForm({ ...form, body: e.target.value })}
                required
              />
            </label>
            <label>
              Audience
              <select value={form.audience} onChange={(e) => setForm({ ...form, audience: e.target.value as Audience })}>
                {AUDIENCES.map((a) => (
                  <option key={a}>{a}</option>
                ))}
              </select>
            </label>
            <label>
              Class (optional)
              <select value={form.classId} onChange={(e) => setForm({ ...form, classId: e.target.value })}>
                <option value="">(none)</option>
                {classes.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </label>
            <button type="submit" disabled={create.busy}>
              {create.busy ? 'Posting...' : 'Post'}
            </button>
          </form>
          <ErrorBox error={create.error} />
        </div>
      )}

      <ErrorBox error={loadError || remove.error} />
      {data && (
        <>
          {data.content.map((a) => (
            <div className="card" key={a.id}>
              <h2 style={{ marginTop: 0 }}>{a.title}</h2>
              <div className="muted">
                {a.authorName} | {new Date(a.createdAt).toLocaleString()} | {a.audience}
                {a.className && ` | ${a.className}`}
              </div>
              {/* Plain text only: React escapes it, and pre-wrap keeps the newlines. */}
              <p className="announcement-body">{a.body}</p>
              {(user.role === 'ADMIN' || a.authorId === user.id) && (
                <button className="danger" disabled={remove.busy} onClick={() => handleDelete(a)}>
                  Delete
                </button>
              )}
            </div>
          ))}
          {data.content.length === 0 && <p className="muted">No announcements.</p>}
          <Pager page={data.page} onChange={setPageNo} />
        </>
      )}
    </div>
  );
}
