export interface VisualObservation {
  aspect: string;
  visibleEvidence: string;
  videoTimestamp: string;
  certainty: 'high' | 'medium' | 'low';
}

export interface VisualFindings {
  observations: VisualObservation[];
  limitations: string[];
}

export interface StorefrontMedia {
  id: string;
  businessId: string;
  originalFilename: string;
  mimeType: string;
  sizeBytes: number;
  sha256: string;
  status: 'UPLOADED' | 'ANALYZING' | 'ANALYZED' | 'FAILED';
  analysisAttempts: number;
  modelName: string | null;
  modelResponseId: string | null;
  findings: VisualFindings | null;
  lastError: string | null;
  createdAt: string;
  analyzedAt: string | null;
}
