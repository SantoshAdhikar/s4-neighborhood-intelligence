import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { ArrowRight, Building2, MapPinned, Plus, ScanEye } from 'lucide-react';
import { listBusinesses } from '../../services/businessApi';
import { useAuth } from '../../app/AuthContext';
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
        <div><p className="s4-kicker">OWNER WORKSPACE</p><h1>Welcome, {account?.displayName}</h1><p>Your businesses, neighborhood research and visual evidence in one place.</p></div>
        <Link className="s4-primary-link" to="/businesses/new"><Plus size={17} /> Add business</Link>
      </header>
      <div className="s4-count-card"><Building2 size={21} /> <strong>{businesses.length}</strong><span>Businesses in your workspace</span></div>
      {loading && <p>Loading your businesses…</p>}
      {error && <div className="s4-form-error" role="alert">{error}</div>}
      {!loading && !error && businesses.length === 0 && (
        <div className="s4-empty-state"><Building2 size={32} /><h2>No businesses linked yet</h2><p>Create a new business profile or follow the migration instructions to assign your previously saved business.</p><Link to="/businesses/new">Create a business <ArrowRight size={17} /></Link></div>
      )}
      <div className="s4-business-grid">
        {businesses.map((business) => (
          <article className="s4-business-card" key={business.id}>
            <span className="s4-category">{business.industry}</span>
            <h2>{business.name}</h2>
            <p>{business.city}, {business.state} · {business.researchRadiusMiles}-mile radius</p>
            <div className="s4-card-actions">
              <Link to={`/businesses/${business.id}/neighborhood`}><MapPinned size={17} /> Neighborhood <ArrowRight size={15} /></Link>
              <Link to={`/businesses/${business.id}/storefront`}><ScanEye size={17} /> Visual inspection <ArrowRight size={15} /></Link>
              <Link to={`/businesses/${business.id}/edit`}><Building2 size={17} /> Edit profile <ArrowRight size={15} /></Link>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
