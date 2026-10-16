// The app icon, in six looks: one per colour set (Settings -> Appearance shows them; a tap changes the
// colours and the background texture, never the launcher icon). The launcher icon is "classic".
//
// Each look is an SVG on Android's 108dp adaptive-icon grid, rendered with Chromium (Playwright):
//   app/src/main/res/mipmap-*/ic_launcher_{background,foreground,monochrome}.png   adaptive icon layers
//   app/src/main/res/mipmap-*/ic_launcher[_round].png                             Android 7 (legacy)
//   app/src/main/res/drawable-nodpi/palette_icon_<key>.png                         Settings tiles
//   store/icon-512.png                                                             Google Play listing
//   node tools/icons/make_icons.mjs [--preview out.png]
// Everything that matters stays inside the 66dp circle in the middle (21..87): launchers may crop the rest.
// Needs the playwright package (npm i playwright) and its Chromium.
import { createRequire } from 'node:module';
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

// CommonJS resolution also finds a globally installed playwright (NODE_PATH).
const { chromium } = createRequire(import.meta.url)('playwright');

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const RES = join(ROOT, 'app/src/main/res');
const DENSITIES = { mdpi: 1, hdpi: 1.5, xhdpi: 2, xxhdpi: 3, xxxhdpi: 4 };
const LAUNCHER = 'sage';
const FONT = readFileSync(join(RES, 'font/rubik_bold.ttf')).toString('base64');

// A small deterministic random, so stars and lines stay where they are.
function rand(seed) {
  let s = seed;
  return () => { s = (s * 16807) % 2147483647; return (s - 1) / 2147483646; };
}
const r1 = (v) => Math.round(v * 10) / 10;

/** A wavy line across the icon: y0 + amplitude * sin(...). */
function wave(y0, amplitude, length, phase, from = -6, to = 114) {
  const pts = [];
  for (let x = from; x <= to; x += 2) pts.push(`${r1(x)} ${r1(y0 + amplitude * Math.sin((x / length) * Math.PI * 2 + phase))}`);
  return `M${pts.join(' L')}`;
}

// The looks, keyed like the app's colour sets. `ink` colours the symbols and the swap pill.
const LOOKS = {
  sage: {
    label: 'classic',
    ink: '#24363F', cardB: '#E3F1E6',
    background: () => {
      // Banknote guilloche: two families of waves crossing into a lattice.
      const lines = [];
      for (let i = 0; i < 9; i++) {
        lines.push(wave(6 + i * 12.5, 4.2, 36, 0));
        lines.push(wave(6 + i * 12.5, 4.2, 36, Math.PI));
      }
      return `
        <defs>
          <linearGradient id="bg" x1="0" y1="0" x2="108" y2="108" gradientUnits="userSpaceOnUse">
            <stop offset="0" stop-color="#93C9A3"/><stop offset="1" stop-color="#3B765B"/>
          </linearGradient>
          <radialGradient id="hl" cx="0.28" cy="0.2" r="0.7"><stop offset="0" stop-color="#fff" stop-opacity="0.28"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></radialGradient>
        </defs>
        <rect width="108" height="108" fill="url(#bg)"/>
        <path d="${lines.join(' ')}" fill="none" stroke="#fff" stroke-opacity="0.2" stroke-width="0.7"/>
        <rect width="108" height="108" fill="url(#hl)"/>`;
    },
  },
  ocean: {
    label: 'sea',
    ink: '#163E5C', cardB: '#E2F1FA',
    background: () => `
      <defs>
        <linearGradient id="bg" x1="0" y1="0" x2="0" y2="108" gradientUnits="userSpaceOnUse">
          <stop offset="0" stop-color="#9ADAF6"/><stop offset="0.55" stop-color="#3796CF"/><stop offset="1" stop-color="#18598A"/>
        </linearGradient>
        <radialGradient id="sun" cx="0.72" cy="0.27" r="0.3"><stop offset="0" stop-color="#fff" stop-opacity="0.7"/><stop offset="0.35" stop-color="#fff" stop-opacity="0.25"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></radialGradient>
      </defs>
      <rect width="108" height="108" fill="url(#bg)"/>
      <rect width="108" height="108" fill="url(#sun)"/>
      <g fill="none" stroke="#fff" stroke-linecap="round">
        <path d="${wave(70, 2, 13, 0)}" stroke-opacity="0.32" stroke-width="1.4"/>
        <path d="${wave(77, 2, 13, 1.6)}" stroke-opacity="0.42" stroke-width="1.4"/>
        <path d="${wave(84, 2, 13, 3.2)}" stroke-opacity="0.55" stroke-width="1.5"/>
        <path d="${wave(91, 2, 13, 4.8)}" stroke-opacity="0.4" stroke-width="1.5"/>
      </g>`,
  },
  sunset: {
    label: 'sand',
    ink: '#4A3427', cardB: '#FBF0DE',
    background: () => `
      <defs>
        <linearGradient id="bg" x1="0" y1="0" x2="0" y2="108" gradientUnits="userSpaceOnUse">
          <stop offset="0" stop-color="#FBEBC6"/><stop offset="1" stop-color="#E2AE6E"/>
        </linearGradient>
      </defs>
      <rect width="108" height="108" fill="url(#bg)"/>
      <circle cx="78" cy="29" r="6.5" fill="#FFF8E6" fill-opacity="0.95"/>
      <path d="M0 66 C18 56 38 57 58 65 S94 74 108 62 V108 H0Z" fill="#D69A5C" fill-opacity="0.5"/>
      <path d="M0 80 C22 70 48 73 68 81 S98 88 108 79 V108 H0Z" fill="#C2814A" fill-opacity="0.7"/>
      <g fill="none" stroke="#FFF7E8" stroke-opacity="0.4" stroke-width="0.8">
        <path d="M14 74 C26 68 38 68 50 72"/><path d="M62 86 C74 82 86 82 98 85"/><path d="M18 88 C30 84 42 84 52 88"/>
      </g>`,
  },
  rose: {
    label: 'sunset',
    ink: '#4A2A38', cardB: '#FFECE8',
    background: () => `
      <defs>
        <linearGradient id="bg" x1="0" y1="0" x2="0" y2="108" gradientUnits="userSpaceOnUse">
          <stop offset="0" stop-color="#FFC48E"/><stop offset="0.55" stop-color="#F27E7C"/><stop offset="1" stop-color="#A24A7C"/>
        </linearGradient>
        <linearGradient id="sun" x1="0" y1="37" x2="0" y2="95" gradientUnits="userSpaceOnUse">
          <stop offset="0" stop-color="#FFE9B8"/><stop offset="1" stop-color="#FFB37A"/>
        </linearGradient>
        <radialGradient id="glow" cx="0.5" cy="0.62" r="0.55"><stop offset="0" stop-color="#FFE6B0" stop-opacity="0.55"/><stop offset="1" stop-color="#FFE6B0" stop-opacity="0"/></radialGradient>
      </defs>
      <rect width="108" height="108" fill="url(#bg)"/>
      <rect width="108" height="108" fill="url(#glow)"/>
      <circle cx="54" cy="66" r="29" fill="url(#sun)" fill-opacity="0.95"/>
      <g fill="url(#bg)">
        <rect x="0" y="80" width="108" height="2"/><rect x="0" y="85" width="108" height="2.6"/><rect x="0" y="90.6" width="108" height="3.2"/>
      </g>`,
  },
  lavender: {
    label: 'night',
    ink: '#2A2650', cardB: '#ECEAFB',
    background: () => {
      const r = rand(7);
      const stars = [];
      for (let i = 0; i < 46; i++) {
        const x = r1(r() * 108), y = r1(r() * 108), s = r1(0.35 + r() * 0.8), o = r1(0.4 + r() * 0.55);
        stars.push(`<circle cx="${x}" cy="${y}" r="${s}" fill="#fff" fill-opacity="${o}"/>`);
      }
      const sparkle = (x, y, k) => `<path d="M${x} ${y - k}Q${x} ${y} ${x + k} ${y}Q${x} ${y} ${x} ${y + k}Q${x} ${y} ${x - k} ${y}Q${x} ${y} ${x} ${y - k}Z" fill="#fff" fill-opacity="0.9"/>`;
      return `
        <defs>
          <linearGradient id="bg" x1="0" y1="0" x2="0" y2="108" gradientUnits="userSpaceOnUse">
            <stop offset="0" stop-color="#6252C2"/><stop offset="1" stop-color="#1F1A4E"/>
          </linearGradient>
          <mask id="moon"><rect width="108" height="108" fill="#fff"/><circle cx="80.5" cy="24.5" r="6.5" fill="#000"/></mask>
        </defs>
        <rect width="108" height="108" fill="url(#bg)"/>
        ${stars.join('')}
        ${sparkle(28, 30, 3)}${sparkle(82, 82, 2.6)}
        <circle cx="76" cy="28" r="7.5" fill="#FFE7A6" mask="url(#moon)"/>`;
    },
  },
  gold: {
    label: 'gold',
    ink: '#4A3412', cardB: '#FFF3D4',
    background: () => {
      // A coin: engine-turned rings, a reeded rim and a sheen across the metal.
      const rings = [];
      for (let r = 8; r <= 40; r += 4) rings.push(`<circle cx="54" cy="54" r="${r}"/>`);
      const ticks = [];
      for (let i = 0; i < 72; i++) {
        const a = (i / 72) * Math.PI * 2, c = Math.cos(a), s = Math.sin(a);
        ticks.push(`M${r1(54 + 44 * c)} ${r1(54 + 44 * s)}L${r1(54 + 49 * c)} ${r1(54 + 49 * s)}`);
      }
      return `
        <defs>
          <linearGradient id="bg" x1="0" y1="0" x2="108" y2="108" gradientUnits="userSpaceOnUse">
            <stop offset="0" stop-color="#FBE29A"/><stop offset="0.5" stop-color="#E2AF48"/><stop offset="1" stop-color="#9E6A1C"/>
          </linearGradient>
          <linearGradient id="sheen" x1="0" y1="0" x2="108" y2="108" gradientUnits="userSpaceOnUse">
            <stop offset="0.25" stop-color="#fff" stop-opacity="0"/><stop offset="0.4" stop-color="#fff" stop-opacity="0.32"/><stop offset="0.55" stop-color="#fff" stop-opacity="0"/>
          </linearGradient>
        </defs>
        <rect width="108" height="108" fill="url(#bg)"/>
        <g fill="none" stroke="#fff" stroke-opacity="0.16" stroke-width="0.7">${rings.join('')}</g>
        <circle cx="54" cy="54" r="42.5" fill="none" stroke="#7A4E10" stroke-opacity="0.25" stroke-width="0.8"/>
        <path d="${ticks.join('')}" stroke="#fff" stroke-opacity="0.28" stroke-width="0.9"/>
        <rect width="108" height="108" fill="url(#sheen)"/>`;
    },
  },
};

const ARROWS = 'M50 57.4V50.6M47.7 52.9L50 50.6L52.3 52.9M58 50.6V57.4M55.7 55.1L58 57.4L60.3 55.1';

/** Two currency cards ($ and €) with the app's swap pill between them, cut into both. */
function foreground(look) {
  return `
    <defs>
      <filter id="shadow" x="-30%" y="-30%" width="160%" height="160%">
        <feDropShadow dx="0" dy="1.6" stdDeviation="1.7" flood-color="#000" flood-opacity="0.24"/>
      </filter>
    </defs>
    <g filter="url(#shadow)">
      <rect x="32" y="29" width="44" height="22.5" rx="8.5" fill="#fff"/>
      <rect x="32" y="56.5" width="44" height="22.5" rx="8.5" fill="${look.cardB}"/>
    </g>
    <g font-family="IconRubik" font-weight="700" font-size="16" text-anchor="middle" fill="${look.ink}">
      <text x="54" y="45.9">$</text>
      <text x="54" y="73.4">€</text>
    </g>
    <rect x="40.5" y="46" width="27" height="16" rx="8" fill="url(#bg)"/>
    <rect x="42.5" y="48" width="23" height="12" rx="6" fill="${look.ink}"/>
    <path d="${ARROWS}" fill="none" stroke="#fff" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>`;
}

/** The foreground as one colour for Android 13 themed icons: cards with the symbols and the pill cut out. */
function monochrome() {
  return `
    <defs><mask id="m">
      <rect width="108" height="108" fill="#000"/>
      <rect x="32" y="29" width="44" height="22.5" rx="8.5" fill="#fff"/>
      <rect x="32" y="56.5" width="44" height="22.5" rx="8.5" fill="#fff"/>
      <g font-family="IconRubik" font-weight="700" font-size="16" text-anchor="middle" fill="#000">
        <text x="54" y="45.9">$</text><text x="54" y="73.4">€</text>
      </g>
      <rect x="40.5" y="46" width="27" height="16" rx="8" fill="#000"/>
      <rect x="42.5" y="48" width="23" height="12" rx="6" fill="#fff"/>
      <path d="${ARROWS}" fill="none" stroke="#000" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
    </mask></defs>
    <rect width="108" height="108" fill="#fff" mask="url(#m)"/>`;
}

const svg = (body, viewBox = '0 0 108 108', clip = '') => `
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="${viewBox}" width="100%" height="100%">
    ${clip ? `<defs><clipPath id="shape">${clip}</clipPath></defs><g clip-path="url(#shape)">${body}</g>` : body}
  </svg>`;

// The whole icon: background + foreground (the background's gradient also fills the pill's ring).
const full = (look) => look.background() + foreground(look);
// The visible 72dp of the adaptive icon, as a rounded square (tiles, legacy icon) or a circle.
const squircle = '<rect x="18" y="18" width="72" height="72" rx="17"/>';
const circle = '<circle cx="54" cy="54" r="36"/>';

async function main() {
  const preview = process.argv.includes('--preview') ? process.argv[process.argv.indexOf('--preview') + 1] : null;
  const browser = await chromium.launch();
  const page = await browser.newPage();

  async function render(markup, px, file) {
    await page.setViewportSize({ width: px, height: px });
    await page.setContent(`<!doctype html><html><head><style>
      @font-face { font-family: IconRubik; font-weight: 700; src: url(data:font/ttf;base64,${FONT}); }
      html, body { margin: 0; background: transparent; } svg { display: block; width: ${px}px; height: ${px}px; }
    </style></head><body>${markup}</body></html>`);
    await page.evaluate(() => document.fonts.ready);
    mkdirSync(dirname(file), { recursive: true });
    writeFileSync(file, await page.screenshot({ omitBackground: true, clip: { x: 0, y: 0, width: px, height: px } }));
  }

  if (preview) {
    // All of them side by side as rounded squares, to compare.
    const scoped = (markup, p) => markup.replace(/id="([^"]+)"/g, `id="${p}$1"`).replace(/url\(#([^)]+)\)/g, `url(#${p}$1)`);
    const cells = Object.values(LOOKS).map((look, i) =>
      `<div style="display:inline-block;width:160px;height:160px;margin:8px">${scoped(svg(full(look), '18 18 72 72', squircle), `l${i}`)}</div>`).join('');
    await page.setViewportSize({ width: Object.keys(LOOKS).length * 176, height: 176 });
    await page.setContent(`<!doctype html><html><head><style>
      @font-face { font-family: IconRubik; font-weight: 700; src: url(data:font/ttf;base64,${FONT}); }
      body { margin: 0; background: #fff; }</style></head><body>${cells}</body></html>`);
    await page.evaluate(() => document.fonts.ready);
    writeFileSync(preview, await page.screenshot());
    await browser.close();
    return;
  }

  const launcher = LOOKS[LAUNCHER];
  for (const [density, scale] of Object.entries(DENSITIES)) {
    const dir = join(RES, `mipmap-${density}`);
    await render(svg(launcher.background()), Math.round(108 * scale), join(dir, 'ic_launcher_background.png'));
    await render(svg(`<defs>${launcher.background().match(/<linearGradient id="bg"[\s\S]*?<\/linearGradient>/)[0]}</defs>` + foreground(launcher)),
      Math.round(108 * scale), join(dir, 'ic_launcher_foreground.png'));
    await render(svg(monochrome()), Math.round(108 * scale), join(dir, 'ic_launcher_monochrome.png'));
    await render(svg(full(launcher), '18 18 72 72', squircle), Math.round(48 * scale), join(dir, 'ic_launcher.png'));
    await render(svg(full(launcher), '18 18 72 72', circle), Math.round(48 * scale), join(dir, 'ic_launcher_round.png'));
  }
  for (const [key, look] of Object.entries(LOOKS)) {
    await render(svg(full(look), '18 18 72 72', squircle), 192, join(RES, 'drawable-nodpi', `palette_icon_${key}.png`));
  }
  await render(svg(full(launcher), '18 18 72 72'), 512, join(ROOT, 'store', 'icon-512.png'));
  await browser.close();
  console.log('icons written');
}

main();
