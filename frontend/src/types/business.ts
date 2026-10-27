
export interface BusinessProfileRequest {
  name: string;
  industry: string;
  streetAddress: string;
  city: string;
  state: string;
  postalCode: string;
  website: string;
  services: string;
  primaryGoal: string;
  researchRadiusMiles: 4 | 10;
}

export interface BusinessProfile extends BusinessProfileRequest {
  id: string;
  createdAt: string;
  updatedAt: string;
}
