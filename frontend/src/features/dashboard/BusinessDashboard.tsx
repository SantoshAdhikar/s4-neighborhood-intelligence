import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { Sparkles, ArrowRight, Building2, MapPinned, Plus, ScanEye, ClipboardList, ShieldCheck } from 'lucide-react';
import { listBusinesses } from '../../services/businessApi';
import { useAuth } from '../../app/useAuth';
import type { BusinessProfile } from '../../types/business';

export default function BusinessDashboard() {
  const { account } = useAuth();
  const [businesses, setBusinesses] = useState<BusinessProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    void listBusinesses().then((result) => {
      if (active) setBusinesses(result);
    }).catch((cause: unknown) => {
      if (active) setError(cause instanceof Error ? cause.message : 'Unable to load businesses');
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  return (
    <section className="s4-dashboard">
      <header className="s4-dashboard-head">
        <div><p className="s4-kicker">OWNER WORKSPACE</p><h1>Welcome, {account?.displayName}</h1><p>Your business, neighborhood research and visual evidence in one place.</p></div>
        {!loading && !error && businesses.length === 0 && <Link className="s4-primary-link" to="/businesses/new"><Plus size={17} /> Set up your business</Link>}
      </header>
      {account && !account.emailVerified && <div className="s4-claim-warning" role="status">Your account email is not verified. You can research your businesses, but ownership claims require email verification.</div>}
      {loading && <p>Loading your businesses…</p>}
      {error && <div className="s4-form-error" role="alert">{error}</div>}
      {!loading && !error && businesses.length === 0 && (
        <div className="s4-empty-state"><Building2 size={32} /><h2>No businesses linked yet</h2><p>Create your business workspace to start neighborhood research.</p><Link to="/businesses/new">Set up your business <ArrowRight size={17} /></Link></div>
      )}
      <div className="s4-business-grid">
        {businesses.map((business) => (
          <article className="s4-business-card" key={business.id}>
            <span className="s4-category">{business.industry}</span>
            <h2>{business.name}</h2>
            <p>{business.city}, {business.state} · {business.researchRadiusMiles}-mile radius</p>
            <div className="s4-card-actions">
              <Link className="s4-primary-analysis-link" to={`/businesses/${business.id}/analyze`}><Sparkles size={17} /> Start complete analysis <ArrowRight size={15} /></Link>
              <Link to={`/businesses/${business.id}/neighborhood`}><MapPinned size={17} /> Neighborhood <ArrowRight size={15} /></Link>
              <Link to={`/businesses/${business.id}/storefront`}><ScanEye size={17} /> Visual inspection <ArrowRight size={15} /></Link>
              <Link to={`/businesses/${business.id}/evidence`}><ClipboardList size={17} /> Growth report <ArrowRight size={15} /></Link>
              <Link to={`/businesses/${business.id}/edit`}><Building2 size={17} /> Edit profile <ArrowRight size={15} /></Link>
              <Link to={`/businesses/${business.id}/claim`}><ShieldCheck size={17} /> Business ownership review <ArrowRight size={15} /></Link>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
