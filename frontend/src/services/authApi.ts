import { apiFetch, readJson, refreshCsrf } from './apiClient';

export interface Account {
  id: string;
  email: string;
  displayName: string;
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
