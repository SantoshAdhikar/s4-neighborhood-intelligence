import { apiFetch, readJson } from './apiClient';
import type { AnalysisRequest, AnalysisRun } from '../types/analysis';

function path(businessId: string) {
  return `/api/businesses/${encodeURIComponent(businessId)}/analysis-runs`;
}

export async function startAnalysis(businessId: string, request: AnalysisRequest) {
  return readJson<AnalysisRun>(await apiFetch(path(businessId), {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  }));
}

export async function latestAnalysis(businessId: string) {
  const response = await apiFetch(`${path(businessId)}/latest`);
  return response.status === 204 ? null : readJson<AnalysisRun>(response);
}

export async function readAnalysis(businessId: string, runId: string) {
  return readJson<AnalysisRun>(await apiFetch(`${path(businessId)}/${encodeURIComponent(runId)}`));
}
