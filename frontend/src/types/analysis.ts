export interface WebsiteRecord {
  kind: 'OWNER_WEBSITE' | 'NEARBY_WEBSITE' | 'NEARBY_COMPETITOR';
  businessName: string;
  url: string;
  title: string | null;
  observedSummary: string | null;
  status: 'FETCHED' | 'BLOCKED' | 'UNAVAILABLE' | 'NO_WEBSITE' | 'SKIPPED';
  note: string | null;
  fetchedAt: string;
  osmSourceUrl: string | null;
  category: string | null;
  distanceMiles: number | null;
  matchLevel: 'SAME_INDUSTRY' | 'RELATED_INDUSTRY' | null;
}

export interface AnalysisRun {
  id: string;
  status: 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  step: string;
  detail: string;
  goal: string;
  website: string | null;
  mediaId: string | null;
  reportId: string | null;
  useMeta: boolean;
  segmentSign: boolean;
  lastError: string | null;
  createdAt: string;
  completedAt: string | null;
  websites: WebsiteRecord[];
}

export interface AnalysisRequest {
  goal: string;
  website: string;
  customerFeedback: string;
  mediaId: string | null;
  useMeta: boolean;
  segmentSign: boolean;
}
