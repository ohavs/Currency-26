// Google Play images for every listing language: five phone screenshots (1080x1920 JPEG, a caption over the
// app) and the feature graphic (1024x500 JPEG; tools/store/listing.py adds the icon), into
// store/listing/<locale>/images (fastlane layout). Sister locales (en-GB, es-419...) get copies at upload time
// (.github/workflows/play-listing.yml), so git keeps one set per language.
//
// The app screens come from the desktop preview (tools/preview, ONLY=store_ gradle -q run -> out/store_*.png).
// Captions reuse the app's own tour titles where they fit, so they are translated like the app.
//
//   PATH=/opt/node22/bin:$PATH NODE_PATH=$(npm root -g) node tools/store/screenshots.mjs [--only iw]
//
// Arabic, Hindi, Chinese and Japanese captions use Noto fonts from npm (@fontsource), fetched into
// tools/store/.fonts on the first run.
import { createRequire } from 'node:module';
import { execSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Global Playwright (NODE_PATH), as in tools/icons.
const { chromium } = createRequire(import.meta.url)('playwright');

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const SRC = path.join(ROOT, 'tools/preview/out');
const OUT = path.join(ROOT, 'store/listing');
const RES = path.join(ROOT, 'app/src/main/res');
const FONTS = path.join(ROOT, 'tools/store/.fonts');
const only = process.argv.includes('--only') ? process.argv[process.argv.indexOf('--only') + 1] : null;

// Preview language -> its Google Play locale.
const LOCALES = {
  iw: 'iw-IL', en: 'en-US', es: 'es-ES', ar: 'ar', ru: 'ru-RU', fr: 'fr-FR', de: 'de-DE', pt: 'pt-BR', it: 'it-IT',
  tr: 'tr-TR', zh: 'zh-CN', ja: 'ja-JP', hi: 'hi-IN',
};
const RTL = new Set(['iw', 'ar']);

// Captions the app has no string for: the first screenshot and the colour sets.
const CAPTIONS = {
  iw: ['יותר מ־160 מטבעות, גם בלי אינטרנט', '6 ערכות צבעים, בהיר וכהה'],
  en: ['160+ currencies, even offline', '6 color themes, light and dark'],
  es: ['Más de 160 monedas, incluso sin conexión', '6 temas de color, claro y oscuro'],
  ar: ['أكثر من 160 عملة، حتى دون إنترنت', '6 سمات ألوان، فاتحة وداكنة'],
  ru: ['160+ валют, даже без интернета', '6 цветовых тем, светлые и тёмные'],
  fr: ['160+ devises, même hors ligne', '6 thèmes de couleur, clair et sombre'],
  de: ['Über 160 Währungen, auch offline', '6 Farbschemas, hell und dunkel'],
  pt: ['Mais de 160 moedas, até offline', '6 temas de cores, claro e escuro'],
  it: ['Oltre 160 valute, anche offline', '6 temi colore, chiaro e scuro'],
  tr: ['160+ para birimi, çevrimdışı bile', '6 renk teması, açık ve koyu'],
  zh: ['160 多种货币，离线也能用', '6 种颜色主题，浅色和深色'],
  ja: ['160以上の通貨、オフラインでも', '6つのカラーテーマ、ライトとダーク'],
  hi: ['160+ मुद्राएँ, ऑफ़लाइन भी', '6 रंग थीम, लाइट और डार्क'],
};

const NOTO = { ar: 'noto-sans-arabic', hi: 'noto-sans-devanagari', zh: 'noto-sans-sc', ja: 'noto-sans-jp' };
const NOTO_FAMILY = { ar: 'Noto Sans Arabic', hi: 'Noto Sans Devanagari', zh: 'Noto Sans SC', ja: 'Noto Sans JP' };

function strings(lang) {
  const folder = lang === 'en' ? 'values' : `values-${lang}`;
  const xml = fs.readFileSync(path.join(RES, folder, 'strings.xml'), 'utf8');
  const map = {};
  for (const m of xml.matchAll(/<string name="([^"]+)">([\s\S]*?)<\/string>/g)) {
    map[m[1]] = m[2].replace(/\\'/g, "'").replace(/\\"/g, '"').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&amp;/g, '&');
  }
  return map;
}

function notoCss(lang) {
  const pkg = NOTO[lang];
  if (!pkg) return '';
  const dir = path.join(FONTS, pkg);
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
    const tgz = execSync(`npm pack @fontsource/${pkg} --silent`, { cwd: dir }).toString().trim().split('\n').pop();
    execSync(`tar xzf ${tgz} && rm ${tgz}`, { cwd: dir });
  }
  return ['500', '700'].map((w) => `<link rel="stylesheet" href="file://${dir}/package/${w}.css">`).join('\n');
}

const uri = (file) => 'data:image/png;base64,' + fs.readFileSync(file).toString('base64');
const shot = (lang, name) => uri(path.join(SRC, `store_${lang}_${name}.png`));

function page(lang, body, { width, height, bg, ink }) {
  const family = NOTO_FAMILY[lang] ? `'${NOTO_FAMILY[lang]}', ` : '';
  const font = (w, f) => `@font-face { font-family: Rubik; font-weight: ${w}; src: url('file://${RES}/font/rubik_${f}.ttf'); }`;
  return `<!doctype html><html lang="${lang}" dir="${RTL.has(lang) ? 'rtl' : 'ltr'}"><head><meta charset="utf-8">
${notoCss(lang)}
<style>
${font(500, 'medium')} ${font(600, 'semibold')} ${font(700, 'bold')}
html, body { margin: 0; width: ${width}px; height: ${height}px; overflow: hidden; }
body { background: ${bg}; color: ${ink}; font-family: Rubik, ${family}sans-serif; position: relative; }
.caption { position: absolute; top: 120px; left: 70px; right: 70px; text-align: center; font-size: 74px; font-weight: 700;
  line-height: 1.14; letter-spacing: -0.5px; }
.badge { display: inline-block; margin-top: 22px; padding: 8px 26px; border-radius: 40px; font-size: 34px; font-weight: 600;
  background: ${ink}; color: ${bg.includes('#2') || bg.includes('#1') ? '#1f1d33' : '#fff'}; }
.phone { position: absolute; left: 50%; transform: translateX(-50%); border-radius: 64px; padding: 16px; background: #1b2126;
  box-shadow: 0 30px 80px rgba(0,0,0,.28); }
.phone > .screen { border-radius: 50px; overflow: hidden; position: relative; }
.phone img.app { display: block; }
</style></head><body>${body}</body></html>`;
}

const SCREEN_W = 640, SCREEN_H = 1376; // the 800x1720 render at 0.8

function phone(content, top = 470) {
  return `<div class="phone" style="top:${top}px"><div class="screen" style="width:${SCREEN_W}px;height:${SCREEN_H}px">${content}</div></div>`;
}
const app = (src) => `<img class="app" src="${src}" width="${SCREEN_W}" height="${SCREEN_H}">`;
const caption = (text, badge) => `<div class="caption">${text}${badge ? `<br><span class="badge">★ ${badge}</span>` : ''}</div>`;

// A home screen: wallpaper, the clock, the large and the small widget.
function homeScreen(lang) {
  const dp = SCREEN_W / 400;
  return `<div style="position:absolute;inset:0;background:linear-gradient(160deg,#f3d9c4 0%,#c98f7a 55%,#6d4a5c 100%)"></div>
    <div style="position:absolute;top:${40 * dp}px;width:100%;text-align:center;color:#fff;font-size:${56 * dp}px;font-weight:500;
      font-family:Roboto,sans-serif;text-shadow:0 2px 12px rgba(0,0,0,.2)" dir="ltr">09:41</div>
    <img src="${shot(lang, '2a_widget')}" style="position:absolute;top:${130 * dp}px;left:${50 * dp}px;width:${300 * dp}px;height:${380 * dp}px;
      filter:drop-shadow(0 8px 18px rgba(0,0,0,.25))">
    <img src="${shot(lang, '2b_widget')}" style="position:absolute;top:${528 * dp}px;left:${50 * dp}px;width:${300 * dp}px;height:${130 * dp}px;
      filter:drop-shadow(0 8px 18px rgba(0,0,0,.25))">
    <div style="position:absolute;bottom:${36 * dp}px;left:0;right:0;display:flex;justify-content:center;gap:${26 * dp}px">
      ${['#f4f1ec', '#cfe3f5', '#d9ead3', '#f7dccf'].map((c) => `<div style="width:${54 * dp}px;height:${54 * dp}px;border-radius:50%;background:${c};opacity:.9"></div>`).join('')}
    </div>`;
}

function themes(lang) {
  const one = (name, rotate, x, y, z) => `<div class="phone" style="top:${y}px;transform:translateX(${x}px) rotate(${rotate}deg) scale(.78);
    transform-origin:50% 0;left:${540 - SCREEN_W / 2 - 16}px;z-index:${z}"><div class="screen" style="width:${SCREEN_W}px;height:${SCREEN_H}px">${app(shot(lang, name))}</div></div>`;
  return one("5b_theme", -7, -250, 540, 1) + one("5c_theme", 7, 250, 540, 1) + one("5a_theme", 0, 0, 450, 2);
}

function featureGraphic(lang, title, tagline) {
  const rtl = RTL.has(lang);
  const textSide = rtl ? 'right' : 'left';
  const phoneSide = rtl ? 'left' : 'right';
  return `<div style="position:absolute;inset:0;background:linear-gradient(135deg,#e4efe1,#a3cbad)"></div>
    <div style="position:absolute;top:96px;${textSide}:64px;width:560px;text-align:${rtl ? 'right' : 'left'}">
      <img src="${uri(path.join(ROOT, 'store/icon-512.png'))}" style="width:120px;height:120px;border-radius:28px;box-shadow:0 10px 30px rgba(0,0,0,.18)">
      <div style="font-size:54px;font-weight:700;line-height:1.1;margin-top:26px">${title}</div>
      <div style="font-size:30px;font-weight:500;margin-top:14px;opacity:.85">${tagline}</div>
    </div>
    <div class="phone" style="top:70px;left:auto;${phoneSide}:70px;transform:rotate(${rtl ? 6 : -6}deg) scale(.45);transform-origin:top ${phoneSide}">
      <div class="screen" style="width:${SCREEN_W}px;height:${SCREEN_H}px">${app(shot(lang, '1_main'))}</div></div>`;
}

const browser = await chromium.launch();
const ctx = await browser.newContext({ deviceScaleFactor: 1 });
const tab = await ctx.newPage();
async function save(html, size, file) {
  await tab.setViewportSize(size);
  await tab.setContent(html, { waitUntil: 'networkidle' });
  await tab.evaluate(() => document.fonts.ready);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  const png = file.endsWith('.png');
  await tab.screenshot({ path: file, type: png ? 'png' : 'jpeg', ...(png ? { omitBackground: true } : { quality: 90 }) });
}

for (const [lang, locale] of Object.entries(LOCALES)) {
  if (only && lang !== only) continue;
  const s = strings(lang);
  const [first, colors] = CAPTIONS[lang];
  const sage = { bg: 'linear-gradient(180deg,#e7f0e4,#bfdcc0)', ink: '#26343f' };
  const shots = [
    [caption(first) + phone(app(shot(lang, '1_main'))), sage],
    [caption(s.tip_widget_title, s.premium) + phone(homeScreen(lang)), { bg: 'linear-gradient(180deg,#f6ebe1,#e3c3ab)', ink: '#3e2f2a' }],
    [caption(s.tip_calc_title, s.premium) + phone(app(shot(lang, '3_calc'))), { bg: 'linear-gradient(180deg,#e3eef6,#a5c5d9)', ink: '#22344a' }],
    [caption(s.tip_select_title) + phone(app(shot(lang, '4_quick'))), sage],
    [caption(colors, s.premium) + themes(lang), { bg: 'linear-gradient(180deg,#2c2a46,#1c1b2c)', ink: '#e8e6f6' }],
  ];
  const dir = path.join(OUT, locale, 'images');
  fs.rmSync(path.join(dir, 'phoneScreenshots'), { recursive: true, force: true });
  for (const [i, [body, look]] of shots.entries()) {
    await save(page(lang, body, { width: 1080, height: 1920, ...look }), { width: 1080, height: 1920 },
      path.join(dir, 'phoneScreenshots', `${i + 1}.jpg`));
  }
  const title = fs.readFileSync(path.join(OUT, locale, 'title.txt'), 'utf8').trim();
  await save(page(lang, featureGraphic(lang, title, first), { width: 1024, height: 500, bg: '#dbe9d8', ink: '#26343f' }),
    { width: 1024, height: 500 }, path.join(dir, 'featureGraphic.jpg'));
  console.log(`${lang}: ${locale}`);
}
await browser.close();
