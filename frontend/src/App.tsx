import BusinessOnboarding from './features/onboarding/BusinessOnboarding';
import NeighborhoodExplorer from './features/neighborhood/NeighborhoodExplorer';
import StorefrontInspection from './features/storefront/StorefrontInspection';

export default function App() {
  const storefront = window.location.pathname.match(
    /^\/businesses\/([0-9a-fA-F-]{36})\/storefront\/?$/,
  );
  if (storefront) return <StorefrontInspection businessId={storefront[1]} />;

  const neighborhood = window.location.pathname.match(
    /^\/businesses\/([0-9a-fA-F-]{36})\/neighborhood\/?$/,
  );
  if (neighborhood) return <NeighborhoodExplorer businessId={neighborhood[1]} />;

  return <BusinessOnboarding />;
}
