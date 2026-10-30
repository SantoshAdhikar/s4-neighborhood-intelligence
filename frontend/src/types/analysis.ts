export interface WebsiteRecord {
  id: string;
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
  matchLevel: 'DIRECT_SPECIALTY' | 'POTENTIAL_OVERLAP' | 'SAME_INDUSTRY' | 'RELATED_INDUSTRY' | null;
}

export interface WebsitePageRecord {
  websiteFindingId: string;
  pageType: 'SERVICES' | 'BOOKING' | 'PRICING' | 'CONTACT' | 'ABOUT';
  pageUrl: string;
  pageTitle: string | null;
  status: 'FETCHED' | 'BLOCKED' | 'UNAVAILABLE';
  observedSummary: string | null;
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
  focusServices: string | null;
  mediaId: string | null;
  reportId: string | null;
  useMeta: boolean;
  segmentSign: boolean;
  lastError: string | null;
  createdAt: string;
  completedAt: string | null;
  websites: WebsiteRecord[];
  pages: WebsitePageRecord[];
}

export interface AnalysisRequest {
  goal: string;
  focusServices: string;
  website: string;
  customerFeedback: string;
  mediaId: string | null;
  useMeta: boolean;
  segmentSign: boolean;
}

export interface AnalysisRunHistoryItem {
  id: string;
  status: AnalysisRun['status'];
  goal: string;
  createdAt: string;
  completedAt: string | null;
  reportId: string | null;
  useMeta: boolean;
}

export interface AnalysisRunHistoryResponse {
  items: AnalysisRunHistoryItem[];
  page: number;
  hasMore: boolean;
}
