/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { useEffect, useState } from 'react';
import { apiRequest } from '../api/client';

interface Props {
  userId: number;
  /** Change this value to force a reload (e.g. after uploading a new photo). */
  version?: number;
  size?: number;
}

/**
 * GET /api/users/{id}/photo needs a bearer token, so a plain <img src> cannot load it.
 * We fetch it as a blob and show it through an object URL.
 */
export default function Photo({ userId, version = 0, size = 96 }: Props) {
  const [url, setUrl] = useState<string | null>(null);

  useEffect(() => {
    let objectUrl: string | null = null;
    let cancelled = false;
    apiRequest(`/users/${userId}/photo`)
      .then((res) => res.blob())
      .then((blob) => {
        if (cancelled) return;
        objectUrl = URL.createObjectURL(blob);
        setUrl(objectUrl);
      })
      .catch(() => setUrl(null));
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [userId, version]);

  if (!url) return <div className="photo placeholder" style={{ width: size, height: size }} />;
  return <img className="photo" src={url} alt="Profile" width={size} height={size} />;
}
