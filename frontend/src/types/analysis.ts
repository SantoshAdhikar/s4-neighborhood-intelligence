export interface WebsiteRecord {
  kind: 'OWNER_WEBSITE' | 'NEARBY_WEBSITE';
  businessName: string;
  url: string;
  title: string | null;
  observedSummary: string | null;
  status: 'FETCHED' | 'BLOCKED' | 'UNAVAILABLE';
  note: string | null;
  fetchedAt: string;
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
