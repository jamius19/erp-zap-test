/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth, useUser } from '../auth/AuthContext';
import ErrorBox from '../components/ErrorBox';
import Photo from '../components/Photo';
import { useAction } from '../components/useAction';

const ALLOWED_TYPES = ['image/png', 'image/jpeg'];
const MAX_BYTES = 2 * 1024 * 1024;

export default function ProfilePage() {
  const user = useUser();
  const { reloadUser, logout } = useAuth();
  const navigate = useNavigate();
  const [file, setFile] = useState<File | null>(null);
  const [photoVersion, setPhotoVersion] = useState(0);
  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '' });
  const upload = useAction();
  const changePw = useAction();

  async function handleUpload(e: FormEvent) {
    e.preventDefault();
    if (!file) return;
    if (!ALLOWED_TYPES.includes(file.type)) {
      upload.setError(new Error('Only PNG or JPEG images are allowed.'));
      return;
    }
    if (file.size > MAX_BYTES) {
      upload.setError(new Error('The file must be 2MB or smaller.'));
      return;
    }
    const body = new FormData();
    body.append('file', file);
    const ok = await upload.run(async () => {
      await api('/users/me/photo', { method: 'POST', body });
      await reloadUser();
    });
    if (ok) {
      setFile(null);
      setPhotoVersion((v) => v + 1);
    }
  }

  async function handleChangePassword(e: FormEvent) {
    e.preventDefault();
    const ok = await changePw.run(() => api('/auth/change-password', { method: 'POST', json: passwords }));
    if (ok) {
      // The backend revokes all refresh tokens, so sign in again with the new password.
      await logout();
      navigate('/login');
    }
  }

  return (
    <div>
      <h1>Profile</h1>
      <div className="card" style={{ display: 'flex', gap: 20, alignItems: 'center' }}>
        {user.hasPhoto ? (
          <Photo userId={user.id} version={photoVersion} />
        ) : (
          <div className="photo" style={{ width: 96, height: 96 }} />
        )}
        <table style={{ width: 'auto' }}>
          <tbody>
            <tr>
              <th>Username</th>
              <td>{user.username}</td>
            </tr>
            <tr>
              <th>Full name</th>
              <td>{user.fullName}</td>
            </tr>
            <tr>
              <th>Email</th>
              <td>{user.email}</td>
            </tr>
            <tr>
              <th>Role</th>
              <td>{user.role}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div className="card">
        <h2 style={{ marginTop: 0 }}>Upload photo</h2>
        <form className="toolbar" onSubmit={handleUpload}>
          <input
            type="file"
            accept="image/png,image/jpeg"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          />
          <button type="submit" disabled={upload.busy || !file}>
            {upload.busy ? 'Uploading...' : 'Upload'}
          </button>
        </form>
        <ErrorBox error={upload.error} />
      </div>

      <div className="card">
        <h2 style={{ marginTop: 0 }}>Change password</h2>
        <form className="grid" onSubmit={handleChangePassword}>
          <label>
            Current password
            <input
              type="password"
              autoComplete="current-password"
              value={passwords.currentPassword}
              onChange={(e) => setPasswords({ ...passwords, currentPassword: e.target.value })}
              required
            />
          </label>
          <label>
            New password
            <input
              type="password"
              autoComplete="new-password"
              value={passwords.newPassword}
              onChange={(e) => setPasswords({ ...passwords, newPassword: e.target.value })}
              required
            />
          </label>
          <button type="submit" disabled={changePw.busy}>
            {changePw.busy ? 'Saving...' : 'Change password'}
          </button>
        </form>
        <p className="muted">Min 8 characters, with at least one letter and one digit. You will be signed out after.</p>
        <ErrorBox error={changePw.error} />
      </div>
    </div>
  );
}
