// Measures what build.py places by hand. Hebrew: the price's box on the tour page (page.html -> measure.json).
// Other languages (node measure.mjs <lang>, after python3 build.py <lang>): the price on page_<lang>.html, the font
// size that keeps each fitted element inside its width (measure_<lang>.html, window.FITS = {id: [size, width]}), and
// the selection menu's item centre at its fitted size -> measure/<lang>.json.
import { createRequire } from 'node:module'; import fs from 'node:fs';
const { chromium } = createRequire(import.meta.url)('playwright');
const lang = process.argv[2] || 'iw';
const b = await chromium.launch();
const price = async (file) => {
  const p = await b.newPage({ viewport: { width: 400, height: 1400 } });
  await p.goto('file://' + process.cwd() + '/' + file); await p.evaluate(() => document.fonts.ready);
  return p.evaluate(() => { const e = document.getElementById('pg-price').getBoundingClientRect(); return { x: e.left, y: e.top, w: e.width, h: e.height }; });
};
if (lang === 'iw') {
  const r = await price('page.html');
  fs.writeFileSync('measure.json', JSON.stringify(r)); console.log(r);
} else {
  const r = { price: await price(`page_${lang}.html`) };
  const p = await b.newPage({ viewport: { width: 1080, height: 1920 } });
  await p.goto('file://' + process.cwd() + `/measure_${lang}.html`);
  await p.evaluate(() => document.fonts.ready); await p.waitForTimeout(300);
  Object.assign(r, await p.evaluate(() => {
    const fit = {}, natural = {};
    for (const [id, [base, width]] of Object.entries(window.FITS)) {
      const el = document.getElementById(id);
      const keep = el.style.cssText;
      el.style.position = 'static'; el.style.display = 'inline-block'; el.style.width = 'max-content'; el.style.transform = 'none';
      let size = base;
      const w = () => el.getBoundingClientRect().width;
      el.style.fontSize = size + 'px'; natural[id] = Math.round(w());
      while (w() > width && size > 12) { size -= 1; el.style.fontSize = size + 'px'; }
      fit[id] = size;
      el.style.cssText = keep; el.style.fontSize = size + 'px';
    }
    const m = document.getElementById('an-menu').getBoundingClientRect(), i = document.getElementById('an-item').getBoundingClientRect();
    return { fit, natural, item: { x: Math.round(i.left + i.width / 2 - m.left), y: Math.round(i.top + i.height / 2 - m.top) } };
  }));
  fs.mkdirSync('measure', { recursive: true });
  fs.writeFileSync(`measure/${lang}.json`, JSON.stringify(r, null, 1)); console.log(lang, JSON.stringify(r.fit), r.item);
}
await b.close();
