export interface StorefrontSegmentation {
  id: string;
  mediaId: string;
  concept: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  attempts: number;
  modelName: string | null;
  modelResponseId: string | null;
  rawOutput: string | null;
  lastError: string | null;
  createdAt: string;
  completedAt: string | null;
}
