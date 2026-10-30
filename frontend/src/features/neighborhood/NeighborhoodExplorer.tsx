import { Link } from 'react-router';

import { useEffect, useState } from 'react';
import {
  Circle,
  CircleMarker,
  MapContainer,
  Popup,
  TileLayer,
} from 'react-leaflet';
import 'leaflet/dist/leaflet.css';

import { MapPinned, RefreshCw } from 'lucide-react';
import BusinessFlowNav from '../../app/BusinessFlowNav';
import { getBusiness } from '../../services/businessApi';
import { researchNeighborhood } from '../../services/neighborhoodApi';
import type { BusinessProfile } from '../../types/business';
import type { NeighborhoodReport } from '../../types/neighborhood';
import './NeighborhoodExplorer.css';

interface Props {
  businessId: string;
}

export default function NeighborhoodExplorer({ businessId }: Props) {
  const [business, setBusiness] = useState<BusinessProfile | null>(null);
  const [report, setReport] = useState<NeighborhoodReport | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [onlyRelevant, setOnlyRelevant] = useState(false);
  const visiblePlaces = report?.places.filter(place => !onlyRelevant || place.matchLevel != null) ?? [];
  const specialtyCount = report?.places.filter(place => place.matchLevel === 'DIRECT_SPECIALTY').length ?? 0;

  useEffect(() => {
    let active = true;

    async function load() {
      try {
        const profile = await getBusiness(businessId);
        if (!active) return;

        setBusiness(profile);

        const result = await researchNeighborhood(businessId);
        if (active) setReport(result);
      } catch (cause) {
        if (active) {
          setError(cause instanceof Error ? cause.message : 'Research failed');
        }
      } finally {
        if (active) setLoading(false);
      }
    }

    void load();

    return () => {
      active = false;
    };
  }, [businessId]);

  async function retry() {
    setLoading(true);
    setError('');

    try {
      setReport(await researchNeighborhood(businessId));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Research failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="explorer-page">

      <div className="explorer-content">
        <BusinessFlowNav businessId={businessId} current="neighborhood" />
        <div className="explorer-title-row">
          <div>
            <p className="explorer-eyebrow">
              STEP 02 / NEIGHBORHOOD EXPLORER
            </p>
            <h1>{business?.name ?? 'Neighborhood research'}</h1>
            <p className="explorer-muted">
              {business
                ? `${business.city}, ${business.state} · ${business.researchRadiusMiles}-mile radius`
                : 'Loading business profile'}
            </p>
          </div>

          <button
            type="button"
            onClick={() => void retry()}
            disabled={loading}
            className="explorer-refresh"
          >
            <RefreshCw size={16} />
            {loading ? 'Researching…' : 'Check research'}
          </button>
        </div>

        <Link
          to={`/businesses/${businessId}/storefront`}
          className="explorer-refresh"
          style={{ width: 'fit-content', textDecoration: 'none', marginTop: 14 }}
        >
          Upload storefront evidence
        </Link>

        {error && (
          <div className="explorer-error" role="alert">
            {error}
          </div>
        )}

        {loading && (
          <div className="explorer-loading" role="status">
            Looking up the address and mapped nearby businesses…
          </div>
        )}

        {report && (
          <>
            <div className="explorer-summary">
              <div>
                <span>Mapped results</span>
                <strong>{report.places.length}</strong>
              </div>
              <div>
                <span>Selected area</span>
                <strong>{report.radiusMiles} miles</strong>
              </div>
              <div>
                <span>Source</span>
                <strong>OpenStreetMap</strong>
              </div>
              <div>
                <span>Research</span>
                <strong>
                  {report.cached ? 'Cached snapshot' : 'New snapshot'}
                </strong>
              </div>
            </div>

            <div className="explorer-summary" style={{ marginTop: 12 }}>
              <div><span>Potential specialty matches</span><strong>{specialtyCount}</strong></div>
              <div><span>Ranked industry candidates</span><strong>{report.places.filter(place => place.matchLevel != null).length}</strong></div>
              <label style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                <input type="checkbox" checked={onlyRelevant} onChange={event => setOnlyRelevant(event.target.checked)} />
                Show likely competitors only
              </label>
            </div>

            <p className="explorer-address">
              Matched location: {report.matchedAddress}
            </p>

            <p className="explorer-muted">
              Check the center pin. Geocoding may be approximate.
              Results are sampled and depend on OpenStreetMap coverage;
              they are not a complete list of competitors.
            </p>

            <div className="explorer-grid">
              <div className="explorer-map-frame">
                <MapContainer
                  key={`${report.latitude}-${report.longitude}`}
                  center={[report.latitude, report.longitude]}
                  zoom={report.radiusMiles === 4 ? 12 : 10}
                  scrollWheelZoom={false}
                  className="explorer-map"
                >
                  <TileLayer
                    url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
                    attribution={'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>'}
                  />

                  <Circle
                    center={[report.latitude, report.longitude]}
                    radius={report.radiusMiles * 1609.344}
                    pathOptions={{
                      color: '#aa824e',
                      fillColor: '#aa824e',
                      fillOpacity: 0.05,
                      weight: 2,
                    }}
                  />

                  <CircleMarker
                    center={[report.latitude, report.longitude]}
                    radius={10}
                    pathOptions={{
                      color: '#fff',
                      weight: 3,
                      fillColor: '#10264a',
                      fillOpacity: 1,
                    }}
                  >
                    <Popup>
                      <strong>{report.businessName}</strong>
                      <div>Your saved location</div>
                    </Popup>
                  </CircleMarker>

                  {visiblePlaces.map((place) => (
                    <CircleMarker
                      key={`${place.osmType}-${place.osmId}`}
                      center={[place.latitude, place.longitude]}
                      radius={6}
                      pathOptions={{
                        color: '#fff',
                        weight: 1,
                        fillColor: place.matchLevel === 'DIRECT_SPECIALTY' ? '#256d70' : '#b58345',
                        fillOpacity: 0.95,
                      }}
                    >
                      <Popup>
                        <strong>{place.name}</strong>
                        <div>{place.category}</div>
                        <div>{place.distanceMiles.toFixed(2)} miles</div>
                        {place.matchLevel && <div><strong>{place.matchLevel.replaceAll('_', ' ').toLowerCase()}</strong></div>}
                        {place.matchReason && <div>{place.matchReason}</div>}
                        <a
                          href={place.sourceUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          View OSM record
                        </a>
                      </Popup>
                    </CircleMarker>
                  ))}
                </MapContainer>
              </div>

              <section
                className="explorer-results"
                aria-label="Mapped nearby businesses"
              >
                <h2>
                  <MapPinned size={20} />
                  Nearby mapped businesses
                </h2>

                {report.places.length === 0 && (
                  <p>
                    No matching mapped businesses were returned.
                    This does not prove that no competitors exist.
                  </p>
                )}

                {visiblePlaces.map((place) => (
                  <article
                    key={`${place.osmType}-${place.osmId}`}
                    className="explorer-place"
                  >
                    <strong>{place.name}</strong>
                    {place.matchLevel && <p><b>{place.matchLevel.replaceAll('_', ' ').toLowerCase()}</b></p>}
                    {place.matchReason && <p>{place.matchReason}</p>}
                    <p>
                      {place.category} · {place.distanceMiles.toFixed(2)} mi
                    </p>

                    <a
                      href={place.sourceUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      Source: OpenStreetMap
                    </a>

                    {place.website && (
                      <a
                        href={place.website}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        Business website
                      </a>
                    )}
                  </article>
                ))}
              </section>
            </div>

            <p className="explorer-footnote">
              Retrieved {new Date(report.fetchedAt).toLocaleString()}.
              Results are sampled from Overpass; classification is based on
              business names and contributed tags, not verified service lists. Map and place data © OpenStreetMap contributors,
              available under ODbL.
            </p>
          </>
        )}
      </div>
    </main>
  );
}
