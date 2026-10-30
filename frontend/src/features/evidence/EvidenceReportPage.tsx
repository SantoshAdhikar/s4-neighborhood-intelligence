import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router';
import { ArrowUpRight, BookOpen, CheckCircle2, ClipboardList, Coins, MapPinned, ScanEye, Sparkles } from 'lucide-react';
import BusinessFlowNav from '../../app/BusinessFlowNav';
import { generateEvidenceReport, latestEvidenceReport } from '../../services/evidenceApi';
import type { EvidenceReport, EvidenceSource } from '../../types/evidence';
import './EvidenceReportPage.css';

function SourceTags({ ids, sources }: { ids: string[]; sources: Map<string, EvidenceSource> }) {
  return (
    <div className="s4-source-tags">
      {ids.map((id) => {
        const source = sources.get(id);
        if (!source) return null;
        return <a key={id} href={source.sourceUrl} title={source.description}
          target={source.sourceUrl.startsWith('http') ? '_blank' : undefined}
          rel={source.sourceUrl.startsWith('http') ? 'noopener noreferrer' : undefined}>
          <BookOpen size={13} /> {source.kind.replaceAll('_', ' ').toLowerCase()} <ArrowUpRight size={12} />
        </a>;
      })}
    </div>
  );
}

export default function EvidenceReportPage({ businessId }: { businessId: string }) {
  const [report, setReport] = useState<EvidenceReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState('');
  const sourceMap = useMemo(() => new Map((report?.sources ?? []).map((source) => [source.id, source])), [report]);
  const coverage = useMemo(() => new Map((report?.evidenceCoverage ?? []).map((item) => [item.rank, item])), [report]);

  useEffect(() => {
    let active = true;
    void latestEvidenceReport(businessId).then((data) => {
      if (active) setReport(data);
    }).catch((cause: unknown) => {
      if (active) setError(cause instanceof Error ? cause.message : 'Unable to load the saved report');
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, [businessId]);

  async function generate() {
    setGenerating(true);
    setError('');
    try {
      const data = await generateEvidenceReport(businessId);
      setReport(data);
      if (data.status === 'FAILED') setError(data.lastError ?? 'Meta could not generate a valid evidence report');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not generate report');
    } finally {
      setGenerating(false);
    }
  }

  const complete = report?.status === 'COMPLETED';
  return (
    <main className="s4-evidence-page">
      <BusinessFlowNav businessId={businessId} current="evidence" />
      <div className="s4-evidence-hero">
        <div>
          <p className="s4-kicker">STEP 04 / EVIDENCE TO ACTION</p>
          <h1>Business Growth Report</h1>
          <p>Three proposed opportunities, their traceable evidence, and a seven-day action plan.</p>
        </div>
        <button type="button" className="s4-evidence-generate" onClick={() => void generate()}
          disabled={generating || loading || report?.status === 'GENERATING'}>
          <Sparkles size={18} /> {generating ? 'Generating with Meta…' : complete ? 'Check for new evidence & generate' : 'Generate with Muse Spark'}
        </button>
      </div>
      <div className="s4-evidence-disclosure"><Coins size={17} /> Opening this page is free. Generating a new report sends saved source summaries to Meta and may use API credits. An unchanged completed source set reuses its saved report.</div>
      {loading && <p>Loading saved evidence report…</p>}
      {error && <p className="s4-form-error" role="alert">{error}</p>}
      {!loading && !report && !error && (
        <div className="s4-evidence-empty">
          <ClipboardList size={32} />
          <h2>Ready to connect the evidence?</h2>
          <p>First make sure you have at least one analyzed storefront image and a saved neighborhood search. Then generate your first action report.</p>
          <div><Link to={`/businesses/${businessId}/storefront`}><ScanEye size={17}/> Visual inspection</Link><Link to={`/businesses/${businessId}/neighborhood`}><MapPinned size={17}/> Neighborhood explorer</Link></div>
        </div>
      )}
      {report?.status === 'GENERATING' && !generating && <p className="s4-form-error">A report request is already running. Don't start another request. Refresh once it finishes.</p>}
      {report?.status === 'FAILED' && !error && <p className="s4-form-error" role="alert">{report.lastError ?? 'The last report request failed. Correct the problem before trying again.'}</p>}
      {complete && report && <>
        <div className="s4-evidence-summary">
          <span><CheckCircle2 size={17} /> Saved report</span>
          <span>{report.sources.length} referenced source records</span>
          <span>{report.modelName} · {new Date(report.completedAt ?? report.createdAt).toLocaleString()}</span>
        </div>
        <section className="s4-evidence-section">
          <h2>Three prioritized opportunities</h2>
          <p className="s4-evidence-intro">These are evidence-grounded proposals, not guarantees of customer growth or revenue.</p>
          <div className="s4-evidence-opportunities">
            {[...report.opportunities].sort((a, b) => a.rank - b.rank).map((item) => (
              <article className="s4-opportunity" key={item.rank}>
                <div className="s4-opportunity-head"><span>#{item.rank} PRIORITY</span><span>{item.effort} effort</span></div>
                <h3>{item.title}</h3>
                <p>{item.rationale}</p>
                {coverage.get(item.rank) && <div className={`s4-evidence-coverage s4-coverage-${coverage.get(item.rank)?.level.toLowerCase().replaceAll('_', '-')}`}>
                  <strong>{coverage.get(item.rank)?.label}</strong>
                  <p>{coverage.get(item.rank)?.explanation}</p>
                  <small>{coverage.get(item.rank)?.observedSources} observed · {coverage.get(item.rank)?.contextualSources} contextual · {coverage.get(item.rank)?.ownerSources} owner provided</small>
                </div>}
                <strong>Supporting evidence</strong>
                <SourceTags ids={item.evidenceIds} sources={sourceMap} />
                <div className="s4-opportunity-uncertainty"><strong>Uncertainty</strong><p>{item.uncertainty}</p></div>
                <div className="s4-opportunity-measure"><strong>Measure</strong><p>{item.measurement}</p></div>
              </article>
            ))}
          </div>
        </section>
        <section className="s4-evidence-section">
          <h2>Your seven-day action plan</h2>
          <div className="s4-action-days">
            {[...report.plan].sort((a, b) => a.day - b.day).map((day) => (
              <article key={day.day} className="s4-action-day">
                <div className="s4-day-number">Day {day.day}</div>
                <div><h3>{day.task}</h3><p>{day.purpose}</p>
                  <div className="s4-day-measure"><strong>Track:</strong> {day.measurement}</div>
                  <SourceTags ids={day.evidenceIds} sources={sourceMap} />
                </div>
              </article>
            ))}
          </div>
        </section>
        <section className="s4-evidence-section s4-evidence-sources">
          <h2>Evidence register</h2>
          <p className="s4-evidence-intro">Each recommendation refers to records below. Open the source link to inspect its origin.</p>
          {report.sources.map((source) => (
            <div key={source.id} className="s4-evidence-source-row">
              <span>{source.kind.replaceAll('_', ' ')}</span>
              <p>{source.description}</p>
              <a href={source.sourceUrl} target={source.sourceUrl.startsWith('http') ? '_blank' : undefined}
                rel={source.sourceUrl.startsWith('http') ? 'noopener noreferrer' : undefined}>View source <ArrowUpRight size={13}/></a>
            </div>
          ))}
        </section>
        <section className="s4-evidence-section s4-evidence-limitations">
          <h2>Limitations and unverified information</h2>
          <ul>{report.limitations.map((item, index) => <li key={index}>{item}</li>)}</ul>
          <p>Recommendations require owner review. Competitor performance and future revenue were not measured.</p>
        </section>
      </>}
    </main>
  );
}
