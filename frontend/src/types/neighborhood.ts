
export interface NearbyBusiness {
  osmType: 'node' | 'way' | 'relation';
  osmId: number;
  name: string;
  category: string;
  latitude: number;
  longitude: number;
  distanceMiles: number;
  website: string | null;
  sourceUrl: string;
}

export interface NeighborhoodReport {
  businessId: string;
  businessName: string;
  radiusMiles: 4 | 10;
  latitude: number;
  longitude: number;
  matchedAddress: string;
  fetchedAt: string;
  cached: boolean;
  places: NearbyBusiness[];
}
