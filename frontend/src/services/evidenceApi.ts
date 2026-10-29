import { apiFetch, readJson } from './apiClient';
import type { EvidenceReport } from '../types/evidence';

function url(businessId: string) {
  return `/api/businesses/${encodeURIComponent(businessId)}/evidence-report`;
}

export async function latestEvidenceReport(businessId: string): Promise<EvidenceReport | null> {
  const response = await apiFetch(url(businessId));
  if (response.status === 204) return null;
  return readJson<EvidenceReport>(response);
}

export async function generateEvidenceReport(businessId: string): Promise<EvidenceReport> {
  return readJson<EvidenceReport>(await apiFetch(url(businessId), { method: 'POST' }));
}

export async function readEvidenceReport(businessId: string, reportId: string): Promise<EvidenceReport> {
  return readJson<EvidenceReport>(await apiFetch(`${url(businessId)}/${encodeURIComponent(reportId)}`));
}
