import { useEffect, useState } from 'react';
import S4LogoAnimation from './S4LogoAnimation';

const INTRO_DURATION_MS = 8200;

/** One intro per document load. Refresh replays it; normal route changes do not. */
export default function S4Intro() {
  const [open, setOpen] = useState(true);

  useEffect(() => {
    if (!open) return;
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reduceMotion) {
      // Static logo appears, with a short opportunity to skip.
      const timeout = window.setTimeout(() => setOpen(false), 700);
      return () => window.clearTimeout(timeout);
    }
    const timeout = window.setTimeout(() => setOpen(false), INTRO_DURATION_MS);
    const skip = () => setOpen(false);
    window.addEventListener('pointerdown', skip, { once: true });
    window.addEventListener('keydown', skip, { once: true });
    return () => {
      window.clearTimeout(timeout);
      window.removeEventListener('pointerdown', skip);
      window.removeEventListener('keydown', skip);
    };
  }, [open]);

  if (!open) return null;

  return (
    <div className="s4-intro" role="presentation" aria-label="S4 introduction. Tap, click or press a key to skip.">
      <div className="s4-intro-grid" aria-hidden="true" />
      <div className="s4-intro-center">
        <S4LogoAnimation logoSrc="/s4-logo.png" maxWidth={530} tilt={false} />
        <div className="s4-intro-caption"><strong>S4</strong> Neighborhood Intelligence</div>
      </div>
      <div className="s4-intro-skip">Tap or click to continue <span aria-hidden="true">↗</span></div>
    </div>
  );
}
