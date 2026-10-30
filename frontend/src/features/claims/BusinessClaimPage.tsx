import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { MailCheck, ShieldCheck } from 'lucide-react';
import { useAuth } from '../../app/useAuth';
import { resendVerification } from '../../services/authApi';
import { getBusinessClaim, requestBusinessClaim, type BusinessClaimStatus } from '../../services/businessClaimApi';
import { getBusiness } from '../../services/businessApi';

export default function BusinessClaimPage({ businessId }: { businessId: string }) {
  const { account } = useAuth();
  const [businessName, setBusinessName] = useState('');
  const [claim, setClaim] = useState<BusinessClaimStatus | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  useEffect(() => {
    let active = true;
    void Promise.all([getBusiness(businessId), getBusinessClaim(businessId)])
      .then(([business, status]) => {
        if (active) { setBusinessName(business.name); setClaim(status); }
      }).catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : 'Could not load business claim');
      });
    return () => { active = false; };
  }, [businessId]);

  async function request() {
    if (busy) return;
    setBusy(true); setError('');
    try { setClaim(await requestBusinessClaim(businessId)); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Claim request failed'); }
    finally { setBusy(false); }
  }
  async function resend() {
    if (busy) return;
    setBusy(true); setError('');
    try { await resendVerification(); setInfo('If SMTP is configured, a verification link was sent. Check your email.'); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not send verification email'); }
    finally { setBusy(false); }
  }

  return <main className="s4-claim-page">
    <Link to="/dashboard">← Back to dashboard</Link>
    <div className="s4-claim-panel">
      <ShieldCheck size={28} />
      <h1>Business ownership review</h1>
      <h2>{businessName || 'Business workspace'}</h2>
      <p>This business is a private S4 research workspace. Registering its name first does not establish legal ownership or reserve that name.</p>
      <p><strong>Claim status:</strong> {claim?.status.replaceAll('_', ' ') || 'Loading…'}</p>
      {claim && <p>{claim.explanation}</p>}
      <div className="s4-claim-email"><MailCheck size={18} /> <span>Email: {account?.email || 'Unknown'} · {account?.emailVerified ? 'Verified' : 'Not verified'}</span></div>
      {!account?.emailVerified && <button type="button" onClick={() => void resend()} disabled={busy}>Send verification email</button>}
      {account?.emailVerified && claim?.status !== 'VERIFIED' && <button type="button" onClick={() => void request()} disabled={busy}>
        {busy ? 'Sending request…' : claim?.status === 'UNVERIFIED' || claim?.status === 'NEEDS_RESUBMISSION' ? 'Request ownership review' : 'Resubmit claim for review'}
      </button>}
      <p className="s4-claim-warning">A pending or disputed claim is not verified ownership. S4 will not transfer another account's private photos, reports or workspace.</p>
      {error && <p role="alert" className="s4-form-error">{error}</p>}
      {info && <p role="status" className="s4-form-info">{info}</p>}
    </div>
  </main>;
}
