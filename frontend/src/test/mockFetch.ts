/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { vi } from 'vitest';

export interface FetchCall {
  method: string;
  url: string;
  headers: Headers;
  body: unknown;
}

type Handler = (call: FetchCall) => Response | Promise<Response> | undefined;

export function json(status: number, body?: unknown): Response {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

/** Replaces global fetch. Unhandled requests get a 404. Returns the recorded calls. */
export function mockFetch(handler: Handler): FetchCall[] {
  const calls: FetchCall[] = [];
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: string, init: RequestInit = {}) => {
      const call: FetchCall = {
        method: init.method ?? 'GET',
        url: input,
        headers: new Headers(init.headers),
        body: typeof init.body === 'string' ? JSON.parse(init.body) : init.body,
      };
      calls.push(call);
      return (await handler(call)) ?? json(404, { detail: `No mock for ${call.method} ${call.url}` });
    }),
  );
  return calls;
}
