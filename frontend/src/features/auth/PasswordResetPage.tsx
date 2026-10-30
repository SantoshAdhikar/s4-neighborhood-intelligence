import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { ArrowLeft, ArrowRight, Eye, EyeOff, KeyRound, MailCheck } from 'lucide-react';
import { requestPasswordReset, resetPassword } from '../../services/authApi';

function PasswordInput({ value, onChange, label, id, visible, onVisibilityChange, autoComplete }: {
  value: string;
  onChange: (value: string) => void;
  label: string;
  id: string;
  visible: boolean;
  onVisibilityChange: () => void;
  autoComplete: string;
}) {
  return <label htmlFor={id}>{label}<span className="s4-password-wrap">
    <input id={id} type={visible ? 'text' : 'password'} autoComplete={autoComplete}
      value={value} onChange={event => onChange(event.target.value)} required minLength={12} maxLength={72} />
    <button type="button" className="s4-password-eye" aria-label={visible ? 'Hide password' : 'Show password'}
      aria-pressed={visible} onClick={onVisibilityChange}>{visible ? <EyeOff size={18} /> : <Eye size={18} />}</button>
  </span></label>;
}

export default function PasswordResetPage() {
  const navigate = useNavigate();
  const [token] = useState(() => new URLSearchParams(window.location.search).get('token'));
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [visible, setVisible] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [sent, setSent] = useState(false);
  const [complete, setComplete] = useState(false);
  const isMatch = confirm.length > 0 && password === confirm;

  // Keep one-time tokens out of browser history, screenshots and referer URLs.
  useEffect(() => {
    if (token) window.history.replaceState(null, '', '/reset-password');
  }, [token]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    setError('');
    if (token && (!isMatch || password.length < 12)) {
      setError('Passwords must match and contain at least 12 characters.');
      return;
    }
    setBusy(true);
    try {
      if (token) {
        await resetPassword(token, password);
        setComplete(true);
        setPassword('');
        setConfirm('');
      } else {
        await requestPasswordReset(email);
        setSent(true);
      }
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not complete this request');
    } finally {
      setBusy(false);
    }
  }

  return <main className="s4-auth-screen s4-reset-screen">
    <section className="s4-auth-side">
      <div className="s4-auth-brand"><span className="s4-brand-icon"><img src="/s4-logo.png" alt="" /></span> S4 Neighborhood Intelligence</div>
      <div className="s4-auth-stage-chip">ACCOUNT SECURITY</div>
      <h1>Secure your next move.</h1>
      <p>Keep your business insights and private research in the right hands.</p>
      <div className="s4-auth-stage">Protected accounts · Private workspaces · Evidence you can trust</div>
    </section>
    <section className="s4-auth-panel"><div className="s4-auth-card s4-animate-in">
      <div className="s4-auth-icon">{sent || complete ? <MailCheck size={23}/> : <KeyRound size={23}/>}</div>
      <h2>{complete ? 'Password updated' : sent ? 'Check your inbox' : token ? 'Set a new password' : 'Forgot password?'}</h2>
      <p>{complete ? 'Your new password is ready. Sign in to continue.' : sent
        ? 'If that email is registered, a secure reset link will arrive shortly. Check spam if needed.'
        : token ? 'Choose a new password for your S4 account.' : 'Enter your account email and we will send a secure reset link.'}</p>
      {!complete && !sent && <form onSubmit={event => void submit(event)}>
        {!token ? <label htmlFor="reset-email">Email address<input id="reset-email" type="email" autoComplete="email"
          value={email} onChange={event => setEmail(event.target.value)} required maxLength={254} /></label> : <>
          <PasswordInput id="reset-new-password" label="New password" value={password} onChange={setPassword}
            visible={visible} onVisibilityChange={() => setVisible(v => !v)} autoComplete="new-password" />
          <div className={`s4-password-match ${confirm ? isMatch ? 'is-valid' : 'is-invalid' : ''}`}>
            <PasswordInput id="reset-confirm-password" label="Confirm password" value={confirm} onChange={setConfirm}
              visible={visible} onVisibilityChange={() => setVisible(v => !v)} autoComplete="new-password" />
            {confirm && <small aria-live="polite">{isMatch ? 'Passwords match' : 'Passwords do not match'}</small>}
          </div>
        </>}
        {error && <p className="s4-form-error" role="alert">{error}</p>}
        <button type="submit" disabled={busy || Boolean(token && !isMatch)}>{busy ? 'Working…' : token ? 'Update password' : 'Send reset link'} <ArrowRight size={17}/></button>
      </form>}
      {(sent || complete) && <button type="button" className="s4-auth-main-link" onClick={() => navigate('/login')}>Back to sign in <ArrowRight size={16}/></button>}
      {!sent && !complete && <Link className="s4-auth-return" to="/login"><ArrowLeft size={15}/> Back to sign in</Link>}
      {token && !complete && <p className="s4-auth-footnote">Reset links expire in 30 minutes and work once.</p>}
    </div></section>
  </main>;
}
