
export interface NearbyBusiness {
  osmType: 'node' | 'way' | 'relation' | 'overture';
  osmId: number;
  name: string;
  category: string;
  latitude: number;
  longitude: number;
  distanceMiles: number;
  website: string | null;
  sourceUrl: string;
  matchLevel: string | null;
  matchReason: string | null;
}

export interface NeighborhoodReport {
  businessId: string;
  businessName: string;
  radiusMiles: 2 | 3 | 4 | 10;
  latitude: number;
  longitude: number;
  matchedAddress: string;
  fetchedAt: string;
  cached: boolean;
  places: NearbyBusiness[];
}
