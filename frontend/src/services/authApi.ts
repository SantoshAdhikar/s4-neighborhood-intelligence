import { apiFetch, readJson, refreshCsrf } from './apiClient';

export interface Account {
  id: string;
  email: string;
  displayName: string;
  emailVerified: boolean;
}

export async function currentAccount(): Promise<Account | null> {
  const response = await apiFetch('/api/auth/me');
  if (response.status === 401) return null;
  return readJson<Account>(response);
}

export async function registerAccount(values: {
  displayName: string;
  email: string;
  password: string;
}): Promise<Account> {
  return readJson<Account>(await apiFetch('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(values),
  }));
}

export async function loginAccount(email: string, password: string): Promise<Account> {
  const payload = new URLSearchParams({ username: email.trim().toLowerCase(), password });
  const response = await apiFetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: payload,
  });
  if (!response.ok) {
    if (response.status === 401) throw new Error('Incorrect email or password');
    throw new Error(`Sign in failed (${response.status})`);
  }
  await refreshCsrf();
  const account = await currentAccount();
  if (!account) throw new Error('Sign in did not establish a session');
  return account;
}

export async function logoutAccount(): Promise<void> {
  const response = await apiFetch('/api/auth/logout', { method: 'POST' });
  if (!response.ok) throw new Error('Unable to sign out');
  await refreshCsrf();
}

export async function verifyEmail(token: string): Promise<void> {
  const result = await apiFetch('/api/auth/verify-email', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token }),
  });
  if (!result.ok) throw new Error('Verification link is invalid or expired');
}

export async function resendVerification(): Promise<void> {
  const result = await apiFetch('/api/auth/resend-verification', { method: 'POST' });
  if (!result.ok) {
    const message = await result.json().catch(() => null) as { detail?: string } | null;
    throw new Error(message?.detail || 'Verification email unavailable. Check SMTP configuration.');
  }
}

export async function requestPasswordReset(email: string): Promise<void> {
  const result = await apiFetch('/api/auth/forgot-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email }),
  });
  if (!result.ok) {
    const detail = await result.json().catch(() => null) as { detail?: string } | null;
    throw new Error(detail?.detail ?? `Unable to request password reset (${result.status})`);
  }
}

export async function resetPassword(token: string, password: string): Promise<void> {
  const result = await apiFetch('/api/auth/reset-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token, password }),
  });
  if (!result.ok) {
    const detail = await result.json().catch(() => null) as { detail?: string } | null;
    throw new Error(detail?.detail ?? 'Reset link expired or invalid. Request a new link.');
  }
}
