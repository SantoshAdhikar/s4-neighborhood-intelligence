import { decodeMaskToRaster, formats, recordsOfKind } from '@meta-sam/parser';
import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { LoaderCircle, ScanSearch } from 'lucide-react';
import { listSegmentations, segmentImage } from '../../services/segmentationApi';
import { storefrontFileUrl } from '../../services/storefrontApi';
import type { StorefrontSegmentation } from '../../types/segmentation';

interface Props {
  businessId: string;
  mediaId: string;
  mimeType: string;
  sizeBytes: number;
}

function MaskViewer({ imageUrl, rawOutput }: { imageUrl: string; rawOutput: string }) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [dimensions, setDimensions] = useState({ width: 0, height: 0 });
  const [showMasks, setShowMasks] = useState(true);
  const [showBoxes, setShowBoxes] = useState(true);
  const [drawError, setDrawError] = useState('');

  const parsed = useMemo(() => {
    try {
      const parser = formats.segmentation.image().createParser();
      parser.push(rawOutput);
      return { result: parser.finish({ status: 'completed' }).result, error: '' };
    } catch (cause) {
      return { result: null, error: cause instanceof Error ? cause.message : 'Invalid SAM response' };
    }
  }, [rawOutput]);

  const boxes = useMemo(() => parsed.result ? recordsOfKind(parsed.result.records, 'box') : [], [parsed]);
  const masks = useMemo(() => parsed.result ? recordsOfKind(parsed.result.records, 'mask') : [], [parsed]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || !dimensions.width || !dimensions.height) return;
    canvas.width = dimensions.width;
    canvas.height = dimensions.height;
    const context = canvas.getContext('2d');
    if (!context) return;
    context.clearRect(0, 0, canvas.width, canvas.height);
    setDrawError('');

    try {
      if (showMasks) {
        for (const [index, record] of masks.entries()) {
          const raster = decodeMaskToRaster(record.mask);
          const { width, height } = record.mask;
          const maskCanvas = document.createElement('canvas');
          maskCanvas.width = width;
          maskCanvas.height = height;
          const maskContext = maskCanvas.getContext('2d');
          if (!maskContext) continue;
          const pixels = maskContext.createImageData(width, height);
          const red = index % 2 === 0 ? 30 : 235;
          const green = index % 2 === 0 ? 184 : 125;
          for (let pixel = 0; pixel < raster.length; pixel++) {
            if (raster[pixel] !== 1) continue;
            const offset = pixel * 4;
            pixels.data[offset] = red;
            pixels.data[offset + 1] = green;
            pixels.data[offset + 2] = 245;
            pixels.data[offset + 3] = 110;
          }
          maskContext.putImageData(pixels, 0, 0);
          context.drawImage(maskCanvas, record.bounds.left, record.bounds.top,
            record.bounds.right - record.bounds.left,
            record.bounds.bottom - record.bounds.top);
        }
      }
      if (showBoxes) {
        context.lineWidth = Math.max(2, canvas.width / 500);
        context.font = `${Math.max(14, Math.round(canvas.width / 45))}px sans-serif`;
        for (const box of boxes) {
          context.strokeStyle = '#ffcf50';
          context.fillStyle = '#ffcf50';
          context.strokeRect(box.left, box.top, box.right - box.left, box.bottom - box.top);
          const confidence = box.confidence === undefined ? '' : ` ${Math.round(box.confidence * 100)}%`;
          const caption = `Object ${box.objectId}${confidence}`;
          const captionWidth = context.measureText(caption).width + 12;
          const labelHeight = Math.max(21, canvas.width / 32);
          const y = Math.max(0, box.top - labelHeight);
          context.fillRect(box.left, y, captionWidth, labelHeight);
          context.fillStyle = '#102348';
          context.fillText(caption, box.left + 6, y + labelHeight - 6);
        }
      }
    } catch (cause) {
      setDrawError(cause instanceof Error ? cause.message : 'Could not decode SAM mask');
    }
  }, [boxes, masks, dimensions, showBoxes, showMasks]);

  return (
    <div className="sam-viewer">
      <div className="sam-viewer-options">
        <label><input type="checkbox" checked={showMasks} onChange={(e) => setShowMasks(e.target.checked)} /> Masks</label>
        <label><input type="checkbox" checked={showBoxes} onChange={(e) => setShowBoxes(e.target.checked)} /> Boxes</label>
        <span>{boxes.length} object{boxes.length === 1 ? '' : 's'} detected</span>
      </div>
      <div className="sam-image-stage">
        <img src={imageUrl} alt="Storefront with SAM segmentation overlay"
          onLoad={(event) => setDimensions({
            width: event.currentTarget.naturalWidth,
            height: event.currentTarget.naturalHeight,
          })} />
        <canvas ref={canvasRef} aria-label="Meta SAM 3.1 segmentation overlay" />
      </div>
      {boxes.length === 0 && <p className="storefront-muted">SAM reported no matching objects for this concept.</p>}
      {parsed.error && <p className="storefront-error">Could not parse saved SAM response: {parsed.error}</p>}
      {parsed.result && parsed.result.diagnostics.length > 0 && <p className="storefront-error">
        SAM parser reported {parsed.result.diagnostics.length} diagnostic(s). Review these findings before relying on the overlay.
      </p>}
      {drawError && <p className="storefront-error">Mask rendering failed: {drawError}</p>}
      <p className="storefront-muted">Overlay pixels and boxes come from the saved SAM response. They are model estimates, not measurements of physical dimensions.</p>
    </div>
  );
}

export default function SegmentationPanel({ businessId, mediaId, mimeType, sizeBytes }: Props) {
  const [runs, setRuns] = useState<StorefrontSegmentation[]>([]);
  const [concept, setConcept] = useState('storefront sign');
  const [activeId, setActiveId] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let current = true;
    setLoading(true);
    listSegmentations(businessId, mediaId)
      .then((results) => {
        if (!current) return;
        setRuns(results);
        setActiveId(results.find((item) => item.status === 'COMPLETED')?.id ?? null);
      })
      .catch((cause: unknown) => {
        if (current) setError(cause instanceof Error ? cause.message : 'Could not load SAM results');
      })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [businessId, mediaId]);

  const supported = mimeType.startsWith('image/') && sizeBytes <= 8 * 1024 * 1024;
  const active = runs.find((item) => item.id === activeId);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!supported || busy) return;
    setError('');
    setBusy(true);
    try {
      const result = await segmentImage(businessId, mediaId, concept);
      setRuns((current) => [result, ...current.filter((item) => item.id !== result.id)]);
      setActiveId(result.id);
      if (result.status === 'FAILED') setError(result.lastError ?? 'Meta SAM could not complete this request');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Segmentation request failed');
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="sam-panel" aria-label="SAM 3.1 image segmentation">
      <h3><ScanSearch size={18} /> SAM 3.1 Visual Segmentation</h3>
      <p className="storefront-muted">Find precise regions of one visible object type in this photo. Each new concept may use Meta API credits.</p>
      {!supported ? (
        <p className="storefront-muted">Available for PNG/JPEG files up to 8 MB. Video tracking will be added separately.</p>
      ) : (
        <form className="sam-concept-form" onSubmit={(event) => void submit(event)}>
          <label htmlFor={`sam-concept-${mediaId}`}>What should SAM find?</label>
          <div className="sam-concept-inputs">
            <input id={`sam-concept-${mediaId}`} value={concept}
              onChange={(event) => setConcept(event.target.value)}
              maxLength={80} required placeholder="e.g., glass door" />
            <button type="submit" disabled={busy || concept.trim().length < 2}>
              {busy ? <LoaderCircle size={16} className="storefront-spin" /> : <ScanSearch size={16} />}
              {busy ? 'Segmenting…' : 'Segment with SAM 3.1'}
            </button>
          </div>
          <div className="sam-suggestions">
            {['storefront sign', 'glass door', 'window poster'].map((option) => (
              <button type="button" key={option} onClick={() => setConcept(option)} disabled={busy}>{option}</button>
            ))}
          </div>
        </form>
      )}
      {error && <p className="storefront-error" role="alert">{error}</p>}
      {loading && <p className="storefront-muted">Loading saved segmentations…</p>}
      {runs.length > 0 && (
        <div className="sam-results">
          <p className="storefront-muted">Saved segmentation requests. Selecting a result does not call Meta again.</p>
          <div className="sam-result-tabs">
            {runs.map((run) => (
              <button type="button" key={run.id} onClick={() => setActiveId(run.id)}
                className={activeId === run.id ? 'is-active' : ''}>
                {run.concept} · {run.status.toLowerCase()}
              </button>
            ))}
          </div>
          {active && (
            <div className="sam-result-detail">
              <p className="storefront-muted">Model: {active.modelName ?? 'SAM 3.1'} · Response: {active.modelResponseId ?? 'not completed'}</p>
              {active.lastError && <p className="storefront-error">{active.lastError}</p>}
              {active.status === 'PROCESSING' && <p className="storefront-muted">A prior SAM request is still marked processing. Check its backend status before retrying.</p>}
              {active.status === 'COMPLETED' && active.rawOutput !== null && (
                <MaskViewer imageUrl={storefrontFileUrl(businessId, mediaId)} rawOutput={active.rawOutput} />
              )}
            </div>
          )}
        </div>
      )}
    </section>
  );
}
