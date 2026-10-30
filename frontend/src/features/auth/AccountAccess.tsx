import { useState, type FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router';
import { ArrowRight, Eye, EyeOff, LockKeyhole } from 'lucide-react';
import { registerAccount } from '../../services/authApi';
import { useAuth } from '../../app/useAuth';
import S4LogoAnimation from '../../components/S4LogoAnimation';

export default function AccountAccess() {
  const { account, signIn } = useAuth();
  const navigate = useNavigate();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const matches = confirmPassword.length > 0 && password === confirmPassword;
  const passwordLengthValid = password.length >= 12 && password.length <= 72;
  const showMatch = mode === 'register' && confirmPassword.length > 0;
  const registrationReady = displayName.trim().length > 0 && email.trim().length > 0 && passwordLengthValid && matches;

  if (account) return <Navigate to="/dashboard" replace />;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    if (mode === 'register' && !registrationReady) {
      setError('Use at least 12 characters and make sure both passwords match.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      if (mode === 'register') {
        await registerAccount({ displayName, email, password });
        setMode('login');
        setPassword('');
        setConfirmPassword('');
        setInfo('Account created. Sign in to continue. Verify your email before requesting ownership review.');
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

  function switchMode() {
    setMode(mode === 'login' ? 'register' : 'login');
    setError('');
    setInfo('');
    setPassword('');
    setConfirmPassword('');
    setShowPassword(false);
  }

  return <main className="s4-auth-screen">
    <section className="s4-auth-side">
      <div className="s4-auth-brand"><span className="s4-auth-brand-mark"><S4LogoAnimation maxWidth={56} tilt={false} /></span><span>S4 Neighborhood Intelligence</span></div>
      <div className="s4-auth-visual"><S4LogoAnimation maxWidth={340} tilt={false} /></div>
      <h1>Know your neighborhood. Own your next move.</h1>
      <p>Real local evidence. Clear actions. A smarter workspace for every small business.</p>

    </section>
    <section className="s4-auth-panel"><div className="s4-auth-card s4-animate-in">
      <div className="s4-auth-icon"><LockKeyhole size={22}/></div>
      <h2>{mode === 'login' ? 'Welcome back' : 'Create your workspace'}</h2>
      <p>{mode === 'login' ? 'Sign in to your private business workspace.' : 'Start researching your neighborhood in minutes.'}</p>
      <form onSubmit={event => void submit(event)}>
        {mode === 'register' && <label htmlFor="s4-display-name">Full name<input id="s4-display-name" autoComplete="name" value={displayName} onChange={event => setDisplayName(event.target.value)} maxLength={120} required /></label>}
        <label htmlFor="s4-email">Email address<input id="s4-email" type="email" autoComplete="email" value={email} onChange={event => setEmail(event.target.value)} maxLength={254} required /></label>
        <label htmlFor="s4-password">Password
          <span className="s4-password-wrap"><input id="s4-password" type={showPassword ? 'text' : 'password'} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} value={password} onChange={event => setPassword(event.target.value)} minLength={mode === 'register' ? 12 : undefined} maxLength={72} required />
            <button type="button" className="s4-password-eye" onClick={() => setShowPassword(value => !value)} aria-label={showPassword ? 'Hide password' : 'Show password'} aria-pressed={showPassword}>
              {showPassword ? <EyeOff size={18}/> : <Eye size={18}/>}
            </button></span>
        </label>
        {mode === 'register' && <>
          <div className={`s4-password-match ${showMatch ? matches && passwordLengthValid ? 'is-valid' : 'is-invalid' : ''}`}>
            <label htmlFor="s4-confirm-password">Confirm password
              <span className="s4-password-wrap"><input id="s4-confirm-password" type={showPassword ? 'text' : 'password'} autoComplete="new-password" value={confirmPassword} onChange={event => setConfirmPassword(event.target.value)} minLength={12} maxLength={72} required aria-invalid={showMatch && (!matches || !passwordLengthValid)} />
                <button type="button" className="s4-password-eye" onClick={() => setShowPassword(value => !value)} aria-label={showPassword ? 'Hide passwords' : 'Show passwords'} aria-pressed={showPassword}>{showPassword ? <EyeOff size={18}/> : <Eye size={18}/>}</button>
              </span>
            </label>
            {showMatch && <small aria-live="polite">{matches && passwordLengthValid ? 'Passwords match ✓' : !passwordLengthValid ? 'Password must be 12–72 characters' : 'Passwords do not match ✕'}</small>}
          </div>
          <small className="s4-password-hint">Use 12–72 characters and a unique password.</small>
        </>}
        {mode === 'login' && <div className="s4-forgot-row"><Link to="/forgot-password">Forgot password?</Link></div>}
        {error && <div className="s4-form-error" role="alert">{error}</div>}
        {info && <div className="s4-form-info" role="status">{info}</div>}
        <button type="submit" disabled={busy || (mode === 'register' && !registrationReady)}>{busy ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'} <ArrowRight size={17}/></button>
      </form>
      <p className="s4-auth-toggle">{mode === 'login' ? 'New to S4?' : 'Have an account?'}{' '}
        <button type="button" onClick={switchMode}>{mode === 'login' ? 'Create an account' : 'Sign in'}</button>
      </p>
      <span className="s4-auth-footnote">Protected accounts. Private business data.</span>
    </div></section>
  </main>;
}
