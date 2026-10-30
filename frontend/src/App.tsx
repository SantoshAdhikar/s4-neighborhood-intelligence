import { lazy, Suspense, type ReactNode } from 'react';
import { BrowserRouter, Navigate, Route, Routes, useLocation, useParams } from 'react-router';
import { AuthProvider } from './app/AuthContext';
import { useAuth } from './app/useAuth';
import AppShell from './app/AppShell';
import GlassCursor from './components/GlassCursor';
import './styles/workspace.css';
import './styles/glass.css';

const AccountAccess = lazy(() => import('./features/auth/AccountAccess'));
const PasswordResetPage = lazy(() => import('./features/auth/PasswordResetPage'));
const VerifyEmailPage = lazy(() => import('./features/auth/VerifyEmailPage'));
const BusinessClaimPage = lazy(() => import('./features/claims/BusinessClaimPage'));
const BusinessDashboard = lazy(() => import('./features/dashboard/BusinessDashboard'));
const BusinessOnboarding = lazy(() => import('./features/onboarding/BusinessOnboarding'));
const NeighborhoodExplorer = lazy(() => import('./features/neighborhood/NeighborhoodExplorer'));
const StorefrontInspection = lazy(() => import('./features/storefront/StorefrontInspection'));
const EvidenceReportPage = lazy(() => import('./features/evidence/EvidenceReportPage'));
const UnifiedAnalysisPage = lazy(() => import('./features/analysis/UnifiedAnalysisPage'));

function DeferredPage({ children }: { children: ReactNode }) {
  return <Suspense fallback={<div className="s4-loading" role="status">Loading page…</div>}>{children}</Suspense>;
}

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
  const { search } = useLocation();
  // A distinct history URL must load its matching saved run and report.
  return businessId ? <UnifiedAnalysisPage key={`${businessId}:${search}`} businessId={businessId} /> : <Navigate to="/dashboard" />;
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
          <Route path="/login" element={<DeferredPage><AccountAccess /></DeferredPage>} />
          <Route path="/forgot-password" element={<DeferredPage><PasswordResetPage /></DeferredPage>} />
          <Route path="/reset-password" element={<DeferredPage><PasswordResetPage /></DeferredPage>} />
          <Route path="/verify-email" element={<DeferredPage><VerifyEmailPage /></DeferredPage>} />
          <Route element={<ProtectedWorkspace />}>
            <Route path="/dashboard" element={<DeferredPage><BusinessDashboard /></DeferredPage>} />
            <Route path="/businesses/new" element={<DeferredPage><BusinessOnboarding /></DeferredPage>} />
            <Route path="/businesses/:businessId/edit" element={<DeferredPage><BusinessOnboarding /></DeferredPage>} />
            <Route path="/businesses/:businessId/claim" element={<DeferredPage><ClaimRoute /></DeferredPage>} />
            <Route path="/businesses/:businessId/neighborhood" element={<DeferredPage><NeighborhoodRoute /></DeferredPage>} />
            <Route path="/businesses/:businessId/storefront" element={<DeferredPage><StorefrontRoute /></DeferredPage>} />
            <Route path="/businesses/:businessId/evidence" element={<DeferredPage><EvidenceRoute /></DeferredPage>} />
            <Route path="/businesses/:businessId/analyze" element={<DeferredPage><AnalysisRoute /></DeferredPage>} />
          </Route>
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
