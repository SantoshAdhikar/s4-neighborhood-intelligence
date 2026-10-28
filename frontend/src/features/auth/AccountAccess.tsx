import { useState, type FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router';
import { ArrowRight, LockKeyhole, Radar } from 'lucide-react';
import { registerAccount } from '../../services/authApi';
import { useAuth } from '../../app/useAuth';

export default function AccountAccess() {
  const { account, signIn } = useAuth();
  const navigate = useNavigate();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');

  if (account) return <Navigate to="/dashboard" replace />;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError('');
    try {
      if (mode === 'register') {
        await registerAccount({ displayName, email, password });
        setMode('login');
        setPassword('');
        setInfo('Account created. Sign in to open your dashboard.');
      } else {
        await signIn(email, password);
        navigate('/dashboard', { replace: true });
      }
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to continue');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="s4-auth-screen">
      <section className="s4-auth-side">
        <div className="s4-auth-brand"><Radar size={27} /> S4 Neighborhood Intelligence</div>
        <h1>Understand your neighborhood.<br />Find your next move.</h1>
        <p>One workspace for storefront evidence, local business discovery, and evidence-backed decisions.</p>
        <div className="s4-auth-stage">01 · Business profile<br />02 · Neighborhood explorer<br />03 · Storefront inspection</div>
      </section>
      <section className="s4-auth-panel">
        <div className="s4-auth-card">
          <div className="s4-auth-icon"><LockKeyhole size={21} /></div>
          <h2>{mode === 'login' ? 'Welcome back' : 'Create your account'}</h2>
          <p>{mode === 'login' ? 'Sign in to manage your businesses.' : 'Create an owner account to get started.'}</p>
          <form onSubmit={(event) => void submit(event)}>
            {mode === 'register' && <label>Full name<input autoComplete="name" value={displayName} onChange={(event) => setDisplayName(event.target.value)} maxLength={120} required /></label>}
            <label>Email<input type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} maxLength={254} required /></label>
            <label>Password<input type="password" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} value={password} onChange={(event) => setPassword(event.target.value)} minLength={mode === 'register' ? 12 : undefined} maxLength={72} required /></label>
            {mode === 'register' && <small>Use a unique password between 12 and 72 characters.</small>}
            {error && <div className="s4-form-error" role="alert">{error}</div>}
            {info && <div className="s4-form-info" role="status">{info}</div>}
            <button type="submit" disabled={busy}>{busy ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'} <ArrowRight size={17} /></button>
          </form>
          <p className="s4-auth-toggle">
            {mode === 'login' ? "Don't have an account?" : 'Already have an account?'}{' '}
            <button type="button" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError(''); setInfo(''); setPassword(''); }}>
              {mode === 'login' ? 'Register' : 'Sign in'}
            </button>
          </p>
          <Link className="s4-back-home" to="/">S4 Intelligence</Link>
        </div>
      </section>
    </div>
  );
}
