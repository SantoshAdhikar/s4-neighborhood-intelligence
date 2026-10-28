import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { currentAccount, loginAccount, logoutAccount, type Account } from '../services/authApi';
import { AuthContext, type AuthValue } from './authState';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [account, setAccount] = useState<Account | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    void currentAccount().then((result) => {
      if (active) setAccount(result);
    }).catch(() => {
      if (active) setAccount(null);
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, []);

  const value = useMemo<AuthValue>(() => ({
    account,
    loading,
    async signIn(email, password) { setAccount(await loginAccount(email, password)); },
    async signOut() { await logoutAccount(); setAccount(null); },
  }), [account, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
