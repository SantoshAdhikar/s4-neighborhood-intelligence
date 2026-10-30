import { BrowserRouter, Navigate, Route, Routes, useParams } from 'react-router';
import { AuthProvider } from './app/AuthContext';
import { useAuth } from './app/useAuth';
import AppShell from './app/AppShell';
import AccountAccess from './features/auth/AccountAccess';
import PasswordResetPage from './features/auth/PasswordResetPage';
import GlassCursor from './components/GlassCursor';
import VerifyEmailPage from './features/auth/VerifyEmailPage';
import BusinessClaimPage from './features/claims/BusinessClaimPage';
import BusinessDashboard from './features/dashboard/BusinessDashboard';
import BusinessOnboarding from './features/onboarding/BusinessOnboarding';
import NeighborhoodExplorer from './features/neighborhood/NeighborhoodExplorer';
import StorefrontInspection from './features/storefront/StorefrontInspection';
import EvidenceReportPage from './features/evidence/EvidenceReportPage';
import UnifiedAnalysisPage from './features/analysis/UnifiedAnalysisPage';
import './styles/workspace.css';
import './styles/glass.css';

function ProtectedWorkspace() {
  const { account, loading } = useAuth();
  if (loading) return <div className="s4-loading">Loading your S4 workspace…</div>;
  if (!account) return <Navigate to="/login" replace />;
  return <AppShell />;
}

function NeighborhoodRoute() {
  const { businessId } = useParams();
  return businessId ? <NeighborhoodExplorer businessId={businessId} /> : <Navigate to="/dashboard" />;
}

function AnalysisRoute() {
  const { businessId } = useParams();
  return businessId ? <UnifiedAnalysisPage businessId={businessId} /> : <Navigate to="/dashboard" />;
}

function ClaimRoute() {
  const { businessId } = useParams();
  return businessId ? <BusinessClaimPage businessId={businessId} /> : <Navigate to="/dashboard" />;
}

function EvidenceRoute() {
  const { businessId } = useParams();
  return businessId ? <EvidenceReportPage businessId={businessId} /> : <Navigate to="/dashboard" />;
}

function StorefrontRoute() {
  const { businessId } = useParams();
  return businessId ? <StorefrontInspection businessId={businessId} /> : <Navigate to="/dashboard" />;
}

export default function App() {
  return (
    <BrowserRouter>
      <GlassCursor />
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<AccountAccess />} />
          <Route path="/forgot-password" element={<PasswordResetPage />} />
          <Route path="/reset-password" element={<PasswordResetPage />} />
          <Route path="/verify-email" element={<VerifyEmailPage />} />
          <Route element={<ProtectedWorkspace />}>
            <Route path="/dashboard" element={<BusinessDashboard />} />
            <Route path="/businesses/new" element={<BusinessOnboarding />} />
            <Route path="/businesses/:businessId/edit" element={<BusinessOnboarding />} />
            <Route path="/businesses/:businessId/claim" element={<ClaimRoute />} />
            <Route path="/businesses/:businessId/neighborhood" element={<NeighborhoodRoute />} />
            <Route path="/businesses/:businessId/storefront" element={<StorefrontRoute />} />
            <Route path="/businesses/:businessId/evidence" element={<EvidenceRoute />} />
            <Route path="/businesses/:businessId/analyze" element={<AnalysisRoute />} />
          </Route>
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
