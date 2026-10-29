import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router';
import { ArrowRight, BookOpen, CheckCircle2, CircleAlert, Clock3, Coins, FileImage, Globe, LoaderCircle, MapPinned, Radar, Sparkles } from 'lucide-react';
import { getBusiness } from '../../services/businessApi';
import { listStorefrontMedia, uploadStorefrontMedia } from '../../services/storefrontApi';
import { latestEvidenceReport, readEvidenceReport } from '../../services/evidenceApi';
import { latestAnalysis, readAnalysis, startAnalysis } from '../../services/analysisApi';
import type { AnalysisRun } from '../../types/analysis';
import type { BusinessProfile } from '../../types/business';
import type { EvidenceReport, EvidenceSource } from '../../types/evidence';
import type { StorefrontMedia } from '../../types/storefront';
import './UnifiedAnalysisPage.css';

const stages = [
  { id: 'NEIGHBORHOOD', title: 'Local businesses', explanation: 'Cached OpenStreetMap discovery' },
  { id: 'WEBSITES', title: 'Website research', explanation: 'Public owner and nearby homepages' },
  { id: 'VISUAL', title: 'Storefront inspection', explanation: 'Meta only with your consent' },
  { id: 'SEGMENTATION', title: 'Object segmentation', explanation: 'Optional SAM 3.1' },
  { id: 'REPORT', title: 'Action recommendations', explanation: 'Local Ollama or Meta, by choice' },
];

function sourcesFor(ids: string[], sources: Map<string, EvidenceSource>) {
  return ids.map((id) => sources.get(id)).filter((source): source is EvidenceSource => Boolean(source));
}

function Report({ report }: { report: EvidenceReport }) {
  const sources = useMemo(() => new Map(report.sources.map((source) => [source.id, source])), [report.sources]);
  return <section className="s4-unified-report" aria-label="Business growth action report">
    <header className="s4-unified-section-head"><div><p className="s4-kicker">BUSINESS GROWTH REPORT</p>
      <h2>Your three priorities</h2><p>Real model proposals based on saved sources, not predicted revenue.</p></div>
      <span className="s4-unified-verified"><CheckCircle2 size={16}/> {report.modelName}</span></header>
    <div className="s4-unified-opportunities">
      {[...report.opportunities].sort((a, b) => a.rank - b.rank).map((item) => <article key={item.rank}>
        <div className="s4-unified-opportunity-rank">PRIORITY {item.rank} · {item.effort.toUpperCase()} EFFORT</div>
        <h3>{item.title}</h3><p>{item.rationale}</p>
        <div className="s4-unified-note"><strong>Measure:</strong> {item.measurement}</div>
        <details><summary>Supporting evidence ({item.evidenceIds.length})</summary>
          {sourcesFor(item.evidenceIds, sources).map((source) => <p key={source.id}><a href={source.sourceUrl} target={source.sourceUrl.startsWith('http') ? '_blank' : undefined} rel="noopener noreferrer"><BookOpen size={13}/> {source.kind.replaceAll('_', ' ').toLowerCase()}</a> {source.description}</p>)}
        </details>
        <details><summary>Uncertainty</summary><p>{item.uncertainty}</p></details>
      </article>)}
    </div>
    <section className="s4-unified-days"><h2>Your seven-day plan</h2>
      {[...report.plan].sort((a,b) => a.day-b.day).map((day) => <div key={day.day} className="s4-unified-day">
        <span>DAY {day.day}</span><div><h3>{day.task}</h3><p>{day.purpose}</p><small>Track: {day.measurement}</small></div>
      </div>)}
    </section>
    <details className="s4-unified-register"><summary>Evidence register ({report.sources.length}) and limitations</summary>
      {report.sources.map(source => <p key={source.id}><strong>{source.kind.replaceAll('_',' ')}</strong> · {source.description} <a href={source.sourceUrl} rel="noopener noreferrer" target={source.sourceUrl.startsWith('http') ? '_blank' : undefined}>View source</a></p>)}
      <h3>What we could not verify</h3><ul>{report.limitations.map((limitation, index) => <li key={index}>{limitation}</li>)}</ul>
    </details>
  </section>;
}

function Progress({ run }: { run: AnalysisRun }) {
  const current = stages.findIndex(stage => stage.id === run.step);
  const complete = run.status === 'COMPLETED';
  return <section className="s4-unified-progress" aria-live="polite">
    <div className="s4-unified-section-head"><div><p className="s4-kicker">ANALYSIS PROGRESS</p><h2>{complete ? 'Analysis complete' : run.status === 'FAILED' ? 'Analysis needs attention' : 'Research in progress'}</h2>
      <p>{run.detail}</p></div><span className="s4-unified-run-status">{run.status}</span></div>
    <div className="s4-unified-progress-grid">{stages.map((stage, index) => {
      const skipped = (!run.useMeta || !run.mediaId) && (stage.id === 'VISUAL' || stage.id === 'SEGMENTATION') || (stage.id === 'SEGMENTATION' && !run.segmentSign);
      const done = !skipped && (complete || run.status === 'RUNNING' && current > index);
      const active = run.status === 'RUNNING' && current === index;
      return <div key={stage.id} className={`s4-unified-stage ${done ? 'is-done' : ''} ${active ? 'is-active' : ''}`}>
        {done ? <CheckCircle2 size={18}/> : active ? <LoaderCircle className="s4-spin" size={18}/> : <Clock3 size={18}/>}
        <strong>{stage.title}</strong><small>{skipped ? 'Not required for this run' : stage.explanation}</small>
      </div>;
    })}</div>
    {run.status === 'FAILED' && <p role="alert" className="s4-form-error">{run.lastError}</p>}
  </section>;
}

export default function UnifiedAnalysisPage({ businessId }: { businessId: string }) {
  const [business, setBusiness] = useState<BusinessProfile | null>(null);
  const [media, setMedia] = useState<StorefrontMedia[]>([]);
  const [goal, setGoal] = useState('');
  const [website, setWebsite] = useState('');
  const [customerFeedback, setCustomerFeedback] = useState('');
  const [photo, setPhoto] = useState<File | null>(null);
  const [mediaId, setMediaId] = useState('');
  const [useMeta, setUseMeta] = useState(false);
  const [segmentSign, setSegmentSign] = useState(false);
  const [run, setRun] = useState<AnalysisRun | null>(null);
  const [report, setReport] = useState<EvidenceReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    void Promise.all([getBusiness(businessId), listStorefrontMedia(businessId), latestAnalysis(businessId), latestEvidenceReport(businessId)])
      .then(async ([profile, images, previous, savedReport]) => {
        const linkedReport = previous?.reportId
          ? await readEvidenceReport(businessId, previous.reportId).catch(() => null)
          : null;
        if (!active) return;
        setBusiness(profile);setMedia(images);setGoal(profile.primaryGoal);setWebsite(profile.website ?? '');
        setMediaId(images.find(item => item.status === 'ANALYZED')?.id ?? '');
        setRun(previous);setReport(linkedReport?.status === 'COMPLETED' ? linkedReport : savedReport?.status === 'COMPLETED' ? savedReport : null);
      }).catch((cause: unknown) => { if (active) setError(cause instanceof Error ? cause.message : 'Could not load business'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [businessId]);

  useEffect(() => {
    if (!run || (run.status !== 'QUEUED' && run.status !== 'RUNNING')) return;
    let active = true;
    const timer = window.setInterval(() => {
      void readAnalysis(businessId, run.id).then((data) => {
        if (!active) return;
        setRun(data);
        if (data.status === 'COMPLETED' && data.reportId) {
          void readEvidenceReport(businessId, data.reportId).then((fresh) => { if (active) setReport(fresh); });
        }
      }).catch((cause: unknown) => { if (active) setError(cause instanceof Error ? cause.message : 'Could not refresh run status'); });
    }, 1600);
    return () => { active = false; window.clearInterval(timer); };
  }, [businessId, run?.id, run?.status]);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting || run?.status === 'QUEUED' || run?.status === 'RUNNING') return;
    setSubmitting(true);setError('');
    try {
      let selectedId = mediaId || null;
      if (photo) {
        const saved = await uploadStorefrontMedia(businessId, photo);
        selectedId = saved.id;
        setMedia((previous) => [saved, ...previous]);
        setMediaId(saved.id);
        setPhoto(null);
      }
      const result = await startAnalysis(businessId, {
        goal: goal.trim(), website: website.trim(), customerFeedback: customerFeedback.trim(),
        mediaId: selectedId, useMeta, segmentSign: useMeta && segmentSign,
      });
      setRun(result);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not start analysis');
    } finally { setSubmitting(false); }
  }

  const running = submitting || run?.status === 'QUEUED' || run?.status === 'RUNNING';
  const freshReport = run?.status === 'COMPLETED' && Boolean(run.reportId && report?.id === run.reportId);

  return <main className="s4-unified-page">
    <Link className="s4-unified-back" to="/dashboard">← All businesses</Link>
    <header className="s4-unified-hero"><p className="s4-kicker">S4 NEIGHBORHOOD INTELLIGENCE</p>
      <h1>One analysis. One clear action plan.</h1><p>Tell S4 what you want to improve. We'll gather available local and website evidence, examine your photo when authorized, and present the findings here.</p>
      <div className="s4-unified-benefits"><span><MapPinned size={16}/> Nearby businesses</span><span><Globe size={16}/> Public websites</span><span><FileImage size={16}/> Storefront evidence</span><span><Radar size={16}/> Prioritized actions</span></div>
    </header>
    {loading ? <p>Loading business profile…</p> : <form className="s4-unified-form" onSubmit={(event) => void submit(event)}>
      <div className="s4-unified-section-head"><div><p className="s4-kicker">NEW ANALYSIS</p><h2>{business?.name}</h2><p>All requirements in one place. No more switching between steps.</p></div></div>
      <label htmlFor="s4-unified-goal">What is your main business goal?</label>
      <textarea id="s4-unified-goal" rows={3} maxLength={1000} required value={goal} onChange={event => setGoal(event.target.value)} placeholder="For example, improve appointments for our lash lift service" />
      <div className="s4-unified-two">
        <div><label htmlFor="s4-unified-website">Your business website (optional)</label>
          <input id="s4-unified-website" type="url" placeholder="https://example.com" value={website} onChange={event => setWebsite(event.target.value)} />
          <small>Free homepage research; sites may block automated access.</small></div>
        <div><label htmlFor="s4-unified-media">New storefront photo (optional)</label>
          <input id="s4-unified-media" type="file" accept="image/png,image/jpeg,video/mp4" onChange={event => setPhoto(event.target.files?.[0] ?? null)} />
          <small>Uploaded privately to your local backend first.</small></div>
      </div>
      <label htmlFor="s4-unified-saved">Or reuse an existing storefront image</label>
      <select id="s4-unified-saved" value={mediaId} onChange={event => setMediaId(event.target.value)} disabled={Boolean(photo)}>
        <option value="">Automatically use latest analyzed image if available</option>
        {media.map(item => <option key={item.id} value={item.id}>{item.originalFilename} ({item.status.toLowerCase()})</option>)}
      </select>
      <label htmlFor="s4-unified-feedback">Customer feedback you have permission to use (optional)</label>
      <textarea id="s4-unified-feedback" rows={3} maxLength={1500} value={customerFeedback} onChange={event => setCustomerFeedback(event.target.value)} placeholder="Summarize customer requests or feedback without names or personal information" />
      <small>We won't scrape Google reviews. Owner-provided feedback is labeled unverified, not a representative review sample.</small>
      <div className="s4-unified-cost">
        <div><strong>Free-only research and local AI</strong><p>Neighborhood data, accessible website summaries, and a local Ollama action report when qwen2.5:7b is running. No Meta requests. A newly uploaded photo is saved locally but not visually analyzed in free-only mode; saved Muse findings can still be reused.</p></div>
        <label className="s4-unified-toggle"><input type="checkbox" checked={useMeta} onChange={event => setUseMeta(event.target.checked)} /><span><Coins size={17}/> Include Meta AI analysis (uses promotional credits)</span></label>
        {useMeta && <label className="s4-unified-toggle"><input type="checkbox" checked={segmentSign} onChange={event => setSegmentSign(event.target.checked)}/><span>Also run SAM 3.1 sign segmentation if needed (additional credit usage)</span></label>}
      </div>
      <div className="s4-unified-actions"><button type="submit" disabled={running || !goal.trim()} className="s4-unified-submit">{submitting ? <LoaderCircle className="s4-spin" size={18}/> : <Sparkles size={18}/>} {submitting ? 'Saving your request…' : running ? 'Analysis running…' : 'Start complete analysis'} <ArrowRight size={18}/></button>
      <span>{useMeta ? 'Meta requests are opt-in; cached results may be reused.' : 'Free-only mode is selected. No Meta billing; local Ollama must be running.'}</span></div>
    </form>}
    {error && <p role="alert" className="s4-form-error"><CircleAlert size={16}/> {error}</p>}
    {run && <Progress run={run}/>}
    {run && run.websites.length > 0 && <section className="s4-unified-websites">
      <div className="s4-unified-section-head"><div><p className="s4-kicker">FREE WEBSITE RESEARCH</p><h2>What we found online</h2><p>Public homepage observations only, not ratings or a full marketing audit.</p></div></div>
      <div className="s4-unified-web-grid">{run.websites.map((site,index) => <article key={`${site.url}-${index}`}>
        <span>{site.kind === 'OWNER_WEBSITE' ? 'YOUR WEBSITE' : 'NEARBY BUSINESS'}</span>
        <h3>{site.businessName}</h3><small>{site.status}</small>
        {site.title && <p><strong>Page:</strong> {site.title}</p>}
        {site.observedSummary && <p>{site.observedSummary}</p>}
        {site.note && <small>{site.note}</small>}
        {site.status === 'FETCHED' && <a href={site.url} target="_blank" rel="noopener noreferrer">Open source ↗</a>}
      </article>)}</div>
    </section>}
    {freshReport && report && <Report report={report}/>}
    {run?.status === 'COMPLETED' && !run.useMeta && <div className="s4-unified-free"><CheckCircle2 size={20}/><div><strong>Your free research and local AI report are saved.</strong><p>No Meta API calls were made. Recommendations from a local model still need owner review and evidence verification.</p></div></div>}
    {run?.status === 'FAILED' && <div className="s4-unified-free"><CircleAlert size={20}/><div><strong>Your completed research is preserved.</strong><p>Review the error above; you can start a new run without deleting your business or existing images.</p></div></div>}
    {report && !freshReport && <details className="s4-unified-previous"><summary>Previously saved Growth Report (not regenerated in this run)</summary><Report report={report}/></details>}
    <footer className="s4-unified-footer"><span>Advanced tools remain available:</span>
      <Link to={`/businesses/${businessId}/neighborhood`}>Neighborhood</Link>
      <Link to={`/businesses/${businessId}/storefront`}>Visual Inspection</Link>
      <Link to={`/businesses/${businessId}/evidence`}>Previous reports</Link>
      <Link to={`/businesses/${businessId}/edit`}>Business settings</Link></footer>
  </main>;
}
