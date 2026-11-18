/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { useState } from 'react';

/** Tracks busy/error state for a form submit or button action. `run` returns true on success. */
export function useAction() {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  async function run(fn: () => Promise<unknown>): Promise<boolean> {
    setBusy(true);
    setError(null);
    try {
      await fn();
      return true;
    } catch (e) {
      setError(e);
      return false;
    } finally {
      setBusy(false);
    }
  }

  return { busy, error, setError, run };
}
