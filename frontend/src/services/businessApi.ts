import { apiFetch, readJson } from './apiClient';

import type {
  BusinessProfile,
  BusinessProfileRequest,
} from '../types/business';

const BASE_URL = '/api/businesses';

async function handleResponse(response: Response): Promise<BusinessProfile> {
  return readJson<BusinessProfile>(response);
}

export async function createBusiness(
  business: BusinessProfileRequest,
): Promise<BusinessProfile> {
  const response = await apiFetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(business),
  });

  return handleResponse(response);
}

export async function getBusiness(id: string): Promise<BusinessProfile> {
  const response = await apiFetch(`${BASE_URL}/${encodeURIComponent(id)}`);
  return handleResponse(response);
}

export async function updateBusiness(
  id: string,
  business: BusinessProfileRequest,
): Promise<BusinessProfile> {
  const response = await apiFetch(`${BASE_URL}/${encodeURIComponent(id)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(business),
  });

  return handleResponse(response);
}

export async function listBusinesses(): Promise<BusinessProfile[]> {
  return readJson<BusinessProfile[]>(await apiFetch(BASE_URL));
}
