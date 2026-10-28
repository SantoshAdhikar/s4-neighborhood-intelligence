import { apiFetch, readJson } from './apiClient';
import type { StorefrontSegmentation } from '../types/segmentation';

function segmentationPath(businessId: string, mediaId: string) {
  return `/api/businesses/${encodeURIComponent(businessId)}/storefront-media/${encodeURIComponent(mediaId)}/segmentations`;
}

export async function listSegmentations(businessId: string, mediaId: string) {
  return readJson<StorefrontSegmentation[]>(await apiFetch(segmentationPath(businessId, mediaId)));
}

export async function segmentImage(businessId: string, mediaId: string, concept: string) {
  return readJson<StorefrontSegmentation>(await apiFetch(segmentationPath(businessId, mediaId), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ concept }),
  }));
}
