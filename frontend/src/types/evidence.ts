export interface EvidenceSource {
  id: string;
  kind: 'OWNER_PROFILE' | 'OWNER_FEEDBACK' | 'STORE_FRONT_IMAGE' | 'SAM_SEGMENTATION' | 'OSM_PLACE' | 'WEBSITE_PAGE' | 'OWNER_WEBSITE' | 'NEARBY_COMPETITOR';
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

export interface EvidenceCoverage {
  rank: number;
  level: 'OBSERVED' | 'CONTEXT' | 'OWNER_ONLY';
  label: string;
  explanation: string;
  observedSources: number;
  contextualSources: number;
  ownerSources: number;
}

export interface EvidenceReport {
  id: string;
  businessId: string;
  status: 'PENDING' | 'GENERATING' | 'COMPLETED' | 'FAILED';
  modelName: string | null;
  modelResponseId: string | null;
  sources: EvidenceSource[];
  opportunities: Opportunity[];
  evidenceCoverage?: EvidenceCoverage[];
  plan: DayAction[];
  limitations: string[];
  lastError: string | null;
  createdAt: string;
  completedAt: string | null;
}
