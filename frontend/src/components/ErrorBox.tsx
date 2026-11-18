/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { ApiError } from '../api/client';

/** Shows a ProblemDetail error: the `detail` text plus any field `errors`. */
export default function ErrorBox({ error }: { error: unknown }) {
  if (!error) return null;
  const message = error instanceof Error ? error.message : String(error);
  const fieldErrors = error instanceof ApiError ? Object.entries(error.errors) : [];
  return (
    <div className="error" role="alert">
      <div>{message}</div>
      {fieldErrors.length > 0 && (
        <ul>
          {fieldErrors.map(([field, msg]) => (
            <li key={field}>
              <b>{field}</b>: {msg}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
