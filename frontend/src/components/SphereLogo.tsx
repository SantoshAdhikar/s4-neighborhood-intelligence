import { useEffect, useRef } from 'react';

type Bead = { x: number; y: number; angle: number; radius: number; delay: number; size: number; color: [number, number, number]; sprite: HTMLCanvasElement };
const SIZE = 600;
const TAU = Math.PI * 2;
const clamp = (v: number) => Math.max(0, Math.min(1, v));
const smooth = (v: number) => { const t = clamp(v); return t * t * (3 - 2 * t); };
const back = (v: number) => { const t = clamp(v) - 1; return 1 + 2.25 * t * t * t + 1.25 * t * t; };

function beadSprite(color: [number, number, number]) {
  const canvas = document.createElement('canvas');
  canvas.width = canvas.height = 80;
  const c = canvas.getContext('2d')!;
  const [r, g, b] = color;
  const glow = c.createRadialGradient(40, 40, 20, 40, 40, 40);
  glow.addColorStop(0, `rgba(${r},${g},${b},.2)`);
  glow.addColorStop(1, `rgba(${r},${g},${b},0)`);
  c.fillStyle = glow; c.fillRect(0, 0, 80, 80);
  c.beginPath(); c.arc(40, 40, 29, 0, TAU);
  const body = c.createRadialGradient(29, 25, 3, 45, 49, 42);
  body.addColorStop(0, '#ffffff');
  body.addColorStop(.22, `rgb(${Math.round((r + 255) / 2)},${Math.round((g + 255) / 2)},${Math.round((b + 255) / 2)})`);
  body.addColorStop(.58, `rgb(${r},${g},${b})`);
  body.addColorStop(1, `rgb(${Math.round(r * .35)},${Math.round(g * .35)},${Math.round(b * .53)})`);
  c.fillStyle = body; c.fill();
  c.lineWidth = 1.7; c.strokeStyle = 'rgba(255,255,255,.65)'; c.stroke();
  const shine = c.createRadialGradient(29, 25, 1, 29, 25, 16);
  shine.addColorStop(0, 'rgba(255,255,255,.95)'); shine.addColorStop(1, 'rgba(255,255,255,0)');
  c.fillStyle = shine; c.beginPath(); c.arc(29, 25, 16, 0, TAU); c.fill();
  return canvas;
}

/** Claude-style glossy bead assembly, using the exact S4 artwork rather than redrawing its shape. */
export default function SphereLogo() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const tiltRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    canvas.width = canvas.height = SIZE * dpr;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    const img = new Image();
    const mask = document.createElement('canvas');
    mask.width = mask.height = SIZE;
    const maskCtx = mask.getContext('2d')!;
    const glint = document.createElement('canvas');
    glint.width = glint.height = SIZE;
    const glintCtx = glint.getContext('2d')!;
    let raf = 0;
    let started = 0;
    let visible = true;
    let disposed = false;
    let beads: Bead[] = [];
    let seed = 4921;
    const random = () => { seed = (seed * 1664525 + 1013904223) >>> 0; return seed / 4294967296; };
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const view = new IntersectionObserver(entries => { visible = entries[0]?.isIntersecting ?? true; }, { threshold: 0.02 });
    view.observe(canvas);

    function drawBead(p: Bead, t: number, opacity: number) {
      if (!ctx || opacity <= 0) return;
      const exit = t >= 5900;
      const q = exit ? smooth((t - 5900 - p.delay * .25) / 1650) : clamp((t - p.delay) / 1750);
      const radius = exit ? p.radius + (390 - p.radius) * q : p.radius + (400 - p.radius) * (1 - back(q));
      const angle = p.angle + (exit ? -3.2 * smooth(q) : 4.8 * (1 - smooth(q)));
      const x = 300 + radius * Math.cos(angle);
      const y = 300 + radius * Math.sin(angle);
      const dimension = p.size * (exit ? 1 + q * .3 : 1 + (1 - q) * .3);
      ctx.globalAlpha = opacity * (exit ? 1 - smooth((q - .7) / .3) : smooth(q * 5));
      ctx.drawImage(p.sprite, x - dimension / 2, y - dimension / 2, dimension, dimension);
    }

    function draw(now: number) {
      if (disposed || !ctx) return;
      raf = requestAnimationFrame(draw);
      if (!visible) { started += 16; return; }
      if (!started) started = now;
      const t = (now - started) % 8400;
      ctx.clearRect(0, 0, SIZE, SIZE);
      const alpha = t < 2300 ? 0 : t < 3000 ? smooth((t - 2300) / 700) : t < 5600 ? 1 : 1 - smooth((t - 5600) / 500);
      const beadAlpha = t < 2500 ? 1 : t < 3100 ? 1 - smooth((t - 2500) / 600) : t < 5600 ? 0 : t < 6000 ? smooth((t - 5600) / 400) : 1;
      if (alpha > 0) {
        ctx.save();
        const shadow = ctx.createRadialGradient(300, 501, 4, 300, 501, 160);
        shadow.addColorStop(0, `rgba(71,67,147,${.23 * alpha})`); shadow.addColorStop(1, 'rgba(71,67,147,0)');
        ctx.fillStyle = shadow; ctx.beginPath(); ctx.ellipse(300, 500, 160, 20, 0, 0, TAU); ctx.fill();
        ctx.globalAlpha = alpha;
        ctx.shadowColor = 'rgba(76,68,151,.27)'; ctx.shadowBlur = 27; ctx.shadowOffsetY = 13;
        ctx.drawImage(img, 105, 105, 390, 390);
        ctx.shadowColor = 'transparent';
        const phase = (t - 3200) / 1000;
        if (phase > 0 && phase < 1) {
          glintCtx.clearRect(0, 0, SIZE, SIZE);
          glintCtx.globalCompositeOperation = 'source-over';
          glintCtx.drawImage(img, 105, 105, 390, 390);
          glintCtx.globalCompositeOperation = 'source-atop';
          const x = -130 + phase * 850;
          const shine = glintCtx.createLinearGradient(x - 110, 0, x + 110, 0);
          shine.addColorStop(0, 'rgba(255,255,255,0)'); shine.addColorStop(.5, 'rgba(255,255,255,.78)'); shine.addColorStop(1, 'rgba(255,255,255,0)');
          glintCtx.save(); glintCtx.translate(300, 300); glintCtx.rotate(-.55); glintCtx.fillStyle = shine; glintCtx.fillRect(-440, -440, 880, 880); glintCtx.restore();
          ctx.drawImage(glint, 0, 0);
        }
        ctx.restore();
      }
      if (beadAlpha > 0) beads.forEach(p => drawBead(p, t, beadAlpha));
      ctx.globalAlpha = 1;
    }

    img.onload = () => {
      if (disposed) return;
      if (reduced) { ctx.drawImage(img, 105, 105, 390, 390); return; }
      maskCtx.clearRect(0, 0, SIZE, SIZE);
      maskCtx.drawImage(img, 105, 105, 390, 390);
      const pixels = maskCtx.getImageData(0, 0, SIZE, SIZE).data;
      const candidates: Array<{ x: number; y: number; color: [number, number, number] }> = [];
      for (let y = 115; y < 485; y += 8) for (let x = 115; x < 485; x += 8) {
        const i = (y * SIZE + x) * 4;
        if (pixels[i + 3] > 170 && pixels[i] + pixels[i + 1] + pixels[i + 2] < 710) {
          candidates.push({ x, y, color: [pixels[i], pixels[i + 1], pixels[i + 2]] });
        }
      }
      if (!candidates.length) { ctx.drawImage(img, 105, 105, 390, 390); return; }
      const sprites = new Map<string, HTMLCanvasElement>();
      beads = Array.from({ length: 150 }, (_, index) => {
        const point = candidates[Math.floor(random() * candidates.length)];
        const key = point.color.map(v => Math.round(v / 35) * 35).join(',');
        let sprite = sprites.get(key);
        if (!sprite) { sprite = beadSprite(point.color); sprites.set(key, sprite); }
        return { x: point.x, y: point.y, radius: Math.hypot(point.x - 300, point.y - 300), angle: Math.atan2(point.y - 300, point.x - 300), delay: random() * 600 + index % 5 * 45, size: 22 + random() * 13, color: point.color, sprite };
      });
      raf = requestAnimationFrame(draw);
    };
    img.src = '/s4-brand.png';

    const tilt = tiltRef.current;
    const pointerMove = (event: PointerEvent) => {
      if (!tilt || reduced || event.pointerType !== 'mouse') return;
      const bounds = tilt.getBoundingClientRect();
      const x = (event.clientX - bounds.left) / bounds.width - .5;
      const y = (event.clientY - bounds.top) / bounds.height - .5;
      tilt.style.transform = `rotateX(${-y * 10}deg) rotateY(${x * 10}deg)`;
    };
    const pointerLeave = () => { if (tilt) tilt.style.transform = ''; };
    tilt?.addEventListener('pointermove', pointerMove);
    tilt?.addEventListener('pointerleave', pointerLeave);
    return () => { disposed = true; cancelAnimationFrame(raf); img.onload = null; view.disconnect(); tilt?.removeEventListener('pointermove', pointerMove); tilt?.removeEventListener('pointerleave', pointerLeave); };
  }, []);
  return <div ref={tiltRef} style={{ width: '100%', maxWidth: 460, margin: 'auto', aspectRatio: '1', perspective: 900, transition: 'transform 240ms ease-out', willChange: 'transform' }}><canvas ref={canvasRef} role="img" aria-label="S4 glossy spheres forming the S4 logo" style={{ display: 'block', width: '100%', height: '100%' }} /></div>;
}
