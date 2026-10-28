import { apiFetch } from './apiClient';

import type { NeighborhoodReport } from '../types/neighborhood';

const requests = new Map<string, Promise<NeighborhoodReport>>();

export function researchNeighborhood(
  businessId: string,
): Promise<NeighborhoodReport> {
  const pending = requests.get(businessId);
  if (pending) return pending;

  const request = apiFetch(
    `/api/businesses/${encodeURIComponent(businessId)}/neighborhood`,
    { method: 'POST' },
  ).then(async (response): Promise<NeighborhoodReport> => {
    if (!response.ok) {
      let message = `Neighborhood research failed (${response.status})`;
      const body: unknown = await response.json().catch(() => null);

      if (body && typeof body === 'object' && 'detail' in body) {
        const detail = (body as { detail?: unknown }).detail;
        if (typeof detail === 'string' && detail.length) {
          message = detail;
        }
      }

      throw new Error(message);
    }

    return response.json() as Promise<NeighborhoodReport>;
  });

  requests.set(businessId, request);

  void request
    .finally(() => requests.delete(businessId))
    .catch(() => undefined);

  return request;
}
