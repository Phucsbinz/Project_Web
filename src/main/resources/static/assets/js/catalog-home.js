'use strict';
(() => {
  document.querySelectorAll('.catalog-homepage [data-home-carousel]').forEach(carousel => {
    const track = carousel.querySelector('.home-product-track');
    const grid = track.querySelector('.product-grid');
    const previous = carousel.querySelector('[data-carousel-prev]');
    const next = carousel.querySelector('[data-carousel-next]');
    const status = carousel.querySelector('[data-carousel-status]');
    const items = Array.from(grid.children).filter(item => item.querySelector('.product-card'));
    let scheduled = false;
    const geometry = () => {
      const gap = parseFloat(getComputedStyle(grid).columnGap) || 0;
      const step = (items[0]?.getBoundingClientRect().width || grid.clientWidth) + gap;
      return {step, columns: Math.max(1, Math.round((grid.clientWidth + gap) / step))};
    };
    function update() {
      scheduled = false;
      if (!track.clientWidth) return;
      const maximum = Math.max(0, track.scrollWidth - track.clientWidth);
      previous.disabled = maximum <= 1 || track.scrollLeft <= 1;
      next.disabled = maximum <= 1 || track.scrollLeft >= maximum - 1;
      let label = items.length ? items.length + ' món' : 'Chưa có món phù hợp';
      if (items.length && maximum > 1) {
        const {step, columns} = geometry();
        const first = Math.round(track.scrollLeft / step) * 2 + 1;
        label = 'Món ' + first + '–' + Math.min(items.length, first + columns * 2 - 1) + ' / ' + items.length;
      }
      if (status.textContent !== label) status.textContent = label;
    }
    function schedule() {
      if (!scheduled) { scheduled = true; requestAnimationFrame(update); }
    }
    function move(direction, singleColumn = false) {
      const {step, columns} = geometry();
      track.scrollBy({left: direction * step * (singleColumn ? 1 : columns),
        behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'});
    }
    previous.addEventListener('click', () => move(-1));
    next.addEventListener('click', () => move(1));
    track.addEventListener('scroll', schedule, {passive: true});
    track.addEventListener('keydown', event => {
      if (event.target !== track) return;
      if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
        event.preventDefault(); move(event.key === 'ArrowLeft' ? -1 : 1, true);
      } else if (event.key === 'Home' || event.key === 'End') {
        event.preventDefault(); track.scrollTo({left: event.key === 'Home' ? 0 : track.scrollWidth,
          behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'});
      }
    });
    document.getElementById('home-drinks').addEventListener('shown.bs.tab', schedule);
    if ('ResizeObserver' in window) new ResizeObserver(schedule).observe(track);
    window.addEventListener('resize', schedule);
    update();
  });
})();
