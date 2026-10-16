// The quiet texture behind each colour set, shared by the app and the home-screen widgets.
//
// Every texture is drawn here once, white on transparent at xxhdpi (3 px per dp), with its strength baked
// into the alpha; the app and the widgets tint it with the colour set's accent:
//   app/src/main/res/drawable-xxhdpi/texture_<key>_tile.png   one repeating tile
//   app/src/main/res/drawable/texture_<key>.xml                 the tile repeated (for a widget ImageView)
// "rose" (sunset) is not a tile but sun rays rising from the bottom edge: a vector drawable for the widgets,
// drawn by PaletteTexture.kt in the app with the same numbers (RAYS below).
//   node tools/textures/make_textures.mjs [--preview out.png]
// Needs the playwright package (npm i playwright) and its Chromium.
import { createRequire } from 'node:module';
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

// CommonJS resolution also finds a globally installed playwright (NODE_PATH).
const { chromium } = createRequire(import.meta.url)('playwright');

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const RES = join(ROOT, 'app/src/main/res');
const SCALE = 3; // xxhdpi
const FONT = readFileSync(join(RES, 'font/rubik_bold.ttf')).toString('base64');

// Sun rays for "rose": wedges every STEP degrees over the upper half, from a sun just below the bottom edge.
export const RAYS = { count: 18, centerY: 1.04, alpha: 0.07 };

// Each tile is drawn in the page by a function of (ctx, w, h) in dp; `alpha` is baked into the PNG.
const TILES = {
  // Classic: a banknote's guilloche - two crossing waves, and a finer pair between the rows.
  sage: { w: 72, h: 26, alpha: 0.11, draw: `(ctx, w, h) => {
    const wave = (y0, a, len, phase, width) => {
      ctx.beginPath();
      for (let x = -2; x <= w + 2; x += 0.5) {
        const y = y0 + a * Math.sin(x / len * 2 * Math.PI + phase);
        x === -2 ? ctx.moveTo(x, y) : ctx.lineTo(x, y);
      }
      ctx.lineWidth = width; ctx.stroke();
    };
    wave(13, 5, 72, 0, 1); wave(13, 5, 72, Math.PI, 1);
    ctx.globalAlpha = 0.55;
    wave(0, 1.6, 24, 0, 0.6); wave(26, 1.6, 24, 0, 0.6);
  }` },
  // Sea: seigaiha, the Japanese pattern of overlapping wave scales.
  ocean: { w: 40, h: 20, alpha: 0.13, draw: `(ctx, w, h) => {
    const R = 20;
    for (let j = -2; j <= 3; j++) {
      for (let i = -1; i <= 2; i++) {
        const cx = i * 2 * R + (j % 2 ? R : 0), cy = j * R / 2 + R;
        ctx.save();
        ctx.globalCompositeOperation = 'destination-out';
        ctx.beginPath(); ctx.arc(cx, cy, R - 0.5, 0, 2 * Math.PI); ctx.fill();
        ctx.restore();
        ctx.lineWidth = 0.9;
        for (const r of [R - 1, R - 5, R - 9, R - 13]) { ctx.beginPath(); ctx.arc(cx, cy, r, 0, 2 * Math.PI); ctx.stroke(); }
        ctx.beginPath(); ctx.arc(cx, cy, 2.2, 0, 2 * Math.PI); ctx.fill();
      }
    }
  }` },
  // Sand: short wind ripples, as on a dune, each with grains drifting off its lee side.
  sunset: { w: 120, h: 60, alpha: 0.22, draw: `(ctx, w, h) => {
    const rnd = (() => { let s = 23; return () => (s = (s * 16807) % 2147483647, (s - 1) / 2147483646); })();
    // [start x, length, baseline y, bend]: arcs that wrap around the tile edges.
    const ripples = [[4, 52, 10, 3.2], [66, 44, 6, 2.6], [30, 58, 25, 3.6], [96, 40, 21, 2.4],
                     [10, 46, 40, 3.0], [62, 56, 44, 3.4], [100, 34, 55, 2.2]];
    for (const [x0, len, y0, bend] of ripples) {
      const y = (x) => y0 - bend * Math.sin(Math.PI * (x - x0) / len);
      for (const dx of [-w, 0, w]) for (const dy of [-h, 0, h]) {
        ctx.lineWidth = 0.9;
        ctx.beginPath();
        for (let x = x0; x <= x0 + len; x += 0.5) {
          const t = (x - x0) / len;
          x === x0 ? ctx.moveTo(x + dx, y(x) + dy) : ctx.lineTo(x + dx, y(x) + dy);
        }
        ctx.stroke();
      }
      for (let k = 0; k < len * 0.9; k++) {
        const x = x0 + 2 + rnd() * (len - 4), d = Math.pow(rnd(), 1.6) * 6;
        const edge = Math.sin(Math.PI * (x - x0) / len);
        const a = 0.8 * (1 - d / 7) * edge, r = 0.4 + rnd() * 0.3;
        for (const dx of [-w, 0, w]) for (const dy of [-h, 0, h]) {
          ctx.globalAlpha = a;
          ctx.beginPath(); ctx.arc(x + dx, y(x) + 1.4 + d + dy, r, 0, 2 * Math.PI); ctx.fill();
        }
      }
      ctx.globalAlpha = 1;
    }
    for (let k = 0; k < 30; k++) {
      ctx.globalAlpha = 0.3 + rnd() * 0.4;
      const x = rnd() * w, yy = rnd() * h, r = 0.4 + rnd() * 0.35;
      ctx.beginPath(); ctx.arc(x, yy, r, 0, 2 * Math.PI); ctx.fill();
    }
  }` },
  // Night: constellations - bright stars joined by faint lines, over star dust.
  lavender: { w: 220, h: 220, alpha: 0.42, draw: `(ctx, w, h) => {
    const rnd = (() => { let s = 5; return () => (s = (s * 16807) % 2147483647, (s - 1) / 2147483646); })();
    const star = (x, y, r, a) => { ctx.globalAlpha = a; ctx.beginPath(); ctx.arc(x, y, r, 0, 2 * Math.PI); ctx.fill(); };
    for (let k = 0; k < 34; k++) star(rnd() * w, rnd() * h, 0.45 + rnd() * 0.4, 0.3 + rnd() * 0.35);
    const figures = [
      // A dipper, a "W", a kite and a small triangle.
      { at: [18, 26], pts: [[0, 0], [16, 5], [30, 4], [42, 12], [46, 30], [66, 34], [62, 14]], path: [0, 1, 2, 3, 4, 5, 6, 3] },
      { at: [122, 18], pts: [[0, 10], [14, 26], [30, 14], [46, 30], [64, 8]], path: [0, 1, 2, 3, 4] },
      { at: [40, 118], pts: [[22, 0], [0, 26], [20, 58], [44, 30], [22, 0], [20, 58], [24, 82]], path: [0, 1, 2, 3, 0, 2, 6] },
      { at: [150, 132], pts: [[0, 30], [26, 0], [44, 38], [18, 50]], path: [0, 1, 2, 3, 0] },
    ];
    for (const f of figures) {
      const p = f.pts.map(([x, y]) => [f.at[0] + x, f.at[1] + y]);
      ctx.globalAlpha = 0.42; ctx.lineWidth = 0.7; ctx.beginPath();
      f.path.forEach((i, n) => n ? ctx.lineTo(...p[i]) : ctx.moveTo(...p[i]));
      ctx.stroke();
      p.forEach(([x, y], i) => {
        const r = 1.3 + ((i * 7 + 3) % 5) * 0.22;
        star(x, y, r, 1);
        if (i % 3 === 0) { ctx.globalAlpha = 0.5; ctx.lineWidth = 0.5; ctx.beginPath(); ctx.arc(x, y, r + 1.8, 0, 2 * Math.PI); ctx.stroke(); }
      });
    }
    ctx.globalAlpha = 1;
  }` },
  // Gold: the currency symbols, staggered and a little tilted.
  gold: { w: 128, h: 112, alpha: 0.12, draw: `(ctx, w, h) => {
    const symbols = ['$', '€', '£', '¥', '₪'];
    const order = [[0, 2, 4, 1], [3, 0, 2, 4], [1, 3, 0, 2], [4, 1, 3, 0]];
    ctx.font = '700 15px IconRubik'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
    for (let r = 0; r < 4; r++) for (let c = 0; c < 4; c++) {
      const x = c * 32 + (r % 2 ? 16 : 0) + 8, y = r * 28 + 14;
      for (const dx of [-w, 0, w]) {
        ctx.save(); ctx.translate(x + dx, y); ctx.rotate(((r + c) % 2 ? -1 : 1) * 0.2);
        ctx.fillText(symbols[order[r][c]], 0, 0); ctx.restore();
      }
    }
  }` },
};

// Light-mode preview colours (background, accent) and dark ones, from ui/theme/Color.kt.
const PREVIEW = {
  sage: ['#D9EBD7', '#34495A', '#24313D', '#D3E8D1'],
  ocean: ['#D8E7EF', '#2C4660', '#1E2B39', '#CBE0EE'],
  sunset: ['#F2E4D8', '#4B3630', '#2C2321', '#F0DAC9'],
  rose: ['#F2E0E2', '#4B303A', '#2A2126', '#F0D3D9'],
  lavender: ['#E3E1F1', '#37355B', '#22212E', '#D9D5F1'],
  gold: ['#F0E7CC', '#4A3D1C', '#26231B', '#EBD49A'],
};

const page = `<!doctype html><html><head><style>
  @font-face { font-family: IconRubik; src: url(data:font/ttf;base64,${FONT}); }
  body { margin: 0; background: #fff; }
</style></head><body><canvas id="c"></canvas></body></html>`;

/** Sun rays as wedge paths in a 100x100 box: the same geometry PaletteTexture.kt draws. */
function raysPath() {
  const cx = 50, cy = 100 * RAYS.centerY, R = 160;
  const step = Math.PI / RAYS.count;
  const parts = [];
  for (let i = 0; i < RAYS.count; i += 2) {
    const a0 = Math.PI + i * step, a1 = a0 + step;
    const p = (a) => `${(cx + R * Math.cos(a)).toFixed(2)},${(cy + R * Math.sin(a)).toFixed(2)}`;
    parts.push(`M${cx},${cy.toFixed(2)} L${p(a0)} L${p(a1)} Z`);
  }
  return parts.join(' ');
}

const bitmapXml = (key) => `<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by tools/textures/make_textures.mjs: the ${key} texture tile, repeated over the widget. -->
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
    android:src="@drawable/texture_${key}_tile"
    android:tileMode="repeat" />
`;

const raysXml = () => `<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by tools/textures/make_textures.mjs: sun rays rising from the bottom edge (the rose texture). -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="100dp"
    android:height="100dp"
    android:viewportWidth="100"
    android:viewportHeight="100">
    <path
        android:fillColor="#FFFFFFFF"
        android:fillAlpha="${RAYS.alpha}"
        android:pathData="${raysPath()}" />
</vector>
`;

async function main() {
  const preview = process.argv.includes('--preview') ? process.argv[process.argv.indexOf('--preview') + 1] : null;
  const browser = await chromium.launch();
  const tab = await browser.newPage();
  await tab.setContent(page);
  await tab.evaluate(() => document.fonts.load('700 15px IconRubik'));

  const pngs = {};
  for (const [key, t] of Object.entries(TILES)) {
    const data = await tab.evaluate(([w, h, alpha, scale, draw]) => {
      const c = document.getElementById('c');
      c.width = Math.round(w * scale); c.height = Math.round(h * scale);
      const ctx = c.getContext('2d');
      ctx.clearRect(0, 0, c.width, c.height);
      ctx.scale(scale, scale);
      ctx.fillStyle = ctx.strokeStyle = '#fff'; ctx.lineCap = 'round'; ctx.lineJoin = 'round';
      // Drawn opaque on a layer, then faded as a whole so overlaps don't add up.
      const layer = document.createElement('canvas');
      layer.width = c.width; layer.height = c.height;
      const lctx = layer.getContext('2d');
      lctx.scale(scale, scale);
      lctx.fillStyle = lctx.strokeStyle = '#fff'; lctx.lineCap = 'round'; lctx.lineJoin = 'round';
      (0, eval)(draw)(lctx, w, h);
      ctx.setTransform(1, 0, 0, 1, 0, 0);
      ctx.globalAlpha = alpha;
      ctx.drawImage(layer, 0, 0);
      return c.toDataURL('image/png');
    }, [t.w, t.h, t.alpha, SCALE, t.draw]);
    pngs[key] = data;
    if (!preview) {
      mkdirSync(join(RES, 'drawable-xxhdpi'), { recursive: true });
      mkdirSync(join(RES, 'drawable'), { recursive: true });
      writeFileSync(join(RES, `drawable-xxhdpi/texture_${key}_tile.png`), Buffer.from(data.split(',')[1], 'base64'));
      writeFileSync(join(RES, `drawable/texture_${key}.xml`), bitmapXml(key));
    }
  }
  if (!preview) writeFileSync(join(RES, 'drawable/texture_rose.xml'), raysXml());

  if (preview) {
    // Every set on a phone-sized panel, light and dark, with a card on top to judge the contrast.
    const keys = Object.keys(PREVIEW);
    const panels = keys.flatMap((key) => [0, 1].map((dark) => {
      const [bgL, accL, bgD, accD] = PREVIEW[key];
      const bg = dark ? bgD : bgL, acc = dark ? accD : accL;
      const opacity = dark ? 0.6 : 1;
      const tex = key === 'rose'
        ? `<svg viewBox="0 0 100 100" preserveAspectRatio="none" style="position:absolute;inset:0;width:100%;height:100%;opacity:${opacity}"><path d="${raysPath()}" fill="${acc}" fill-opacity="${RAYS.alpha}"/></svg>`
        : `<div style="position:absolute;inset:0;background:${acc};opacity:${opacity};-webkit-mask:url(${pngs[key]}) 0 0/${TILES[key].w}px ${TILES[key].h}px"></div>`;
      return `<div style="position:relative;width:200px;height:380px;background:${bg};border-radius:18px;overflow:hidden">${tex}
        <div style="position:absolute;left:14px;right:14px;top:70px;height:64px;border-radius:20px;background:${acc};opacity:0.12"></div>
        <div style="position:absolute;left:10px;top:8px;font:600 13px sans-serif;color:${acc}">${key}${dark ? ' · dark' : ''}</div></div>`;
    }));
    await tab.setViewportSize({ width: 1700, height: 820 });
    await tab.setContent(`<body style="margin:0;background:#888;display:grid;grid-template-columns:repeat(6,200px);gap:12px 10px;padding:12px;width:1300px">
      ${keys.map((k, i) => panels[i * 2]).join('')}${keys.map((k, i) => panels[i * 2 + 1]).join('')}</body>`);
    await tab.screenshot({ path: preview, fullPage: true });
  }
  await browser.close();
}

main();
