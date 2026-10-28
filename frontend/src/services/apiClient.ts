function csrfCookie(): string | null {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : null;
}

export async function refreshCsrf(): Promise<void> {
  const response = await fetch('/api/auth/csrf', { credentials: 'same-origin' });
  if (!response.ok) throw new Error('Could not initialize secure session');
}

export async function apiFetch(url: string, init: RequestInit = {}): Promise<Response> {
  const method = (init.method || 'GET').toUpperCase();
  const headers = new Headers(init.headers);

  if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
    // CSRF token can rotate after login and logout.
    if (!csrfCookie()) await refreshCsrf();
    const token = csrfCookie();
    if (!token) throw new Error('Missing CSRF token. Refresh the page and try again.');
    headers.set('X-XSRF-TOKEN', token);
  }

  return fetch(url, { ...init, headers, credentials: 'same-origin' });
}

export async function readJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const body: unknown = await response.json().catch(() => null);
    let message = `Request failed (${response.status})`;
    if (body && typeof body === 'object' && 'detail' in body) {
      const detail = (body as { detail?: unknown }).detail;
      if (typeof detail === 'string') message = detail;
    }
    if (response.status === 401) message = 'Session expired. Please sign in again.';
    throw new Error(message);
  }
  return response.json() as Promise<T>;
}
