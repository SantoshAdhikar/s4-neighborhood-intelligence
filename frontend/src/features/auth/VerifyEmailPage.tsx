import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router';
import { MailCheck } from 'lucide-react';
import { verifyEmail } from '../../services/authApi';

export default function VerifyEmailPage() {
  const [status, setStatus] = useState('Verifying your email address…');
  const [complete, setComplete] = useState(false);
  const [token] = useState(() => new URLSearchParams(window.location.search).get('token'));
  const operation = useRef<Promise<void> | null>(null);
  useEffect(() => {
    window.history.replaceState(null, '', '/verify-email');
    if (!token) {
      queueMicrotask(() => setStatus('This verification link is missing its token.'));
      return;
    }
    let active = true;
    if (!operation.current) operation.current = verifyEmail(token);
    void operation.current.then(() => {
      if (active) { setComplete(true); setStatus('Email verified. You can sign in to S4.'); }
    }).catch((cause: unknown) => {
      if (active) setStatus(cause instanceof Error ? cause.message : 'Email verification failed');
    });
    return () => { active = false; };
  }, [token]);
  return <main className="s4-auth-screen s4-verify-email">
    <div className="s4-auth-card">
      <MailCheck size={34} aria-hidden="true" />
      <h1>Email verification</h1>
      <p role="status">{status}</p>
      <p>{complete ? 'This verifies your email only. Business ownership is a separate review.' : 'Verification links are single-use and expire after 24 hours.'}</p>
      <Link to="/login">Back to sign in</Link>
    </div>
  </main>;
}
