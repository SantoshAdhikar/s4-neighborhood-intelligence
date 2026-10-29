
import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import BusinessFlowNav from '../../app/BusinessFlowNav';
import {
  ArrowRight,
  Building2,
  CheckCircle2,
  LoaderCircle,
  MapPin,
  Radar,
} from 'lucide-react';

import {
  createBusiness,
  getBusiness,
  updateBusiness,
} from '../../services/businessApi';

import type {
  BusinessProfile,
  BusinessProfileRequest,
} from '../../types/business';

const initialForm: BusinessProfileRequest = {
  name: '',
  industry: '',
  streetAddress: '',
  city: '',
  state: '',
  postalCode: '',
  website: '',
  services: '',
  primaryGoal: '',
  researchRadiusMiles: 4,
};

export default function BusinessOnboarding() {
  const { businessId } = useParams();
  const navigate = useNavigate();
  const [form, setForm] = useState<BusinessProfileRequest>(initialForm);
  const [saved, setSaved] = useState<BusinessProfile | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!businessId) return;
    let active = true;
    void getBusiness(businessId).then((profile) => {
      if (!active) return;
      setSaved(profile);
      setForm({
        name: profile.name, industry: profile.industry,
        streetAddress: profile.streetAddress, city: profile.city,
        state: profile.state, postalCode: profile.postalCode,
        website: profile.website ?? '', services: profile.services,
        primaryGoal: profile.primaryGoal,
        researchRadiusMiles: profile.researchRadiusMiles,
      });
    }).catch((cause: unknown) => {
      if (active) setError(cause instanceof Error ? cause.message : 'Unable to load business');
    });
    return () => { active = false; };
  }, [businessId]);

  function handleChange(
    event: ChangeEvent<
      HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
    >,
  ) {
    const { name, value } = event.target;

    if (name === 'researchRadiusMiles') {
      setForm((current) => ({
        ...current,
        researchRadiusMiles: Number(value) as 4 | 10,
      }));
    } else {
      setForm((current) => ({
        ...current,
        [name]: value,
      }));
    }

    setError('');
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (saving) return;

    setSaving(true);
    setError('');

    try {
      const result = saved
        ? await updateBusiness(saved.id, form)
        : await createBusiness(form);

      setSaved(result);
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'Unable to save your business. Please try again.',
      );
    } finally {
      setSaving(false);
    }
  }

  function startAnotherBusiness() {
    setForm(initialForm);
    setSaved(null);
    setError('');
    navigate('/businesses/new');
  }

  return (
    <main className="onboarding-page">

      <div className="s4-onboarding-nav"><BusinessFlowNav businessId={saved?.id ?? businessId} current="profile" /></div>
      <div className="onboarding-layout">
        <aside className="intro-panel">
          <div className="eyebrow">
            YOUR NEIGHBORHOOD. YOUR NEXT MOVE.
          </div>

          <h1>
            Understand what's happening around your business.
          </h1>

          <p>
            Start with your business information. S4 will use it
            to investigate your local market and connect
            recommendations to real evidence.
          </p>

          <div className="steps">
            <div className="step active">
              <Building2 size={20} />
              <div>
                <strong>01. Business profile</strong>
                <span>Tell us about your business</span>
              </div>
            </div>

            <div className="step">
              <MapPin size={20} />
              <div>
                <strong>02. Neighborhood research</strong>
                <span>Discover nearby businesses</span>
              </div>
            </div>

            <div className="step">
              <Radar size={20} />
              <div>
                <strong>03. Evidence & recommendations</strong>
                <span>Understand your next move</span>
              </div>
            </div>
          </div>
        </aside>

        <section className="form-panel">
          <div className="form-heading">
            <span className="form-kicker">
              STEP 01 / BUSINESS ONBOARDING
            </span>

            <h2>Tell us about your business</h2>

            <p>
              We'll use these details as the starting point
              for neighborhood research.
            </p>
          </div>

          <form
            className="business-form"
            onSubmit={handleSubmit}
          >
            <div className="field">
              <label htmlFor="name">Business name *</label>
              <input
                id="name"
                name="name"
                value={form.name}
                onChange={handleChange}
                maxLength={160}
                placeholder="Your business name"
                required
              />
            </div>

            <div className="field">
              <label htmlFor="industry">Industry *</label>

              <select
                id="industry"
                name="industry"
                value={form.industry}
                onChange={handleChange}
                required
              >
                <option value="">Select an industry</option>
                <option value="Beauty & Personal Care">
                  Beauty & Personal Care
                </option>
                <option value="Restaurant & Food">
                  Restaurant & Food
                </option>
                <option value="Retail">Retail</option>
                <option value="Health & Wellness">
                  Health & Wellness
                </option>
                <option value="Professional Services">
                  Professional Services
                </option>
                <option value="Automotive">Automotive</option>
                <option value="Home Services">Home Services</option>
                <option value="Other">Other</option>
              </select>
            </div>

            <div className="field">
              <label htmlFor="streetAddress">
                Street address *
              </label>
              <input
                id="streetAddress"
                name="streetAddress"
                value={form.streetAddress}
                onChange={handleChange}
                maxLength={250}
                placeholder="Street address"
                required
              />
            </div>

            <div className="form-grid">
              <div className="field">
                <label htmlFor="city">City *</label>
                <input
                  id="city"
                  name="city"
                  value={form.city}
                  onChange={handleChange}
                  maxLength={120}
                  required
                />
              </div>

              <div className="field">
                <label htmlFor="state">State *</label>
                <input
                  id="state"
                  name="state"
                  value={form.state}
                  onChange={handleChange}
                  maxLength={100}
                  placeholder="CA"
                  required
                />
              </div>

              <div className="field">
                <label htmlFor="postalCode">
                  ZIP / Postal code *
                </label>
                <input
                  id="postalCode"
                  name="postalCode"
                  value={form.postalCode}
                  onChange={handleChange}
                  maxLength={20}
                  required
                />
              </div>

              <div className="field">
                <label htmlFor="website">
                  Website (optional)
                </label>
                <input
                  id="website"
                  name="website"
                  type="url"
                  value={form.website}
                  onChange={handleChange}
                  maxLength={500}
                  placeholder="https://example.com"
                />
              </div>
            </div>

            <div className="field">
              <label htmlFor="services">
                Products or services *
              </label>
              <textarea
                id="services"
                name="services"
                value={form.services}
                onChange={handleChange}
                rows={3}
                placeholder="Eyebrow threading, waxing, facials..."
                required
              />
            </div>

            <div className="field">
              <label htmlFor="primaryGoal">
                What is your main business goal? *
              </label>
              <textarea
                id="primaryGoal"
                name="primaryGoal"
                value={form.primaryGoal}
                onChange={handleChange}
                maxLength={5000}
                rows={3}
                placeholder="Attract more customers from nearby neighborhoods"
                required
              />
            </div>

            <fieldset className="radius-field">
              <legend>Research radius *</legend>
              <p>
                How far around your business should S4 investigate?
              </p>

              <div className="radius-options">
                {([4, 10] as const).map((radius) => (
                  <label
                    key={radius}
                    className={`radius-option ${
                      form.researchRadiusMiles === radius
                        ? 'selected'
                        : ''
                    }`}
                  >
                    <input
                      type="radio"
                      name="researchRadiusMiles"
                      value={radius}
                      checked={
                        form.researchRadiusMiles === radius
                      }
                      onChange={handleChange}
                    />

                    <strong>{radius} miles</strong>

                    <span>
                      {radius === 4
                        ? 'Immediate neighborhood'
                        : 'Wider local market'}
                    </span>
                  </label>
                ))}
              </div>
            </fieldset>

            {error && (
              <div className="form-error" role="alert">
                {error}
              </div>
            )}

            {saved && (
              <div className="form-success" role="status">
                <CheckCircle2 size={22} />

                <div>
                  <strong>
                    Business profile saved successfully
                  </strong>
                  <span>Profile ID: {saved.id}</span>
                </div>
              </div>
            )}

            <button
              type="submit"
              className="submit-button"
              disabled={saving}
            >
              {saving ? (
                <>
                  <LoaderCircle
                    className="spinner"
                    size={20}
                  />
                  Saving...
                </>
              ) : (
                <>
                  {saved
                    ? 'Save changes'
                    : 'Save business profile'}
                  <ArrowRight size={20} />
                </>
              )}
            </button>

            {saved && (
              <div className="s4-flow-links">
                <Link to={`/businesses/${saved.id}/analyze`}>Start complete analysis →</Link>
                <Link to={`/businesses/${saved.id}/neighborhood`}>Explore neighborhood →</Link>
                <Link to={`/businesses/${saved.id}/storefront`}>Inspect storefront →</Link>
                <Link to="/dashboard">Dashboard →</Link>
              </div>
            )}

            {saved && (
              <button
    type="button"
    onClick={startAnotherBusiness}
    className="add-business-button"
  >
                Add another business
              </button>
            )}
          </form>
        </section>
      </div>
    </main>
  );
}
