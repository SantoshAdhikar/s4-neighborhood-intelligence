import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react';
import { ArrowLeft, Camera, FileVideo, LoaderCircle, ScanEye } from 'lucide-react';
import { getBusiness } from '../../services/businessApi';
import {
  analyzeStorefrontMedia,
  listStorefrontMedia,
  storefrontFileUrl,
  uploadStorefrontMedia,
} from '../../services/storefrontApi';
import type { BusinessProfile } from '../../types/business';
import type { StorefrontMedia } from '../../types/storefront';
import './StorefrontInspection.css';

interface Props { businessId: string }

export default function StorefrontInspection({ businessId }: Props) {
  const [business, setBusiness] = useState<BusinessProfile | null>(null);
  const [files, setFiles] = useState<StorefrontMedia[]>([]);
  const [selected, setSelected] = useState<File | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    Promise.all([getBusiness(businessId), listStorefrontMedia(businessId)])
      .then(([profile, media]) => {
        if (!active) return;
        setBusiness(profile);
        setFiles(media);
      })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : 'Unable to load storefront media');
      });
    return () => { active = false; };
  }, [businessId]);

  function chooseFile(event: ChangeEvent<HTMLInputElement>) {
    setSelected(event.target.files?.item(0) ?? null);
    setError('');
  }

  async function upload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected || busy) return;
    setBusy('upload');
    setError('');
    try {
      const result = await uploadStorefrontMedia(businessId, selected);
      setFiles((current) => [result, ...current]);
      setSelected(null);
      const input = document.getElementById('storefront-file') as HTMLInputElement | null;
      if (input) input.value = '';
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Upload failed');
    } finally {
      setBusy(null);
    }
  }

  async function analyze(mediaId: string) {
    if (busy) return;
    setBusy(mediaId);
    setError('');
    try {
      const result = await analyzeStorefrontMedia(businessId, mediaId);
      setFiles((current) => current.map((file) => file.id === result.id ? result : file));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Analysis failed');
    } finally {
      setBusy(null);
    }
  }

  return (
    <main className="storefront-page">
      <header className="storefront-header">
        <strong><ScanEye size={23} /> S4 Neighborhood Intelligence</strong>
        <a href={`/businesses/${businessId}/neighborhood`}>
          <ArrowLeft size={16} /> Neighborhood Explorer
        </a>
      </header>
      <div className="storefront-content">
        <p className="storefront-kicker">STEP 03 / VISUAL EVIDENCE</p>
        <h1>Storefront Visual Inspection</h1>
        <p className="storefront-muted">
          {business?.name ?? 'Loading business profile'} · Upload original storefront footage or a photo.
        </p>
        <div className="storefront-notice">
          Images and videos remain on your machine until you explicitly click <strong>Analyze with Meta</strong>.
          That action sends the selected media to Meta Model API. Results describe visible evidence, not verified business performance.
        </div>

        <form onSubmit={(event) => void upload(event)} className="storefront-upload">
          <label htmlFor="storefront-file">Choose storefront media</label>
          <input id="storefront-file" type="file" accept="image/jpeg,image/png,video/mp4,.mp4"
            onChange={chooseFile} required />
          <p>JPEG, PNG, or MP4. Prefer a clear 10–30 second storefront video. Server-enforced upload limits apply.</p>
          {selected && <p className="storefront-selection">Selected: {selected.name}</p>}
          <button type="submit" disabled={!selected || busy !== null}>
            {busy === 'upload' ? <LoaderCircle size={17} className="storefront-spin" /> : <Camera size={17} />}
            {busy === 'upload' ? 'Uploading…' : 'Save media locally'}
          </button>
        </form>

        {error && <div className="storefront-error" role="alert">{error}</div>}

        <h2 className="storefront-section-title">Uploaded evidence ({files.length})</h2>
        {files.length === 0 && <p className="storefront-muted">No storefront media uploaded yet.</p>}
        <div className="storefront-items">
          {files.map((file) => (
            <article className="storefront-card" key={file.id}>
              <div className="storefront-preview">
                {file.mimeType.startsWith('video/') ? (
                  <video controls preload="metadata" src={storefrontFileUrl(businessId, file.id)} />
                ) : (
                  <img src={storefrontFileUrl(businessId, file.id)} alt={`Storefront upload ${file.originalFilename}`} />
                )}
              </div>
              <div className="storefront-card-content">
                <div className="storefront-card-heading">
                  <div>
                    <strong><FileVideo size={17} /> {file.originalFilename}</strong>
                    <small>{(file.sizeBytes / 1024 / 1024).toFixed(2)} MB · {new Date(file.createdAt).toLocaleString()}</small>
                  </div>
                  <span className="storefront-status">{file.status}</span>
                </div>
                {file.status !== 'ANALYZED' && file.status !== 'ANALYZING' && (
                  <button className="storefront-analyze" type="button" disabled={busy !== null}
                    onClick={() => void analyze(file.id)}>
                    {busy === file.id ? <LoaderCircle size={17} className="storefront-spin" /> : <ScanEye size={17} />}
                    {busy === file.id ? 'Analyzing with Meta…' : 'Analyze with Meta Muse Spark'}
                  </button>
                )}
                {file.status === 'ANALYZING' && <p>Analysis already started. Refresh the page to check its status.</p>}
                {file.lastError && <p className="storefront-error" role="alert">{file.lastError}</p>}
                {file.findings && (
                  <section className="storefront-findings">
                    <h3>Model-observed evidence</h3>
                    <p className="storefront-muted">Model: {file.modelName} · Response: {file.modelResponseId}</p>
                    {file.findings.observations.map((item, index) => (
                      <div key={`${file.id}-${index}`} className="storefront-observation">
                        <strong>{item.aspect} <span>({item.certainty})</span></strong>
                        <p>{item.visibleEvidence}</p>
                        <small>Media position: {item.videoTimestamp}</small>
                      </div>
                    ))}
                    <h4>Evidence limitations</h4>
                    {file.findings.limitations.length === 0 ? (
                      <p>No limitations reported by the model; human verification is still required.</p>
                    ) : (
                      <ul>{file.findings.limitations.map((item, index) => <li key={index}>{item}</li>)}</ul>
                    )}
                  </section>
                )}
              </div>
            </article>
          ))}
        </div>
      </div>
    </main>
  );
}
