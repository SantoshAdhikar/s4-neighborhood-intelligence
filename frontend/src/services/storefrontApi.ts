import type { StorefrontMedia } from '../types/storefront';

function path(businessId: string) {
  return `/api/businesses/${encodeURIComponent(businessId)}/storefront-media`;
}

async function readResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const data: unknown = await response.json().catch(() => null);
    if (data && typeof data === 'object' && 'detail' in data) {
      const detail = (data as { detail?: unknown }).detail;
      if (typeof detail === 'string') throw new Error(detail);
    }
    throw new Error(`Storefront request failed (HTTP ${response.status})`);
  }
  return response.json() as Promise<T>;
}

export function storefrontFileUrl(businessId: string, mediaId: string) {
  return `${path(businessId)}/${encodeURIComponent(mediaId)}/file`;
}

export async function listStorefrontMedia(businessId: string) {
  return readResponse<StorefrontMedia[]>(await fetch(path(businessId)));
}

export async function uploadStorefrontMedia(businessId: string, file: File) {
  const body = new FormData();
  body.append('file', file);
  return readResponse<StorefrontMedia>(await fetch(path(businessId), {
    method: 'POST',
    body,
  }));
}

export async function analyzeStorefrontMedia(businessId: string, mediaId: string) {
  return readResponse<StorefrontMedia>(await fetch(
    `${path(businessId)}/${encodeURIComponent(mediaId)}/analyze`,
    { method: 'POST' },
  ));
}
