export interface EvidenceSource {
  id: string;
  kind: 'OWNER_PROFILE' | 'STORE_FRONT_IMAGE' | 'SAM_SEGMENTATION' | 'OSM_PLACE';
  description: string;
  sourceUrl: string;
  observedAt: string | null;
}

export interface Opportunity {
  rank: number;
  title: string;
  rationale: string;
  evidenceIds: string[];
  uncertainty: string;
  effort: 'low' | 'medium' | 'high';
  measurement: string;
}

export interface DayAction {
  day: number;
  task: string;
  purpose: string;
  evidenceIds: string[];
  measurement: string;
}

export interface EvidenceReport {
  id: string;
  businessId: string;
  status: 'PENDING' | 'GENERATING' | 'COMPLETED' | 'FAILED';
  modelName: string | null;
  modelResponseId: string | null;
  sources: EvidenceSource[];
  opportunities: Opportunity[];
  plan: DayAction[];
  limitations: string[];
  lastError: string | null;
  createdAt: string;
  completedAt: string | null;
}
