import { useId } from 'react';

type Place = {
  name: string;
  x: number;
  y: number;
  primary?: boolean;
  labelX: number;
  labelY: number;
};

// Locations are approximate, in a decorative LA-area scene. This is not a street map or a navigation route.
const places: Place[] = [
  { name: 'Downtown LA', x: 242, y: 124, labelX: 174, labelY: 90 },
  { name: 'Hollywood', x: 182, y: 91, labelX: 94, labelY: 51 },
  { name: 'Downey', x: 365, y: 200, labelX: 395, labelY: 177 },
  { name: 'Golden Brows', x: 326, y: 244, labelX: 351, labelY: 266, primary: true },
  { name: 'Long Beach', x: 282, y: 336, labelX: 184, labelY: 365 },
  { name: 'Anaheim', x: 486, y: 295, labelX: 494, labelY: 258 },
];

const route = 'M182 91 C196 104 221 114 242 124 S332 172 365 200 Q347 225 326 244 C307 275 290 307 282 336 C353 338 428 315 486 295';
const mainRoads = [
  'M36 108 C142 110 246 124 583 143',
  'M37 180 C169 175 327 177 592 200',
  'M15 275 C166 255 369 263 603 289',
  'M61 355 C180 339 349 349 553 365',
  'M119 34 Q128 185 177 395',
  'M252 18 C248 115 271 269 324 415',
  'M378 17 Q358 163 431 412',
  'M502 48 Q488 201 526 382',
];
const minorRoads = [
  'M30 63 L570 92', 'M33 226 L586 235', 'M43 314 L585 320',
  'M70 27 L213 399', 'M176 22 L260 411', 'M326 17 L374 413',
  'M451 35 L472 394', 'M564 99 L555 367',
];

/** Cinematic map of the Los Angeles area. The real Neighborhood Explorer remains the geographic source of truth. */
export default function CaliforniaNetwork({ businessName }: { businessName?: string }) {
  const uid = useId().replace(/:/g, '');
  const label = businessName?.trim() || 'Your business';
  return (
    <section className="s4-network-panel" aria-labelledby={`s4-la-heading-${uid}`}>
      <div className="s4-network-copy">
        <span className="s4-network-eyebrow">NEIGHBORHOOD INTELLIGENCE</span>
        <h2 id={`s4-la-heading-${uid}`}>Your neighborhood. In focus.</h2>
        <p>Real research. Local opportunities. A clearer next move.</p>
        <div className="s4-network-anchor"><span className="s4-network-pulse" /> PARAMOUNT · LOS ANGELES</div>
        <span className="s4-network-note">Animated LA-area illustration · Not a live map or driving route</span>
      </div>
      <div className="s4-network-stage">
        <div className="s4-map-aura" />
        <svg className="s4-la-plate" viewBox="0 0 630 445" role="img" aria-label={`Illustrative 3D Los Angeles map, with ${label} highlighted in Paramount and five neighboring destinations`}>
          <defs>
            <linearGradient id={`s4-terrain-${uid}`} x1=".13" y1="0" x2=".84" y2="1"><stop stopColor="#fff"/><stop offset=".53" stopColor="#e9f1ff"/><stop offset="1" stopColor="#d9e3fc"/></linearGradient>
            <linearGradient id={`s4-edge-${uid}`} x1="0" y1="0" x2="0" y2="1"><stop stopColor="#c4d2f1"/><stop offset="1" stopColor="#8397c8"/></linearGradient>
            <linearGradient id={`s4-road-${uid}`} x1="0" y1="0" x2="1" y2="1"><stop stopColor="#5f72e8"/><stop offset="1" stopColor="#9a79ee"/></linearGradient>
            <radialGradient id={`s4-pin-${uid}`} cx="30%" cy="18%" r="85%"><stop stopColor="#f7f8ff"/><stop offset=".24" stopColor="#8a9efa"/><stop offset=".68" stopColor="#5364d2"/><stop offset="1" stopColor="#293b91"/></radialGradient>
            <filter id={`s4-plate-shadow-${uid}`} x="-40%" y="-40%" width="180%" height="200%"><feDropShadow dx="1" dy="23" stdDeviation="17" floodColor="#243670" floodOpacity=".24"/></filter>
            <filter id={`s4-route-glow-${uid}`} x="-35%" y="-35%" width="170%" height="170%"><feGaussianBlur stdDeviation="5" result="blur"/><feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
            <clipPath id={`s4-plate-clip-${uid}`}><rect x="25" y="22" width="575" height="382" rx="32" /></clipPath>
          </defs>
          <g className="s4-la-board">
            <rect x="25" y="43" width="575" height="382" rx="32" fill={`url(#s4-edge-${uid})`} filter={`url(#s4-plate-shadow-${uid})`}/>
            <rect x="25" y="22" width="575" height="382" rx="32" fill={`url(#s4-terrain-${uid})`} stroke="#fff" strokeWidth="3" />
            <g clipPath={`url(#s4-plate-clip-${uid})`}>
              <path d="M-9 310 C92 263 140 311 150 430 L-9 450Z" fill="#d1e8fa" opacity=".72" />
              <path d="M67 -12 C115 76 112 198 78 277" stroke="#d3e1f6" strokeWidth="54" fill="none" opacity=".45"/>
              <path d="M360 -16 C350 126 448 256 512 429" stroke="#e0e7f7" strokeWidth="62" fill="none" opacity=".52"/>
              {minorRoads.map((road, index) => <path key={`minor-${index}`} d={road} fill="none" stroke="#fff" strokeWidth="8" opacity=".9" />)}
              {mainRoads.map((road, index) => <path key={`main-shadow-${index}`} d={road} fill="none" stroke="#c9d3ec" strokeWidth="16" opacity=".82" />)}
              {mainRoads.map((road, index) => <path key={`main-${index}`} d={road} fill="none" stroke="#fff" strokeWidth="10" opacity=".97" />)}
              {Array.from({ length: 15 }, (_,i) => <path key={`block-${i}`} d={`M${62 + (i%5)*103} ${70+Math.floor(i/5)*108}l40 4 7 27 -42 -4Z`} fill="#dce6f8" opacity=".57"/>)}
              <path d={route} className="s4-la-route-halo" filter={`url(#s4-route-glow-${uid})`} />
              <path d={route} className="s4-la-route" stroke={`url(#s4-road-${uid})`} />
            </g>
            {places.map((place, index) => (
              <g className={`s4-la-location ${place.primary ? 's4-la-location-primary' : ''}`} key={place.name} style={{ animationDelay: `${.85 + index * .22}s` }}>
                <circle cx={place.x} cy={place.y+8} r={place.primary ? 19 : 13} className="s4-la-pin-shadow" />
                <circle cx={place.x} cy={place.y} r={place.primary ? 17 : 11} fill={`url(#s4-pin-${uid})`} stroke="#fff" strokeWidth="4" />
                <circle cx={place.x-3.2} cy={place.y-4} r={place.primary ? 5 : 3} fill="#fff" opacity=".92" />
                {place.primary && <circle cx={place.x} cy={place.y} r="24" className="s4-la-primary-ring"/>}
                <g className="s4-la-place-label" transform={`translate(${place.labelX},${place.labelY})`}>
                  <rect x="-7" y="-17" width={place.primary ? 117 : Math.max(66,place.name.length*6.8 + 12)} height="27" rx="13" fill={place.primary ? '#293d85' : '#fff'} stroke={place.primary ? '#819aff' : '#d9e2f5'} strokeWidth="1.2" />
                  <text x="6" y="1" fontSize="11" fontWeight={place.primary ? 800 : 650} fill={place.primary ? '#fff' : '#3a4f80'}>{place.name}</text>
                </g>
              </g>
            ))}
          </g>
        </svg>
        <div className="s4-network-glass-label"><span className="s4-network-dot" /> <span className="s4-network-label-main">{label}</span><span className="s4-network-label-sub">Paramount, CA</span></div>
      </div>
    </section>
  );
}
