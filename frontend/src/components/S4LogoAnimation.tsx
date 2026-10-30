"use client"; // needed in Next.js App Router; harmless in Vite / CRA

/**
 * S4LogoAnimation
 * ---------------------------------------------------------------
 * "Dots -> Logo" animation: ~150 glass beads swirl in, snap together
 * into the S4 logo, a light glint + sparkles play, then the beads
 * scatter and the loop repeats. Pure <canvas> + React, no libraries.
 *
 * SETUP
 *  Nothing to upload: the S4 logo is embedded in this file (S4_LOGO_DATA_URI).
 *  Just use it anywhere:  <S4LogoAnimation />
 *  (Optional) pass logoSrc="/my-logo.png" to use a different logo.
 *
 * NOTES FOR AN AI ASSISTANT READING THIS FILE
 *  - The logo image's alpha channel is sampled to decide where each bead
 *    lands, so any custom logo MUST be a PNG/WebP with a transparent
 *    background served from the same origin (or a data: URI).
 *  - Everything runs inside one useEffect; the cleanup cancels the
 *    animation frame, observers and listeners.
 *  - Light/tilt/sparkles can be switched off with props.
 *  - Respects prefers-reduced-motion (shows the static logo).
 */

import { useEffect, useRef } from "react";

// Embedded S4 logo (transparent WebP). Do not edit.
const S4_LOGO_DATA_URI =
  "data:image/webp;base64,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";

export type S4LogoAnimationProps = {
  /** Optional custom logo (transparent PNG/WebP, same origin or data: URI). Defaults to the embedded S4 logo. */
  logoSrc?: string;
  /** Max rendered width in px. */
  maxWidth?: number;
  /** Grid spacing for bead targets: smaller = more beads (24 ≈ 150). */
  countStep?: number;
  /** Base bead radius in canvas units. */
  ballRadius?: number;
  /** Full loop length in ms. */
  loopMs?: number;
  /** 3D tilt following the pointer. */
  tilt?: boolean;
  /** Twinkles after the logo forms. */
  sparkles?: boolean;
  className?: string;
};

type RGB = [number, number, number];

interface Part {
  r1: number; a1: number;          // final position (polar, around centre)
  r0: number; sw: number;          // start radius / swirl amount (coming in)
  r2: number; sw2: number;         // end radius / swirl amount (going out)
  d1: number; d2: number;          // stagger delays in / out (ms)
  z: number;                       // depth (drives size + blur while flying)
  s: number;                       // bead radius
  spr: HTMLCanvasElement[];        // [sharp, soft, blurry] sprites
}

interface BeadState {
  x: number; y: number; s: number; a: number;
  fly: boolean; h: number; b: number; p: Part;
}

const TAU = Math.PI * 2;
const S = 600;          // logical canvas size
const L = 110;          // logo offset
const LS = 380;         // logo size
const CX = 300;
const CY = 300;
const SP = 128;         // bead sprite size
const IN_DUR = 1700;
const OUT_START = 5900;
const OUT_DUR = 1700;

const PALE: RGB[] = [[170, 174, 246], [190, 178, 250], [150, 166, 240], [178, 190, 248]];
const SPARK: [number, number, number, number][] = [
  [196, 196, 3300, 15], [428, 192, 3500, 20], [412, 424, 3750, 14], [200, 410, 3950, 12],
];

const clamp = (t: number) => Math.max(0, Math.min(1, t));
const easeInOut = (t: number) => { t = clamp(t); return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2; };
const easeIn = (t: number) => { t = clamp(t); return t * t * t; };
const easeOutBack = (t: number) => { t = clamp(t); const c1 = 1.25, c3 = c1 + 1; return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2); };

function mix(c: RGB, k: number): string {
  const o = c.map((v) => Math.round(k > 0 ? v + (255 - v) * k : v * (1 + k)));
  return `rgb(${o.join(",")})`;
}

function roundRect(g: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
  g.beginPath(); g.moveTo(x + r, y);
  g.arcTo(x + w, y, x + w, y + h, r); g.arcTo(x + w, y + h, x, y + h, r);
  g.arcTo(x, y + h, x, y, r); g.arcTo(x, y, x + w, y, r); g.closePath();
}

/** Glass bead: diffuse + bounce light + fresnel rim + window reflection. */
function makeSprite(c: RGB): HTMLCanvasElement {
  const cs = document.createElement("canvas");
  cs.width = cs.height = SP;
  const g = cs.getContext("2d")!;
  const m = SP / 2, R = SP * 0.375;

  g.save();
  g.beginPath(); g.arc(m, m, R, 0, TAU); g.clip();
  const b = g.createRadialGradient(m - R * 0.38, m - R * 0.42, R * 0.04, m - R * 0.1, m - R * 0.1, R * 1.25);
  b.addColorStop(0, mix(c, 0.5)); b.addColorStop(0.3, mix(c, 0.12));
  b.addColorStop(0.62, mix(c, -0.08)); b.addColorStop(1, mix(c, -0.55));
  g.fillStyle = b; g.fillRect(0, 0, SP, SP);

  const bo = g.createRadialGradient(m + R * 0.25, m + R * 0.62, 0, m + R * 0.25, m + R * 0.62, R * 0.8);
  bo.addColorStop(0, mix(c, 0.55).replace("rgb", "rgba").replace(")", ",.55)"));
  bo.addColorStop(1, "rgba(255,255,255,0)");
  g.fillStyle = bo; g.fillRect(0, 0, SP, SP);

  const fr = g.createRadialGradient(m, m, R * 0.72, m, m, R);
  fr.addColorStop(0, "rgba(255,255,255,0)"); fr.addColorStop(1, "rgba(235,240,255,.38)");
  g.fillStyle = fr; g.fillRect(0, 0, SP, SP);
  g.restore();

  // window (softbox) reflection, two panes
  g.save();
  g.beginPath(); g.arc(m, m, R * 0.97, 0, TAU); g.clip();
  g.translate(m - R * 0.34, m - R * 0.4); g.rotate(-0.62); g.scale(1, 0.78);
  g.shadowColor = "rgba(255,255,255,.9)"; g.shadowBlur = R * 0.07;
  const wg = g.createLinearGradient(0, -R * 0.2, 0, R * 0.2);
  wg.addColorStop(0, "rgba(255,255,255,.98)"); wg.addColorStop(1, "rgba(225,235,255,.55)");
  g.fillStyle = wg;
  roundRect(g, -R * 0.3, -R * 0.17, R * 0.27, R * 0.34, R * 0.07); g.fill();
  roundRect(g, R * 0.02, -R * 0.17, R * 0.27, R * 0.34, R * 0.07); g.fill();
  g.restore();

  // secondary reflection arc + tiny hot spot
  g.save();
  g.beginPath(); g.arc(m, m, R * 0.8, 0.35, 1.25);
  g.strokeStyle = "rgba(255,255,255,.3)"; g.lineWidth = R * 0.07; g.lineCap = "round";
  g.shadowColor = "rgba(255,255,255,.6)"; g.shadowBlur = R * 0.06; g.stroke();
  g.restore();
  g.beginPath(); g.arc(m + R * 0.46, m - R * 0.5, R * 0.045, 0, TAU);
  g.fillStyle = "rgba(255,255,255,.8)"; g.fill();

  g.beginPath(); g.arc(m, m, R - 0.5, 0, TAU);
  g.strokeStyle = "rgba(30,20,90,.18)"; g.lineWidth = 1; g.stroke();
  return cs;
}

/** Cheap blur for depth-of-field: shrink then enlarge (works in every browser). */
function blurred(src: HTMLCanvasElement, f: number): HTMLCanvasElement {
  const w = Math.max(4, Math.round(SP / f));
  const a = document.createElement("canvas"), o = document.createElement("canvas");
  a.width = a.height = w; o.width = o.height = SP;
  const ac = a.getContext("2d")!, oc = o.getContext("2d")!;
  ac.imageSmoothingEnabled = true; oc.imageSmoothingEnabled = true;
  ac.imageSmoothingQuality = "high"; oc.imageSmoothingQuality = "high";
  let cur: HTMLCanvasElement = src, sz = SP;
  while (sz / 2 > w) {
    sz = Math.round(sz / 2);
    const tmp = document.createElement("canvas");
    tmp.width = tmp.height = sz;
    tmp.getContext("2d")!.drawImage(cur, 0, 0, sz, sz);
    cur = tmp;
  }
  ac.drawImage(cur, 0, 0, w, w);
  oc.drawImage(a, 0, 0, SP, SP);
  return o;
}

function makeShadowSprite(): HTMLCanvasElement {
  const s = document.createElement("canvas");
  s.width = s.height = 64;
  const g = s.getContext("2d")!;
  const gr = g.createRadialGradient(32, 32, 0, 32, 32, 32);
  gr.addColorStop(0, "rgba(40,28,110,.95)");
  gr.addColorStop(0.35, "rgba(40,28,110,.55)");
  gr.addColorStop(1, "rgba(40,28,110,0)");
  g.fillStyle = gr; g.fillRect(0, 0, 64, 64);
  return s;
}

export default function S4LogoAnimation({
  logoSrc = S4_LOGO_DATA_URI,
  maxWidth = 560,
  countStep = 24,
  ballRadius = 14,
  loopMs = 8400,
  tilt = true,
  sparkles = true,
  className,
}: S4LogoAnimationProps) {
  const wrapRef = useRef<HTMLDivElement>(null);
  const tiltRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const wrap = wrapRef.current!;
    const tiltEl = tiltRef.current!;
    const cv = canvasRef.current!;
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    cv.width = cv.height = S * dpr;
    const ctx = cv.getContext("2d")!;
    ctx.scale(dpr, dpr);

    const glintCv = document.createElement("canvas");
    glintCv.width = glintCv.height = S;
    const gctx = glintCv.getContext("2d")!;
    const shSpr = makeShadowSprite();

    const img = new Image();
    const parts: Part[] = [];
    let visible = true;
    let t0: number | null = null;
    let raf = 0;
    let cancelled = false;
    let observer: IntersectionObserver | null = null;

    function build() {
      const off = document.createElement("canvas");
      off.width = off.height = S;
      const oc = off.getContext("2d")!;
      oc.drawImage(img, L, L, LS, LS);
      const d = oc.getImageData(0, 0, S, S).data; // needs a same-origin image

      for (let y = L + 10; y < L + LS - 6; y += countStep) {
        for (let x = L + 10; x < L + LS - 6; x += countStep) {
          const jx = Math.round(x + (Math.random() - 0.5) * countStep * 0.35);
          const jy = Math.round(y + (Math.random() - 0.5) * countStep * 0.35);
          const i = (jy * S + jx) * 4;
          if (d[i + 3] < 220) continue;
          let c: RGB = [d[i], d[i + 1], d[i + 2]];
          if ((c[0] + c[1] + c[2]) / 3 > 212) c = PALE[Math.floor(Math.random() * PALE.length)];
          const dx = jx - CX, dy = jy - CY, r1 = Math.hypot(dx, dy);
          const sharp = makeSprite(c);
          parts.push({
            r1, a1: Math.atan2(dy, dx),
            r0: 340 + Math.random() * 130, sw: 2.6 + Math.random() * 2.2,
            r2: 340 + Math.random() * 130, sw2: -(2.2 + Math.random() * 2),
            d1: (r1 / 270) * 350 + Math.random() * 350,
            d2: (1 - r1 / 270) * 250 + Math.random() * 250,
            z: -0.35 + Math.random() * 1.05,
            s: ballRadius * (0.85 + Math.random() * 0.4),
            spr: [sharp, blurred(sharp, 3), blurred(sharp, 7)],
          });
        }
      }
    }

    function stateAt(p: Part, time: number): BeadState {
      let q: number, r: number, a: number, al: number, sz: number, fly: boolean, h: number, bl: number;
      if (time < OUT_START + p.d2) {
        q = clamp((time - p.d1) / IN_DUR);
        r = p.r1 + (p.r0 - p.r1) * (1 - easeOutBack(q));
        a = p.a1 + p.sw * (1 - easeInOut(q));
        al = clamp(q * 5); sz = p.s * (1 + p.z * (1 - q)); fly = q > 0 && q < 1;
        h = 1 - easeInOut(q); bl = clamp(Math.abs(p.z) * 1.5 * (1 - q));
      } else {
        q = clamp((time - OUT_START - p.d2) / OUT_DUR);
        r = p.r1 + (p.r2 - p.r1) * easeIn(q);
        a = p.a1 + p.sw2 * easeInOut(q);
        al = 1 - clamp((q - 0.65) / 0.35); sz = p.s * (1 + p.z * q); fly = q > 0 && q < 1;
        h = easeInOut(q); bl = clamp(Math.abs(p.z) * 1.5 * q);
      }
      return { x: CX + r * Math.cos(a), y: CY + r * Math.sin(a), s: sz, a: al, fly, h, b: bl, p };
    }

    function bead(st: BeadState, alpha: number) {
      const w = (SP * st.s) / (SP * 0.375);
      const x = st.x - w / 2, y = st.y - w / 2;
      const pos = st.b * 2, i = Math.min(1, Math.floor(pos)), f = pos - i;
      ctx.globalAlpha = alpha * (1 - f);
      ctx.drawImage(st.p.spr[i], x, y, w, w);
      if (f > 0.02) { ctx.globalAlpha = alpha * f; ctx.drawImage(st.p.spr[i + 1], x, y, w, w); }
    }

    function shadow(st: BeadState, alpha: number) {
      const h = st.h, w = st.s * 2.9 * (1 + h * 0.55);
      const ox = st.s * (0.32 + 1.5 * h), oy = st.s * (0.5 + 2.1 * h);
      ctx.globalAlpha = alpha * (0.45 - 0.33 * h);
      ctx.drawImage(shSpr, st.x + ox - w / 2, st.y + oy - w * 0.45, w, w * 0.9);
    }

    function star(x: number, y: number, s: number) {
      ctx.save();
      ctx.translate(x, y); ctx.globalAlpha = 1;
      ctx.fillStyle = "#fff"; ctx.shadowColor = "rgba(150,130,255,.95)"; ctx.shadowBlur = 14;
      const k = s * 0.12;
      ctx.beginPath(); ctx.moveTo(0, -s);
      ctx.quadraticCurveTo(k, -k, s, 0); ctx.quadraticCurveTo(k, k, 0, s);
      ctx.quadraticCurveTo(-k, k, -s, 0); ctx.quadraticCurveTo(-k, -k, 0, -s);
      ctx.fill(); ctx.restore();
    }

    function frame(now: number) {
      raf = requestAnimationFrame(frame);
      if (!visible) return;
      if (t0 === null) t0 = now;
      const t = (now - t0) % loopMs;
      ctx.globalAlpha = 1;
      ctx.clearRect(0, 0, S, S);

      const logoA = t < 2300 ? 0 : t < 3000 ? clamp((t - 2300) / 700) : t < 5600 ? 1 : 1 - clamp((t - 5600) / 500);
      const ballA = t < 2500 ? 1 : t < 3100 ? 1 - clamp((t - 2500) / 600) : t < 5600 ? 0 : t < 6000 ? clamp((t - 5600) / 400) : 1;
      const sc = t >= 2300 && t < 3100 ? 0.93 + 0.07 * easeOutBack((t - 2300) / 800) : 1;
      const dy = logoA > 0 ? Math.sin(now / 850) * 4 * clamp((t - 3000) / 600) : 0;

      if (logoA > 0) {
        // soft glow
        const pulse = 0.85 + 0.15 * Math.sin(now / 700);
        const gl = ctx.createRadialGradient(CX, CY, 40, CX, CY, 300);
        gl.addColorStop(0, `rgba(150,130,255,${0.15 * logoA * pulse})`);
        gl.addColorStop(1, "rgba(150,130,255,0)");
        ctx.fillStyle = gl; ctx.fillRect(0, 0, S, S);

        // ground shadow (wide soft + tight dark)
        ctx.save();
        ctx.translate(CX, L + LS + 20 - dy * 0.4); ctx.scale(1, 0.1);
        const sh = ctx.createRadialGradient(0, 0, 0, 0, 0, 185);
        sh.addColorStop(0, `rgba(60,50,170,${0.30 * logoA})`);
        sh.addColorStop(0.5, `rgba(60,50,170,${0.12 * logoA})`);
        sh.addColorStop(1, "rgba(60,50,170,0)");
        ctx.fillStyle = sh; ctx.beginPath(); ctx.arc(0, 0, 185, 0, TAU); ctx.fill();
        const sh2 = ctx.createRadialGradient(0, -20, 0, 0, -20, 110);
        sh2.addColorStop(0, `rgba(30,20,100,${0.30 * logoA})`);
        sh2.addColorStop(1, "rgba(30,20,100,0)");
        ctx.fillStyle = sh2; ctx.beginPath(); ctx.arc(0, -20, 110, 0, TAU); ctx.fill();
        ctx.restore();

        // logo + glint
        ctx.save();
        ctx.translate(CX, CY + dy); ctx.scale(sc, sc); ctx.translate(-CX, -CY);
        ctx.globalAlpha = logoA;
        ctx.shadowColor = "rgba(90,80,200,.25)"; ctx.shadowBlur = 30; ctx.shadowOffsetY = 12;
        ctx.drawImage(img, L, L, LS, LS);
        ctx.shadowColor = "transparent";
        const gp = clamp((t - 3300) / 1000);
        if (gp > 0 && gp < 1) {
          gctx.globalCompositeOperation = "source-over";
          gctx.clearRect(0, 0, S, S);
          gctx.drawImage(img, L, L, LS, LS);
          gctx.globalCompositeOperation = "source-atop";
          gctx.save(); gctx.translate(CX, CY); gctx.rotate(-0.6);
          const gx = -430 + gp * 860;
          const lg = gctx.createLinearGradient(gx - 80, 0, gx + 80, 0);
          lg.addColorStop(0, "rgba(255,255,255,0)");
          lg.addColorStop(0.5, "rgba(255,255,255,.75)");
          lg.addColorStop(1, "rgba(255,255,255,0)");
          gctx.fillStyle = lg; gctx.fillRect(-500, -500, 1000, 1000);
          gctx.restore();
          ctx.drawImage(glintCv, 0, 0, S, S);
        }
        ctx.restore();
      }

      // impact: bloom + ripple ring
      const ik = (t - 2450) / 1000;
      if (ik > 0 && ik < 1) {
        const bl = ctx.createRadialGradient(CX, CY, 20, CX, CY, 120 + 280 * ik);
        bl.addColorStop(0, `rgba(170,150,255,${0.30 * (1 - ik)})`);
        bl.addColorStop(1, "rgba(170,150,255,0)");
        ctx.globalAlpha = 1; ctx.fillStyle = bl; ctx.fillRect(0, 0, S, S);
        ctx.beginPath(); ctx.arc(CX, CY, 150 + 140 * (1 - Math.pow(1 - ik, 3)), 0, TAU);
        ctx.strokeStyle = `rgba(140,125,255,${0.55 * (1 - ik)})`;
        ctx.lineWidth = 1 + 4 * (1 - ik); ctx.stroke();
      }

      // beads: trails first, then back-to-front so each bead shades its neighbours
      if (ballA > 0) {
        const arr: BeadState[] = [];
        for (const p of parts) {
          const st = stateAt(p, t);
          if (st.a * ballA < 0.01) continue;
          arr.push(st);
          if (st.fly) {
            for (let k = 3; k >= 1; k--) {
              const tr = stateAt(p, t - k * 26);
              tr.s = st.s * (1 - 0.07 * k);
              tr.b = Math.max(tr.b, 0.25 * k);
              bead(tr, (st.a * ballA * 0.13 * (4 - k)) / 3);
            }
          }
        }
        arr.sort((a, b) => b.x + b.y - (a.x + a.y));
        for (const st of arr) {
          shadow(st, st.a * ballA);
          bead(st, st.a * ballA);
        }
      }

      // sparkles
      if (sparkles && logoA > 0.9) {
        for (const sp of SPARK) {
          const sk = (t - sp[2]) / 750;
          if (sk > 0 && sk < 1) star(sp[0], sp[1] + dy, sp[3] * Math.sin(sk * Math.PI));
        }
      }
      ctx.globalAlpha = 1;
    }

    function onMove(e: PointerEvent) {
      const r = wrap.getBoundingClientRect();
      const x = (e.clientX - r.left) / r.width - 0.5;
      const y = (e.clientY - r.top) / r.height - 0.5;
      tiltEl.style.transform = `rotateX(${(-y * 14).toFixed(2)}deg) rotateY(${(x * 14).toFixed(2)}deg)`;
    }
    function onLeave() {
      tiltEl.style.transform = "rotateX(0deg) rotateY(0deg)";
    }

    img.onload = () => {
      if (cancelled) return;
      try {
        build();
      } catch (err) {
        console.error("S4LogoAnimation: could not read the logo image. Use a same-origin PNG/WebP or a data: URI.", err);
        return;
      }
      const calm = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
      if (calm) { ctx.drawImage(img, L, L, LS, LS); return; } // static logo, no motion
      if ("IntersectionObserver" in window) {
        observer = new IntersectionObserver((e) => { visible = e[0].isIntersecting; });
        observer.observe(wrap);
      }
      if (tilt) {
        wrap.addEventListener("pointermove", onMove);
        wrap.addEventListener("pointerleave", onLeave);
      }
      raf = requestAnimationFrame(frame);
    };
    img.onerror = () => console.error("S4LogoAnimation: logo failed to load:", logoSrc.slice(0, 80));
    img.src = logoSrc;

    return () => {
      cancelled = true;
      cancelAnimationFrame(raf);
      observer?.disconnect();
      wrap.removeEventListener("pointermove", onMove);
      wrap.removeEventListener("pointerleave", onLeave);
    };
  }, [logoSrc, countStep, ballRadius, loopMs, tilt, sparkles]);

  return (
    <div
      ref={wrapRef}
      className={className}
      style={{ width: "100%", maxWidth, aspectRatio: "1 / 1", margin: "0 auto", perspective: 900 }}
    >
      <div
        ref={tiltRef}
        style={{ width: "100%", height: "100%", transition: "transform .25s ease-out", willChange: "transform" }}
      >
        <canvas ref={canvasRef} style={{ width: "100%", height: "100%", display: "block" }} />
      </div>
    </div>
  );
}
