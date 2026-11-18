/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import type { AuthResponse } from './types';

// Access token lives in memory only. It is never written to localStorage.
let accessToken: string | null = null;
let refreshPromise: Promise<AuthResponse | null> | null = null;
let authFailureHandler: (() => void) | null = null;

// These endpoints must not trigger the refresh-on-401 flow.
const NO_REFRESH_PATHS = ['/auth/login', '/auth/refresh', '/auth/logout'];

export class ApiError extends Error {
  status: number;
  errors: Record<string, string>;

  constructor(status: number, message: string, errors: Record<string, string> = {}) {
    super(message);
    this.status = status;
    this.errors = errors;
  }
}

export function getAccessToken() {
  return accessToken;
}

export function setAccessToken(token: string | null) {
  accessToken = token;
}

/** Called when the session cannot be restored (refresh failed after a 401). */
export function setAuthFailureHandler(handler: (() => void) | null) {
  authFailureHandler = handler;
}

export interface RequestOptions {
  method?: string;
  json?: unknown;
  body?: BodyInit;
}

function send(path: string, opts: RequestOptions) {
  const headers = new Headers();
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);
  let body = opts.body;
  if (opts.json !== undefined) {
    headers.set('Content-Type', 'application/json');
    body = JSON.stringify(opts.json);
  }
  return fetch('/api' + path, { method: opts.method ?? 'GET', headers, body, credentials: 'include' });
}

async function toApiError(res: Response): Promise<ApiError> {
  let detail = `Request failed (${res.status})`;
  let errors: Record<string, string> = {};
  try {
    const problem = await res.json();
    detail = problem.detail || problem.title || detail;
    errors = problem.errors || {};
  } catch {
    // Body was not JSON; keep the generic message.
  }
  return new ApiError(res.status, detail, errors);
}

/** Calls /auth/refresh. Parallel callers share the same in-flight request. */
export function refreshSession(): Promise<AuthResponse | null> {
  if (!refreshPromise) {
    refreshPromise = fetch('/api/auth/refresh', { method: 'POST', credentials: 'include' })
      .then(async (res) => {
        if (!res.ok) {
          accessToken = null;
          return null;
        }
        const data: AuthResponse = await res.json();
        accessToken = data.accessToken;
        return data;
      })
      .catch(() => {
        accessToken = null;
        return null;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

/** Sends a request; on 401 refreshes once and retries. Throws ApiError on non-2xx. */
export async function apiRequest(path: string, opts: RequestOptions = {}): Promise<Response> {
  let res = await send(path, opts);
  if (res.status === 401 && !NO_REFRESH_PATHS.includes(path)) {
    const refreshed = await refreshSession();
    if (!refreshed) {
      authFailureHandler?.();
      throw await toApiError(res);
    }
    res = await send(path, opts);
  }
  if (!res.ok) throw await toApiError(res);
  return res;
}

/** Like apiRequest but parses the JSON body (undefined for empty responses such as 204). */
export async function api<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const res = await apiRequest(path, opts);
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export async function login(username: string, password: string): Promise<AuthResponse> {
  const data = await api<AuthResponse>('/auth/login', { method: 'POST', json: { username, password } });
  accessToken = data.accessToken;
  return data;
}

export async function logout(): Promise<void> {
  try {
    await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' });
  } catch {
    // Network error: the local session is cleared below anyway.
  }
  accessToken = null;
}
