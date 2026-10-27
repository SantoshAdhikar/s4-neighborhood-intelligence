
import BusinessOnboarding from './features/onboarding/BusinessOnboarding';
import NeighborhoodExplorer from './features/neighborhood/NeighborhoodExplorer';

export default function App() {
  const match = window.location.pathname.match(
    /^\/businesses\/([0-9a-fA-F-]{36})\/neighborhood\/?$/,
  );

  return match
    ? <NeighborhoodExplorer businessId={match[1]} />
    : <BusinessOnboarding />;
}
