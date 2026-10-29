import { Link, NavLink } from 'react-router';
import { ArrowLeft, ChevronRight, Sparkles } from 'lucide-react';

type Step = 'profile' | 'neighborhood' | 'storefront' | 'evidence';

export default function BusinessFlowNav({ businessId, current }: { businessId?: string; current: Step }) {
  return (
    <nav className="s4-flow-nav" aria-label="Business workflow">
      <Link to="/dashboard" className="s4-flow-back"><ArrowLeft size={15} /> All businesses</Link>
      {businessId && <Link to={`/businesses/${businessId}/analyze`} className="s4-flow-auto"><Sparkles size={15}/> Run everything together</Link>}
      <div className="s4-flow-steps">
        {businessId ? (
          <>
            <NavLink to={`/businesses/${businessId}/edit`} aria-current={current === 'profile' ? 'step' : undefined}
              className={current === 'profile' ? 'is-current' : ''}>01 Profile</NavLink>
            <ChevronRight size={14} aria-hidden="true" />
            <NavLink to={`/businesses/${businessId}/neighborhood`} aria-current={current === 'neighborhood' ? 'step' : undefined}
              className={current === 'neighborhood' ? 'is-current' : ''}>02 Neighborhood</NavLink>
            <ChevronRight size={14} aria-hidden="true" />
            <NavLink to={`/businesses/${businessId}/storefront`} aria-current={current === 'storefront' ? 'step' : undefined}
              className={current === 'storefront' ? 'is-current' : ''}>03 Visual evidence</NavLink>
            <ChevronRight size={14} aria-hidden="true" />
            <NavLink to={`/businesses/${businessId}/evidence`} aria-current={current === 'evidence' ? 'step' : undefined}
              className={current === 'evidence' ? 'is-current' : ''}>04 Action report</NavLink>
          </>
        ) : (
          <span className="is-current">01 Create business profile</span>
        )}
      </div>
    </nav>
  );
}
