import { useEffect, useRef } from 'react';

/** Desktop-only pointer enhancement. Mouse, keyboard, and touch still work normally. */
export default function GlassCursor() {
  const head = useRef<HTMLDivElement>(null);
  const tail = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const finePointer = window.matchMedia('(pointer:fine) and (hover:hover)');
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    if (!finePointer.matches || reducedMotion.matches) return;

    let tx = -100;
    let ty = -100;
    let hx = -100;
    let hy = -100;
    let sx = -100;
    let sy = -100;
    let raf = 0;
    let visible = false;

    const move = (event: PointerEvent) => {
      tx = event.clientX;
      ty = event.clientY;
      if (!visible) {
        hx = sx = tx;
        hy = sy = ty;
        head.current?.classList.add('is-visible');
        tail.current?.classList.add('is-visible');
        visible = true;
      }
      const element = event.target instanceof Element ? event.target : null;
      const interactive = Boolean(element?.closest('button,a,summary,[role="button"],input,textarea,select'));
      head.current?.classList.toggle('is-interactive', interactive);
    };
    const hide = () => {
      head.current?.classList.remove('is-visible');
      tail.current?.classList.remove('is-visible');
      visible = false;
    };
    const animate = () => {
      hx += (tx - hx) * .32;
      hy += (ty - hy) * .32;
      sx += (hx - sx) * .17;
      sy += (hy - sy) * .17;
      if (head.current) head.current.style.transform = `translate3d(${hx}px,${hy}px,0) translate(-50%,-50%)`;
      if (tail.current) tail.current.style.transform = `translate3d(${sx}px,${sy}px,0) translate(-50%,-50%)`;
      raf = requestAnimationFrame(animate);
    };
    window.addEventListener('pointermove', move, {passive:true});
    document.addEventListener('pointerleave', hide);
    window.addEventListener('blur', hide);
    raf = requestAnimationFrame(animate);
    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener('pointermove', move);
      document.removeEventListener('pointerleave', hide);
      window.removeEventListener('blur', hide);
    };
  }, []);

  return <>
    <div ref={tail} className="s4-cursor-tail" aria-hidden="true" />
    <div ref={head} className="s4-cursor-head" aria-hidden="true" />
  </>;
}
