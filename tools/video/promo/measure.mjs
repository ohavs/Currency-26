import { createRequire } from 'node:module'; import fs from 'node:fs';
const { chromium } = createRequire(import.meta.url)('playwright');
const b = await chromium.launch(); const p = await b.newPage({ viewport: { width: 400, height: 1400 } });
await p.goto('file://' + process.cwd() + '/page.html'); await p.evaluate(() => document.fonts.ready);
const r = await p.evaluate(() => { const e = document.getElementById('pg-price').getBoundingClientRect(); return { x: e.left, y: e.top, w: e.width, h: e.height }; });
fs.writeFileSync('measure.json', JSON.stringify(r)); console.log(r); await b.close();
