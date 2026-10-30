import { apiFetch, readJson } from './apiClient';

export interface BusinessClaimStatus {
  status: 'UNVERIFIED' | 'PENDING_REVIEW' | 'CONFLICT_REVIEW' | 'NEEDS_RESUBMISSION' | 'VERIFIED' | 'REJECTED';
  explanation: string;
  requestedAt: string | null;
}

export async function getBusinessClaim(businessId: string): Promise<BusinessClaimStatus> {
  return readJson<BusinessClaimStatus>(await apiFetch(`/api/businesses/${businessId}/claim`));
}

export async function requestBusinessClaim(businessId: string): Promise<BusinessClaimStatus> {
  return readJson<BusinessClaimStatus>(await apiFetch(`/api/businesses/${businessId}/claim`, {
    method: 'POST',
  }));
}
