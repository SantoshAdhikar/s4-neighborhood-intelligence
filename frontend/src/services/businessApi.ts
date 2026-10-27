
import type {
  BusinessProfile,
  BusinessProfileRequest,
} from '../types/business';

const BASE_URL = '/api/businesses';

async function handleResponse(response: Response): Promise<BusinessProfile> {
  if (!response.ok) {
    const body: unknown = await response.json().catch(() => null);

    let message = `Request failed (${response.status})`;

    if (body && typeof body === 'object' && 'detail' in body) {
      const detail = (body as { detail?: unknown }).detail;
      if (typeof detail === 'string') message = detail;
    }

    throw new Error(message);
  }

  return response.json() as Promise<BusinessProfile>;
}

export async function createBusiness(
  business: BusinessProfileRequest,
): Promise<BusinessProfile> {
  const response = await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(business),
  });

  return handleResponse(response);
}

export async function getBusiness(id: string): Promise<BusinessProfile> {
  const response = await fetch(`${BASE_URL}/${encodeURIComponent(id)}`);
  return handleResponse(response);
}

export async function updateBusiness(
  id: string,
  business: BusinessProfileRequest,
): Promise<BusinessProfile> {
  const response = await fetch(`${BASE_URL}/${encodeURIComponent(id)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(business),
  });

  return handleResponse(response);
}
