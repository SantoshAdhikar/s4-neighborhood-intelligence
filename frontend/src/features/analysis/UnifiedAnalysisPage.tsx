import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import { ArrowRight, BookOpen, CheckCircle2, CircleAlert, Clock3, Coins, FileImage, Globe, LoaderCircle, MapPinned, Radar, Sparkles, Trash2, SquareCheckBig } from 'lucide-react';
import { getBusiness } from '../../services/businessApi';
import { listStorefrontMedia, uploadStorefrontMedia } from '../../services/storefrontApi';
import { latestEvidenceReport, readEvidenceReport } from '../../services/evidenceApi';
import { deleteAnalysisHistory, latestAnalysis, listAnalysisHistory, readAnalysis, startAnalysis } from '../../services/analysisApi';
import type { AnalysisRun, AnalysisRunHistoryItem } from '../../types/analysis';
import type { BusinessProfile } from '../../types/business';
import type { ActionReadiness, EvidenceReport, EvidenceSource } from '../../types/evidence';
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

type EvidenceCoverageItem = {
  rank: number;
  level: string;
  label: string;
  explanation: string;
  observedSources: number;
  contextualSources: number;
  ownerSources: number;
};

function Report({ report }: { report: EvidenceReport }) {
  const sources = useMemo(() => new Map(report.sources.map((source) => [source.id, source])), [report.sources]);
  const coverageItems = (report as EvidenceReport & { evidenceCoverage?: EvidenceCoverageItem[] }).evidenceCoverage;
  const coverage = useMemo(() => new Map((coverageItems ?? []).map((item) => [item.rank, item])), [coverageItems]);
  const actionReadiness = useMemo(() => new Map((report.actionReadiness ?? []).map(item => [item.modelRank, item])), [report.actionReadiness]);
  const ranked = useMemo(() => [...report.opportunities].sort((a, b) => {
    const aPosition = actionReadiness.get(a.rank)?.recommendedOrder ?? a.rank;
    const bPosition = actionReadiness.get(b.rank)?.recommendedOrder ?? b.rank;
    return aPosition - bPosition;
  }), [report.opportunities, actionReadiness]);
  return <section className="s4-unified-report" aria-label="Business growth action report">
    <header className="s4-unified-section-head"><div><p className="s4-kicker">BUSINESS GROWTH REPORT</p>
      <h2>Your three priorities</h2><p>Suggested action order uses cited evidence, owner-goal wording, and stated effort. It does not predict sales or guarantee success.</p></div>
      <span className="s4-unified-model"><Sparkles size={16}/> AI recommendations · {report.modelName}</span></header>
    <div className="s4-unified-opportunities">
      {ranked.map((item) => <article key={item.rank}>
        <div className="s4-unified-opportunity-rank">{actionReadiness.has(item.rank)
          ? `SUGGESTED ACTION ${actionReadiness.get(item.rank)?.recommendedOrder} · MODEL PRIORITY ${item.rank}`
          : `PRIORITY ${item.rank}`} · {item.effort.toUpperCase()} EFFORT</div>
        {actionReadiness.get(item.rank) && <div className="s4-action-readiness">
          <span className={`s4-action-readiness-tag is-${actionReadiness.get(item.rank)?.readiness.toLowerCase().replaceAll('_', '-')}`}>
            {actionReadiness.get(item.rank)?.readiness === 'READY_TO_TEST' ? 'Ready for a small test'
              : actionReadiness.get(item.rank)?.readiness === 'PLAN_FIRST' ? 'Plan effort first' : 'Verify evidence first'}
          </span>
          <p>{actionReadiness.get(item.rank)?.reason}</p>
          {(actionReadiness.get(item.rank) as ActionReadiness).matchedGoalTerms > 0 &&
            <small>Shares wording with the owner-stated goal or service focus. This is not a semantic relevance guarantee.</small>}
        </div>}
        <h3>{item.title}</h3><p>{item.rationale}</p>
        {coverage.get(item.rank) && <div className={`s4-evidence-coverage s4-coverage-${coverage.get(item.rank)?.level.toLowerCase().replaceAll('_', '-')}`}>
          <strong>{coverage.get(item.rank)?.label}</strong>
          <p>{coverage.get(item.rank)?.explanation}</p>
          <small>{coverage.get(item.rank)?.observedSources} observed · {coverage.get(item.rank)?.contextualSources} contextual · {coverage.get(item.rank)?.ownerSources} owner provided</small>
        </div>}
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
  const [searchParams, setSearchParams] = useSearchParams();
  const savedRunId = searchParams.get('runId');
  const [history, setHistory] = useState<AnalysisRunHistoryItem[]>([]);
  const [expandedHistory, setExpandedHistory] = useState(Boolean(savedRunId));
  const [historyPage, setHistoryPage] = useState(0);
  const [historyHasMore, setHistoryHasMore] = useState(false);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState('');
  const [selectionMode, setSelectionMode] = useState(false);
  const [selectedRunIds, setSelectedRunIds] = useState<string[]>([]);
  const [deletingHistory, setDeletingHistory] = useState(false);
  const [historyNotice, setHistoryNotice] = useState('');
  const [business, setBusiness] = useState<BusinessProfile | null>(null);
  const [media, setMedia] = useState<StorefrontMedia[]>([]);
  const [goal, setGoal] = useState('');
  const [focusServices, setFocusServices] = useState('');
  const goalFieldRef = useRef<HTMLTextAreaElement>(null);
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
  const [showAllCompetitors, setShowAllCompetitors] = useState(false);

  useLayoutEffect(() => {
    const field = goalFieldRef.current;
    if (!field) return;
    field.style.height = 'auto';
    field.style.height = `${field.scrollHeight}px`;
  }, [goal, loading]);

  useEffect(() => {
    if (loading) return;
    const field = goalFieldRef.current;
    if (!field || !field.parentElement) return;
    const observer = new ResizeObserver(() => {
      field.style.height = 'auto';
      field.style.height = `${field.scrollHeight}px`;
    });
    observer.observe(field.parentElement);
    return () => observer.disconnect();
  }, [loading]);

  useEffect(() => {
    let active = true;
    const previousRequest = savedRunId ? readAnalysis(businessId, savedRunId) : latestAnalysis(businessId);
    const historyRequest = listAnalysisHistory(businessId).catch(() => null);
    void Promise.all([getBusiness(businessId), listStorefrontMedia(businessId), previousRequest, historyRequest])
      .then(async ([profile, images, previous, savedHistory]) => {
        if (!active) return;
        setBusiness(profile);
        if (savedHistory) {
          setHistory(savedHistory.items);
          setHistoryHasMore(savedHistory.hasMore);
        } else {
          setHistoryError('Saved analysis history could not be loaded. Try refreshing the page.');
        }
        setMedia(images);
        setGoal(profile.primaryGoal);
        setWebsite(profile.website ?? '');
        setFocusServices((profile.services ?? '').slice(0, 500));
        setMediaId(images.find(item => item.status === 'ANALYZED')?.id ?? '');
        setRun(previous);
        setLoading(false);

        // An older report may be unavailable even when the active run is healthy.
        const linkedReport = previous?.reportId
          ? await readEvidenceReport(businessId, previous.reportId).catch(() => null)
          : null;
        if (!active) return;
        if (linkedReport?.status === 'COMPLETED') {
          setReport(linkedReport);
          return;
        }
        // Only the default/latest view may show a legacy standalone report.
        // Never attach an unrelated report to a historical run.
        if (!savedRunId) {
          const savedReport = await latestEvidenceReport(businessId).catch(() => null);
          if (active && savedReport?.status === 'COMPLETED') setReport(savedReport);
        }
      })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : 'Could not load business');
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [businessId, savedRunId]);

  async function refreshHistory() {
    if (historyLoading) return;
    setHistoryLoading(true);
    setHistoryError('');
    try {
      const data = await listAnalysisHistory(businessId);
      setHistory(data.items);
      setHistoryPage(0);
      setHistoryHasMore(data.hasMore);
    } catch (cause) {
      setHistoryError(cause instanceof Error ? cause.message : 'Could not refresh saved analyses.');
    } finally {
      setHistoryLoading(false);
    }
  }

  async function loadMoreHistory() {
    if (historyLoading || !historyHasMore) return;
    setHistoryLoading(true);
    setHistoryError('');
    try {
      const nextPage = historyPage + 1;
      const data = await listAnalysisHistory(businessId, nextPage);
      setHistory(previous => [...previous, ...data.items.filter(item => !previous.some(saved => saved.id === item.id))]);
      setHistoryPage(nextPage);
      setHistoryHasMore(data.hasMore);
    } catch (cause) {
      setHistoryError(cause instanceof Error ? cause.message : 'Could not load earlier analyses.');
    } finally {
      setHistoryLoading(false);
    }
  }

  function toggleSelected(id: string) {
    setSelectedRunIds(previous => previous.includes(id)
      ? previous.filter(item => item !== id) : [...previous, id]);
  }

  async function removeHistory(ids: string[]) {
    if (deletingHistory || ids.length === 0) return;
    const allowed = history.filter(item => ids.includes(item.id)
      && item.status !== 'RUNNING' && item.status !== 'QUEUED');
    if (allowed.length !== ids.length) {
      setHistoryError('Only finished or failed analyses can be deleted.');
      return;
    }
    const message = ids.length === 1
      ? 'Permanently remove this analysis from history and delete its captured website findings? Saved Growth Reports and photos will be kept.'
      : `Permanently remove ${ids.length} analyses from history and delete their captured website findings? Saved Growth Reports and photos will be kept.`;
    if (!window.confirm(message)) return;
    setDeletingHistory(true);
    setHistoryError('');
    setHistoryNotice('');
    try {
      await deleteAnalysisHistory(businessId, ids);
      setSelectedRunIds([]);
      setSelectionMode(false);
      if (run?.id && ids.includes(run.id)) {
        window.location.assign(`/businesses/${encodeURIComponent(businessId)}/analyze`);
        return;
      }
      setHistoryNotice(`${ids.length} saved ${ids.length === 1 ? 'analysis' : 'analyses'} removed from history.`);
      // Reload from page zero because deleting a run changes pagination offsets.
      const updated = await listAnalysisHistory(businessId);
      setHistory(updated.items);
      setHistoryPage(0);
      setHistoryHasMore(updated.hasMore);
    } catch (cause) {
      setHistoryError(cause instanceof Error ? cause.message : 'Could not delete selected analyses.');
    } finally {
      setDeletingHistory(false);
    }
  }

  const activeRunId =
    run?.status === 'QUEUED' || run?.status === 'RUNNING'
      ? run.id
      : null;

  useEffect(() => {
    if (!activeRunId) return;
    const runId = activeRunId;
    let active = true;
    let timer: number | undefined;

    async function refreshRun() {
      try {
        const data = await readAnalysis(businessId, runId);
        if (!active) return;
        if (data.status === 'COMPLETED' && data.reportId) {
          try {
            const savedReport = await readEvidenceReport(businessId, data.reportId);
            if (!active) return;
            setReport(savedReport);
            setError('');
          } catch {
            if (!active) return;
            setError('The analysis completed, but its report could not be loaded. Refresh this page to try again.');
          }
        } else {
          setError('');
        }
        setRun(data);
        setHistory(previous => previous.map(item => item.id === data.id
          ? { ...item, status: data.status, completedAt: data.completedAt, reportId: data.reportId }
          : item));
        if (data.status === 'QUEUED' || data.status === 'RUNNING') {
          timer = window.setTimeout(() => void refreshRun(), 1600);
        }
      } catch (cause) {
        if (!active) return;
        setError(cause instanceof Error ? cause.message : 'Could not refresh analysis status. Retrying…');
        timer = window.setTimeout(() => void refreshRun(), 4000);
      }
    }

    void refreshRun();
    return () => {
      active = false;
      if (timer !== undefined) window.clearTimeout(timer);
    };
  }, [businessId, activeRunId]);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting || run?.status === 'QUEUED' || run?.status === 'RUNNING'
      || history.some(item => item.status === 'QUEUED' || item.status === 'RUNNING')) return;
    setSubmitting(true);setError('');setShowAllCompetitors(false);
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
        goal: goal.trim(), focusServices: focusServices.trim(), website: website.trim(), customerFeedback: customerFeedback.trim(),
        mediaId: selectedId, useMeta, segmentSign: useMeta && segmentSign,
      });
      setRun(result);
      // Return to the latest view after a successful new submission.
      if (savedRunId) setSearchParams({});
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not start analysis');
    } finally { setSubmitting(false); }
  }

  const running = submitting || run?.status === 'QUEUED' || run?.status === 'RUNNING'
    || history.some(item => item.status === 'QUEUED' || item.status === 'RUNNING');
  const competitors = run?.websites.filter(site => site.kind === 'NEARBY_COMPETITOR') ?? [];
  const legacyResearch = run?.websites.some(site => site.kind === 'NEARBY_WEBSITE') && competitors.length === 0;
  const specialtyCount = competitors.filter(site => site.matchLevel === 'DIRECT_SPECIALTY').length;
  const possibleCount = competitors.filter(site => site.matchLevel === 'POTENTIAL_OVERLAP' || site.matchLevel === 'SAME_INDUSTRY').length;
  const relatedCount = competitors.filter(site => site.matchLevel === 'RELATED_INDUSTRY').length;
  const fetchedCount = competitors.filter(site => site.status === 'FETCHED').length;
  const visibleWebsites = [
    ...(run?.websites.filter(site => site.kind !== 'NEARBY_COMPETITOR') ?? []),
    ...(showAllCompetitors ? competitors : competitors.slice(0, 12)),
  ];
  const visibleHistory = expandedHistory ? history : history.slice(0, 3);
  const freshReport = run?.status === 'COMPLETED' && Boolean(run.reportId && report?.id === run.reportId);

  return <main className="s4-unified-page">
    <Link className="s4-unified-back" to="/dashboard">← All businesses</Link>
    <header className="s4-unified-hero"><p className="s4-kicker">S4 NEIGHBORHOOD INTELLIGENCE</p>
      <h1>One analysis. One clear action plan.</h1><p>Tell S4 what you want to improve. We'll gather available local and website evidence, examine your photo when authorized, and present the findings here.</p>
      <div className="s4-unified-benefits"><span><MapPinned size={16}/> Nearby businesses</span><span><Globe size={16}/> Public websites</span><span><FileImage size={16}/> Storefront evidence</span><span><Radar size={16}/> Prioritized actions</span></div>
    </header>
    <section className="s4-unified-history" aria-label="Saved analysis history">
      <div className="s4-unified-history-heading">
        <div><p className="s4-kicker">SAVED ANALYSES</p><h2>Analysis history</h2>
          <p>Reopen earlier findings without generating a new report or using AI credits.</p></div>
        <div className="s4-unified-history-toolbar">
          <button type="button" onClick={() => void refreshHistory()} disabled={historyLoading}
            className="s4-unified-history-latest">Refresh list</button>
          {savedRunId && <Link className="s4-unified-history-latest" to={`/businesses/${businessId}/analyze`}>View latest</Link>}
        </div>
      </div>
      {history.length > 0 && <div className="s4-unified-history-manage">
        <button type="button" className="s4-unified-history-latest" disabled={deletingHistory}
          onClick={() => { setSelectionMode(current => !current); setSelectedRunIds([]); }}>
          <SquareCheckBig size={15}/> {selectionMode ? 'Cancel selection' : 'Select analyses'}
        </button>
        {selectionMode && <>
          <button type="button" className="s4-unified-history-latest"
            onClick={() => setSelectedRunIds(visibleHistory.filter(item => item.status !== 'RUNNING' && item.status !== 'QUEUED').map(item => item.id))}>
            Select shown ({visibleHistory.filter(item => item.status !== 'RUNNING' && item.status !== 'QUEUED').length})
          </button>
          <button type="button" className="s4-unified-history-latest" onClick={() => setSelectedRunIds([])}>Clear</button>
          <button type="button" className="s4-unified-history-delete" disabled={!selectedRunIds.length || deletingHistory}
            onClick={() => void removeHistory(selectedRunIds)}>
            <Trash2 size={15}/> {deletingHistory ? 'Deleting…' : `Delete selected (${selectedRunIds.length})`}
          </button>
        </>}
      </div>}
      {history.length > 0 ? <div className="s4-unified-history-list">
        {visibleHistory.map((item, index) => <div key={item.id} className={`s4-unified-history-card ${run?.id === item.id ? 'is-selected' : ''}`}>
          {selectionMode && <label className="s4-unified-history-checkbox">
            <input type="checkbox" checked={selectedRunIds.includes(item.id)}
              disabled={item.status === 'RUNNING' || item.status === 'QUEUED' || deletingHistory}
              onChange={() => toggleSelected(item.id)} aria-label={`Select analysis from ${new Date(item.createdAt).toLocaleString()}`}/>
          </label>}
          <Link to={`/businesses/${businessId}/analyze?runId=${encodeURIComponent(item.id)}`}
            className="s4-unified-history-item" aria-current={run?.id === item.id ? 'page' : undefined}>
            <div className="s4-unified-history-item-title"><strong>{index === 0 ? 'Most recent' : 'Previous analysis'} · {new Date(item.createdAt).toLocaleString()}</strong>
              <span className={`s4-unified-history-status is-${item.status.toLowerCase()}`}>{item.status}</span></div>
            <p>{item.goal}</p><small>{item.useMeta ? 'Meta AI' : 'Local AI'} · {item.reportId ? 'Saved growth report' : 'Research record'}{run?.id === item.id ? ' · Viewing now' : ''}</small>
          </Link>
          {!selectionMode && <button type="button" className="s4-unified-history-delete-one" title="Remove this analysis from history"
            aria-label={`Delete analysis from ${new Date(item.createdAt).toLocaleString()}`}
            disabled={item.status === 'RUNNING' || item.status === 'QUEUED' || deletingHistory}
            onClick={() => void removeHistory([item.id])}><Trash2 size={15}/></button>}
        </div>)}
      </div> : !loading && <p className="s4-unified-history-empty">No saved analyses found for this business yet.</p>}
      {history.length > 3 && <button type="button" className="s4-unified-history-more"
        onClick={() => setExpandedHistory(current => !current)}>{expandedHistory ? 'Show recent 3' : `See all ${history.length} recent analyses`}</button>}
      {historyHasMore && expandedHistory && <button type="button" className="s4-unified-history-more" disabled={historyLoading}
        onClick={() => void loadMoreHistory()}>{historyLoading ? 'Loading earlier analyses…' : 'Show older analyses'}</button>}
      {savedRunId && run && !history.some(item => item.id === run.id) && <p className="s4-unified-history-empty">Viewing archived analysis from {new Date(run.createdAt).toLocaleString()}.</p>}
      {historyNotice && <p role="status" className="s4-unified-history-notice">{historyNotice}</p>}
      {historyError && <p role="alert" className="s4-form-error">{historyError}</p>}
      <p className="s4-unified-history-safety">Deletion removes selected analysis entries and their run-specific website findings. Saved Growth Reports, uploaded photos and the business profile are preserved. Active runs cannot be deleted.</p>
    </section>
    {loading ? <p>Loading business profile…</p> : <form className="s4-unified-form" onSubmit={(event) => void submit(event)}>
      <div className="s4-unified-section-head"><div><p className="s4-kicker">NEW ANALYSIS</p><h2>{business?.name}</h2><p>All requirements in one place. No more switching between steps.</p></div></div>
      <label htmlFor="s4-unified-goal">What is your main business goal?</label>
      <textarea ref={goalFieldRef} id="s4-unified-goal" rows={5} maxLength={5000} required value={goal} onChange={event => setGoal(event.target.value)} placeholder="For example, improve appointments for our lash lift service" />
        <small className="s4-unified-goal-count">{goal.length.toLocaleString()} / 5,000 characters</small>
      <p className="s4-unified-source-note">Industry: {business?.industry ?? 'Not selected'} · Search area: {business?.researchRadiusMiles ?? 4} miles. S4 uses these settings in automatic research and the manual Neighborhood Explorer. You can adjust them in Business Settings.</p>
      <label htmlFor="s4-unified-focus">Which services should S4 focus on when finding competitors?</label>
      <textarea id="s4-unified-focus" rows={2} maxLength={500} value={focusServices}
        onChange={event => setFocusServices(event.target.value)}
        placeholder="Example: haircuts and coloring; Indian cuisine; nail services; brake repair" />
      <small>Pre-filled from your business profile. Edit this for each analysis. S4 ranks specialty-name matches first; these remain unverified until supported by published service evidence.</small>
      <div className="s4-unified-two">
        <div><label htmlFor="s4-unified-website">Your business website (optional)</label>
          <input id="s4-unified-website" type="url" placeholder="https://example.com" value={website} onChange={event => setWebsite(event.target.value)} />
          <small>Free homepage and selected internal-page research; sites may block automated access.</small></div>
        <div><label htmlFor="s4-unified-media">New storefront photo (optional)</label>
          <input id="s4-unified-media" type="file" accept="image/png,image/jpeg,video/mp4" onChange={event => setPhoto(event.target.files?.[0] ?? null)} />
          <small>Saved with this submission. Enable Meta below to inspect the photo automatically; otherwise it is stored without visual analysis.</small></div>
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
        <div><strong>Free-only research and local AI</strong><p>Up to 50 nearby same-industry or clearly labeled related business candidates from OpenStreetMap plus optional Overture Maps enrichment, with up to 12 competitor homepages and a bounded sample of service, booking, pricing and contact pages checked per run. Candidate listings are not proof of direct competition, and website access may be unavailable. Neighborhood data, accessible website summaries, and a local Ollama action report when qwen2.5:7b is running. No Meta requests. A newly uploaded photo is saved locally but not visually analyzed in free-only mode; saved Muse findings can still be reused.</p></div>
        <label className="s4-unified-toggle"><input type="checkbox" checked={useMeta} onChange={event => setUseMeta(event.target.checked)} /><span><Coins size={17}/> Automatically inspect the selected photo and generate the report with Meta (uses promotional credits)</span></label>
        {useMeta && <label className="s4-unified-toggle"><input type="checkbox" checked={segmentSign} onChange={event => setSegmentSign(event.target.checked)}/><span>Also run SAM 3.1 sign segmentation if needed (additional credit usage)</span></label>}
      </div>
      <div className="s4-unified-actions"><button type="submit" disabled={running || !goal.trim()} className="s4-unified-submit">{submitting ? <LoaderCircle className="s4-spin" size={18}/> : <img className="s4-unified-submit-icon" src="/business-actions/start-complete-analysis.png" alt="" />} {submitting ? 'Saving your request…' : running ? 'Analysis running…' : 'Start complete analysis'} <ArrowRight size={18}/></button>
      <span>{useMeta ? 'One click runs neighborhood research, website checks, optional photo inspection, and the Meta report. API credits may be used.' : 'One click runs neighborhood research, website checks and a local Ollama report. Saved photos are not visually analyzed in local mode.'}</span></div>
    </form>}
    {error && <p role="alert" className="s4-form-error"><CircleAlert size={16}/> {error}</p>}
    {run && <Progress run={run}/>}
    {freshReport && report && <Report report={report}/>}
    {run && run.websites.length > 0 && <section className="s4-unified-websites">
      <div className="s4-unified-section-head"><div><p className="s4-kicker">FREE COMPETITOR & WEBSITE RESEARCH</p>
        <h2>Nearby industry comparison</h2>
        <p>Target: up to 50 likely competitors, ranked by services first and distance second. Public website checks remain bounded; no invented services, ratings, reviews or sales.</p></div></div>
      <div className="s4-unified-coverage" role="status">
        <span><strong>{competitors.length}/50</strong> candidates found</span>
        <span><strong>{specialtyCount}</strong> specialty-name/tag matches</span>
        <span><strong>{possibleCount}</strong> possible service-overlap candidates</span>
        <span><strong>{relatedCount}</strong> related-sector listings</span>
        <span><strong>{fetchedCount}</strong> competitor homepages inspected</span>
        <span><strong>{competitors.filter(site => site.note?.includes('Overture Maps record')).length}</strong> Overture-enriched candidates</span>
        <span><strong>{run.pages?.filter(page => page.status === 'FETCHED').length ?? 0}</strong> additional pages inspected</span>
      </div>
      {legacyResearch && <p className="s4-unified-shortfall">This is an older analysis that inspected up to 3 nearby websites. Start a new analysis to build the 50-business shortlist.</p>}
      {!legacyResearch && competitors.length < 50 && <p className="s4-unified-shortfall">{run?.detail.includes('Neighborhood discovery was unavailable') ? 'Live neighborhood discovery was unavailable. This report cannot establish nearby competitors, but website and available evidence analysis can still complete.' : 'The available open-place datasets identified fewer than 50 category-matched businesses. S4 will not invent listings or label unrelated shops as direct competitors. You can expand the radius in Business Settings if needed.'}</p>}
      <p className="s4-unified-source-note">Research focus: {run.focusServices || business?.services || 'General industry'}. Industry and service-focus candidates are ranked from business names and contributed categories, not independently confirmed service menus. Google review text is not automatically collected.</p>
      <p className="s4-unified-source-note">Candidates come from OpenStreetMap and, when configured, Overture Maps. Verify business identity, service overlap and website ownership before using findings to make competitive claims.</p>
      <details className="s4-unified-research-details">
        <summary>Explore {competitors.length} nearby candidates and {run.pages?.length ?? 0} website-page records</summary>
      {competitors.length > 12 && <div className="s4-unified-expand">
        <span>Showing {showAllCompetitors ? competitors.length : 12} of {competitors.length} nearby candidates (plus your website).</span>
        <button type="button" onClick={() => setShowAllCompetitors(current => !current)} aria-expanded={showAllCompetitors}>
          {showAllCompetitors ? 'Show first 12' : `Show all ${competitors.length} candidates`}
        </button>
      </div>}
      <div className="s4-unified-web-grid">{visibleWebsites.map((site,index) => <article key={`${site.url}-${index}-${site.businessName}`}>
        <span>{site.kind === 'OWNER_WEBSITE' ? 'YOUR WEBSITE' : site.matchLevel === 'DIRECT_SPECIALTY' ? 'SPECIALTY SIGNAL · SERVICE NOT VERIFIED' : site.matchLevel === 'POTENTIAL_OVERLAP' ? 'POSSIBLE SERVICE OVERLAP' : site.matchLevel === 'RELATED_INDUSTRY' ? 'RELATED SECTOR · NOT VERIFIED DIRECT COMPETITOR' : 'SAME INDUSTRY CANDIDATE'}</span>
        <h3>{site.businessName}</h3>
        {site.distanceMiles != null && <small>{site.distanceMiles.toFixed(2)} miles · {site.category ?? 'Business category unavailable'}</small>}
        <small>Website: {site.status === 'NO_WEBSITE' ? 'Not present in the available source record' : site.status === 'FETCHED' ? 'Homepage inspected' : site.status === 'BLOCKED' ? 'Access disallowed' : site.status === 'SKIPPED' ? 'Inspection skipped' : 'Unavailable'}</small>
        {site.title && <p><strong>Page:</strong> {site.title}</p>}
        {site.observedSummary && <p>{site.observedSummary}</p>}
        {site.note && <small>{site.note}</small>}
        {(() => {
          const detailPages = run.pages?.filter(page => page.websiteFindingId === site.id) ?? [];
          return detailPages.length > 0 && <details className="s4-unified-site-pages">
            <summary>Website pages checked ({detailPages.filter(page => page.status === 'FETCHED').length}/{detailPages.length})</summary>
            {detailPages.map(page => <div key={page.pageUrl} className="s4-unified-site-page">
              <div><strong>{page.pageType.toLowerCase()}</strong> · {page.status.toLowerCase()}</div>
              {page.pageTitle && <p>{page.pageTitle}</p>}
              {page.observedSummary && <p>{page.observedSummary}</p>}
              {page.note && <small>{page.note}</small>}
              <a href={page.pageUrl} target="_blank" rel="noopener noreferrer">View checked page</a>
            </div>)}
          </details>;
        })()}
        <div className="s4-unified-links">
          {site.status === 'FETCHED' && <a href={site.url} target="_blank" rel="noopener noreferrer">Open homepage</a>}
          {site.osmSourceUrl && <a href={site.osmSourceUrl} target="_blank" rel="noopener noreferrer">View OSM listing</a>}
          {site.kind === 'NEARBY_COMPETITOR' && <a
            href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent([site.businessName, business?.city, business?.state].filter(Boolean).join(', '))}`}
            target="_blank" rel="noopener noreferrer">Check Google Maps manually</a>}
        </div>
      </article>)}</div>
      </details>
    </section>}
    {run?.status === 'COMPLETED' && !run.useMeta && <div className="s4-unified-free"><CheckCircle2 size={20}/><div><strong>Your free research and local AI report are saved.</strong><p>No Meta API calls were made. Recommendations from a local model still need owner review and evidence verification.</p></div></div>}
    {run?.status === 'FAILED' && <div className="s4-unified-free"><CircleAlert size={20}/><div><strong>Your completed research is preserved.</strong><p>Review the error above; you can start a new run without deleting your business or existing images.</p></div></div>}
    {!savedRunId && report && !freshReport && <details className="s4-unified-previous"><summary>Previously saved Growth Report (not regenerated in this run)</summary><Report report={report}/></details>}
    <footer className="s4-unified-footer"><span>Advanced tools remain available:</span>
      <Link to={`/businesses/${businessId}/neighborhood`}>Neighborhood</Link>
      <Link to={`/businesses/${businessId}/storefront`}>Visual Inspection</Link>
      <Link to={`/businesses/${businessId}/evidence`}>Previous reports</Link>
      <Link to={`/businesses/${businessId}/edit`}>Business settings</Link></footer>
  </main>;
}
