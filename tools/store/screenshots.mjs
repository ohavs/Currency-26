// Google Play images for every listing language: five phone screenshots (1080x1920 JPEG) and the feature graphic
// (1024x500 JPEG; tools/store/listing.py adds the icon), into store/listing/<locale>/images (fastlane layout).
// Sister locales (en-GB, es-419...) get copies at upload time (.github/workflows/play-listing.yml), so git keeps one
// set per language.
//
// Style: brutalist, picked by the owner (2026-10-27): one flat colour per screenshot taken from the launcher icon of
// the colour set its screen shows (tools/icons/make_icons.mjs), the app's slate ink for a frame, outlines and hard
// offset shadows, a giant headline whose last beat sits in a solid block, tilted phones and stickers. Headlines
// use Secular One (Hebrew and Latin); Russian falls back to the app's Rubik, Arabic, Hindi, Chinese and Japanese
// to Noto. The quality rules (thumbnail test, no clipped text, the phone's focus never cropped) come from the
// app-store-screenshots skill (github.com/ParthJadhav/app-store-screenshots).
//
// The app screens come from the desktop preview (tools/preview, ONLY=store_ gradle -q run -> out/store_*.png).
//
//   PATH=/opt/node22/bin:$PATH NODE_PATH=$(npm root -g) node tools/store/screenshots.mjs [--only iw]
//
// Fonts come from npm (@fontsource) into tools/store/.fonts on the first run.
import { createRequire } from 'node:module';
import { execSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import * as scenes from './scenes.mjs';

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
// Secular One draws the Hebrew (its Latin is too light for these headlines); Latin and Cyrillic use the app's bold
// Rubik, the other scripts bold Noto.
const SECULAR = new Set(['iw']);
const CJK = new Set(['zh', 'ja']);

// Headlines are two beats: plain words, then the payoff in a block. Stickers are short facts that are true for the
// app: 160+ currencies, offline rates, 6 colour sets. Premium and "from any app" come from the app's own strings.
const COPY = {
  iw: {
    h1: ['כל מחיר בעולם.', 'בשקלים.'], h2: ['ההמרה ישר מהמסך', 'הראשי.'], h3: ['מחשבים וממירים', 'בבת אחת.'],
    h4: ['מסמנים מחיר.', 'ההמרה קופצת.'], h5: ['הצבעים שלך.', 'בהיר או כהה.'],
    cur: 'מטבעות', off: 'גם בלי אינטרנט', sets: 'ערכות צבעים',
  },
  en: {
    h1: ['Every price.', 'In your currency.'], h2: ['Convert right from', 'your home screen.'],
    h3: ['Calculate first.', 'Convert as you go.'], h4: ['Select a price.', 'See it converted.'],
    h5: ['Your colors.', 'Light or dark.'], cur: 'currencies', off: 'Works offline', sets: 'color sets',
  },
  es: {
    h1: ['Cada precio.', 'En tu moneda.'], h2: ['Convierte desde', 'tu pantalla de inicio.'],
    h3: ['Calcula primero.', 'Convierte al momento.'], h4: ['Selecciona un precio.', 'Míralo convertido.'],
    h5: ['Tus colores.', 'Claro u oscuro.'], cur: 'monedas', off: 'Funciona sin conexión', sets: 'temas de color',
  },
  ar: {
    h1: ['كل سعر.', 'بعملتك أنت.'], h2: ['حوّل مباشرة من', 'الشاشة الرئيسية.'], h3: ['احسب أولًا.', 'وحوّل فورًا.'],
    h4: ['حدّد سعرًا.', 'وشاهده محوّلًا.'], h5: ['ألوانك.', 'فاتح أو داكن.'],
    cur: 'عملة', off: 'يعمل دون إنترنت', sets: 'سمات ألوان',
  },
  ru: {
    h1: ['Любая цена.', 'В вашей валюте.'], h2: ['Конвертируйте', 'с главного экрана.'],
    h3: ['Сначала посчитайте.', 'Сразу в валюте.'], h4: ['Выделите цену.', 'Она уже в рублях.'],
    h5: ['Ваши цвета.', 'Светлые и тёмные.'], cur: 'валют', off: 'Работает без интернета', sets: 'цветовых тем',
  },
  fr: {
    h1: ['Chaque prix.', 'Dans votre devise.'], h2: ['Convertissez depuis', "l'écran d'accueil."],
    h3: ["Calculez d'abord.", 'Convertissez aussitôt.'], h4: ['Sélectionnez un prix.', 'Voyez-le converti.'],
    h5: ['Vos couleurs.', 'Clair ou sombre.'], cur: 'devises', off: 'Fonctionne hors ligne', sets: 'thèmes de couleur',
  },
  de: {
    h1: ['Jeder Preis.', 'In deiner Währung.'], h2: ['Umrechnen direkt', 'vom Startbildschirm.'],
    h3: ['Erst rechnen.', 'Dann umrechnen.'], h4: ['Preis markieren.', 'Sofort umgerechnet.'],
    h5: ['Deine Farben.', 'Hell oder dunkel.'], cur: 'Währungen', off: 'Funktioniert offline', sets: 'Farbschemas',
  },
  pt: {
    h1: ['Cada preço.', 'Na sua moeda.'], h2: ['Converta direto da', 'tela inicial.'],
    h3: ['Calcule primeiro.', 'Converta na hora.'], h4: ['Selecione um preço.', 'Veja convertido.'],
    h5: ['Suas cores.', 'Claro ou escuro.'], cur: 'moedas', off: 'Funciona offline', sets: 'temas de cores',
  },
  it: {
    h1: ['Ogni prezzo.', 'Nella tua valuta.'], h2: ['Converti dalla', 'schermata Home.'],
    h3: ['Prima calcola.', 'Poi converti.'], h4: ['Seleziona un prezzo.', 'Eccolo convertito.'],
    h5: ['I tuoi colori.', 'Chiaro o scuro.'], cur: 'valute', off: 'Funziona offline', sets: 'temi colore',
  },
  tr: {
    h1: ['Her fiyat.', 'Kendi paranla.'], h2: ['Ana ekrandan', 'hemen çevir.'], h3: ['Önce hesapla.', 'Sonra çevir.'],
    h4: ['Bir fiyat seç.', 'Çevrilmiş gör.'], h5: ['Senin renklerin.', 'Açık ya da koyu.'],
    cur: 'para birimi', off: 'İnternetsiz çalışır', sets: 'renk teması',
  },
  zh: {
    h1: ['每一个价格，', '换成你的货币。'], h2: ['在主屏幕上', '直接换算。'], h3: ['先计算，', '再换算。'],
    h4: ['选中价格，', '立即换算。'], h5: ['你的颜色，', '浅色或深色。'], cur: '种货币', off: '离线也能用', sets: '套颜色主题',
  },
  ja: {
    h1: ['どんな価格も、', 'あなたの通貨で。'], h2: ['ホーム画面から', 'すぐ換算。'], h3: ['まず計算して、', 'そのまま換算。'],
    h4: ['価格を選んで、', 'その場で換算。'], h5: ['好きな色で、', 'ライトもダークも。'],
    cur: '通貨', off: 'オフラインでも使える', sets: 'カラーテーマ',
  },
  hi: {
    h1: ['हर क़ीमत,', 'आपकी मुद्रा में।'], h2: ['होम स्क्रीन से', 'सीधे बदलें।'], h3: ['पहले हिसाब,', 'फिर बदलें।'],
    h4: ['कोई क़ीमत चुनें,', 'तुरंत बदली हुई।'], h5: ['आपके रंग,', 'लाइट या डार्क।'],
    cur: 'मुद्राएँ', off: 'ऑफ़लाइन भी चलता है', sets: 'रंग थीम',
  },
};

// Headline fonts: Secular One covers Hebrew and Latin; the rest fall back per script.
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

/** A @fontsource package, fetched into tools/store/.fonts once; returns <link> tags for its CSS files. */
function fontsource(pkg, files) {
  const dir = path.join(FONTS, pkg);
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
    const tgz = execSync(`npm pack @fontsource/${pkg} --silent`, { cwd: dir }).toString().trim().split('\n').pop();
    execSync(`tar xzf ${tgz} && rm ${tgz}`, { cwd: dir });
  }
  return files.map((f) => `<link rel="stylesheet" href="file://${dir}/package/${f}.css">`).join('\n');
}

const uri = (file) => 'data:image/png;base64,' + fs.readFileSync(file).toString('base64');
const shot = (lang, name) => uri(path.join(SRC, `store_${lang}_${name}.png`));
const esc = (t) => t.replace(/&/g, '&amp;').replace(/</g, '&lt;');
const ltr = (t) => `<bdi dir="ltr">${t}</bdi>`;

const W = 1080, H = 1920;
const INK = '#1F2C35'; // the app's slate
const SCREEN_RATIO = 0.447; // phone screen width / height: the renders get a status strip on top to fill it

// One look per screenshot: the launcher-icon colours of the colour set its screen shows.
const LOOKS = [
  { bg: '#93C9A3', fg: INK, blockBg: INK, blockFg: '#E3F1E6' }, // Classic (sage): the main screen
  { bg: '#E2AE6E', fg: '#3A2A1E', blockBg: '#FBEBC6', blockFg: '#3A2A1E' }, // Sand: the home screen widgets
  { bg: '#3796CF', fg: '#FFFFFF', blockBg: '#9ADAF6', blockFg: '#163E5C' }, // Sea: the calculator
  { bg: '#F27E7C', fg: '#3C1F2A', blockBg: '#FFC48E', blockFg: '#3C1F2A' }, // Sunset: the bubble
  { bg: '#6252C2', fg: '#FFFFFF', blockBg: '#FFE7A6', blockFg: '#2A2650' }, // Night: the colour sets
];

/**
 * A phone: the slate bezel with a hard shadow, the capture under a status strip in its own top colour (sampled in
 * the page). cropTop hides the top of the capture, like a page scrolled a little, so a tall screen's focus fits.
 */
function phone(content, { w, x, y, rot = 0, cropTop = 0, strip = true }) {
  // Bezel, padding and hard shadow scale with the phone (740 px wide in the screenshots).
  const k = w / 740, border = Math.round(10 * k), pad = Math.round(16 * k), shadow = Math.round(26 * k);
  const inner = w - 2 * (border + pad), full = inner / SCREEN_RATIO, bar = strip ? full - inner * (1720 / 800) : 0;
  return `<div class="phone" style="left:${x}px;top:${y}px;width:${w}px;transform:rotate(${rot}deg);border-width:${border}px;padding:${pad}px;
    border-radius:${Math.round(64 * k)}px;box-shadow:${shadow}px ${shadow}px 0 ${INK}">
    <div class="screen" style="height:${full * (1 - cropTop)}px;border-radius:${w * 0.07}px">
      <div style="margin-top:${-full * cropTop}px">${content(inner, bar)}</div></div></div>`;
}
const appScreen = (src) => (inner, bar) =>
  `<div class="strip" style="height:${bar}px"></div><img data-strip src="${src}" style="width:${inner}px;display:block">`;

/** The home screen around both widgets (status bar, apps, dock): tools/store/scenes.mjs. */
const homeScreen = (lang) => (inner) =>
  scenes.homeScreen({ width: inner, large: shot(lang, '2a_widget'), small: shot(lang, '2b_widget') });

/**
 * Quick convert on a booking page: the price selected, the selection menu with the app's item, a hand tapping it,
 * and the app's real bubble (the bottom of the 4_quick capture) over the page.
 */
function quickScreen(lang, rtl) {
  const s = strings(lang), menu = [...scenes.SELECTION[lang], s.quick_convert_label];
  return (inner, bar) => {
    const full = inner / SCREEN_RATIO, k = inner / 800, top = 1196, scroll = 150, dp = inner / scenes.DP_W;
    const tap = scenes.menuItemDp(menu, rtl);
    return `<div style="position:relative;width:${inner}px;height:${full}px">
      ${scenes.tourPage({ width: inner, height: full, scroll, selected: true, menu, rtl })}
      <div style="position:absolute;left:${tap.x * dp - 0.39 * 90}px;top:${(tap.y - scroll) * dp - 0.1 * 90}px">${scenes.hand(90)}</div>
      <div style="position:absolute;left:0;top:${bar + top * k}px;width:${inner}px;height:${(1720 - top) * k}px;overflow:hidden">
        <img src="${shot(lang, '4_quick')}" style="position:absolute;left:0;top:${-top * k}px;width:${inner}px"></div></div>`;
  };
}

/** A sticker pinned to the start or end side (start is the right side in RTL), tilted, with a hard shadow. */
const sticker = (rtl, text, { side, inset, y, rot, bg, fg = INK, size = 54 }) => {
  const right = (side === 'start') === rtl;
  return `<div class="sticker" style="${right ? 'right' : 'left'}:${inset}px;top:${y}px;transform:rotate(${right ? rot : -rot}deg);
    background:${bg};color:${fg};font-size:${size}px">${text}</div>`;
};

function page(lang, body, look, { width = W, height = H } = {}) {
  const rtl = RTL.has(lang);
  const noto = NOTO[lang] ? fontsource(NOTO[lang], ['700']) : '';
  const family = `${SECULAR.has(lang) ? "'Secular One', " : ''}Rubik, ${NOTO_FAMILY[lang] ? `'${NOTO_FAMILY[lang]}', ` : ''}sans-serif`;
  const rubik = (w, f) => `@font-face { font-family: Rubik; font-weight: ${w}; src: url('file://${RES}/font/rubik_${f}.ttf'); }`;
  return `<!doctype html><html lang="${lang}"><head><meta charset="utf-8">
${fontsource('secular-one', ['400'])}
${fontsource('jetbrains-mono', ['500'])}
${noto}
<style>
${rubik(400, 'regular')} ${rubik(500, 'medium')} ${rubik(700, 'bold')}
* { box-sizing: border-box; }
html, body { margin: 0; width: ${width}px; height: ${height}px; overflow: hidden; }
body { position: relative; background: ${look.bg}; color: ${look.fg}; font-family: ${family}; font-weight: ${SECULAR.has(lang) ? 400 : 700}; }
.rtl, h1, .sticker { direction: ${rtl ? 'rtl' : 'ltr'}; }
.frame { position: absolute; inset: 28px; border: 8px solid ${look.fg}; pointer-events: none; z-index: 6; }
.tier { position: absolute; top: 62px; ${rtl ? 'right' : 'left'}: 62px; padding: 4px 18px 8px; background: ${INK}; color: #FFE7A6;
  font-size: 40px; border-radius: 6px; direction: ${rtl ? 'rtl' : 'ltr'}; }
.tier b { color: #E2AF48; font-weight: inherit; }
${SECULAR.has(lang) ? 'h1, .sticker, .fit, .tier { -webkit-text-stroke: .035em currentColor; paint-order: stroke fill; }' : ''}
h1 { position: absolute; top: 140px; left: 62px; right: 62px; margin: 0; font-weight: inherit; font-size: 172px; line-height: .92;
  text-align: start; ${CJK.has(lang) ? 'word-break: keep-all; line-break: strict; line-height: 1.06;' : ''} }
h1 .block { display: inline-block; max-width: 100%; margin-top: 18px; padding: .02em .14em .1em; background: ${look.blockBg};
  color: ${look.blockFg}; border: 8px solid ${INK}; box-shadow: 14px 14px 0 ${INK}; }
.phone { position: absolute; border: 10px solid ${INK}; border-radius: 64px; background: ${INK}; padding: 16px;
  box-shadow: 26px 26px 0 ${INK}; }
.screen { overflow: hidden; background: #000; }
.strip { width: 100%; }
.sticker { position: absolute; padding: 10px 26px 16px; border: 7px solid ${INK}; box-shadow: 10px 10px 0 ${INK};
  white-space: nowrap; z-index: 5; }
</style></head><body>${body}</body></html>`;
}

const headline = ([plain, payoff]) => `<h1><span class="plain">${esc(plain)}</span><br><span class="block">${esc(payoff)}</span></h1>`;
const chrome = () => '<div class="frame"></div>';
const tier = (s) => `<div class="tier"><b>★</b> ${esc(s.premium)}</div>`;

// The headline may take the space above the phones (PHONE_TOP - 60); every phone starts at the same height.
const PHONE_TOP = 880;

function screenshots(lang) {
  const t = COPY[lang], s = strings(lang), rtl = RTL.has(lang);
  const stk = (text, o) => sticker(rtl, text, o);
  const single = (content, rot, extra = {}) => phone(content, { w: 740, x: 170, y: PHONE_TOP + 20, rot: rtl ? -rot : rot, ...extra });
  return [
    headline(t.h1) + single(appScreen(shot(lang, '1_main')), 4)
      + stk(`${ltr('160+')} ${esc(t.cur)}`, { side: 'end', inset: 70, y: PHONE_TOP - 20, rot: 6, bg: '#E2AF48' })
      + stk(esc(t.off), { side: 'start', inset: 70, y: 1500, rot: -5, bg: '#FFFFFF' }),
    tier(s) + headline(t.h2) + single(homeScreen(lang), -4, { strip: false }),
    tier(s) + headline(t.h3) + single(appScreen(shot(lang, '3_calc')), 3)
      + stk(ltr('120 + 15%'), { side: 'end', inset: 70, y: PHONE_TOP - 20, rot: 7, bg: '#E2F1FA', size: 60 }),
    // The bubble sits at the bottom of the screen: a narrower phone, its top hidden, shows all of it.
    headline(t.h4) + phone(quickScreen(lang, rtl), { w: 600, x: 240, y: PHONE_TOP, rot: rtl ? -3 : 3, cropTop: 0.2 })
      + stk(esc(s.tip_select_title), { side: 'start', inset: 50, y: 1440, rot: -6, bg: '#E3F1E6' }),
    tier(s) + headline(t.h5)
      + phone(appScreen(shot(lang, '5c_theme')), { w: 560, x: rtl ? 40 : 480, y: PHONE_TOP + 110, rot: rtl ? -8 : 8 })
      + phone(appScreen(shot(lang, '5a_theme')), { w: 600, x: rtl ? 440 : 40, y: PHONE_TOP + 40, rot: rtl ? 5 : -5 })
      + stk(`${ltr('6')} ${esc(t.sets)}`, { side: 'end', inset: 60, y: PHONE_TOP - 30, rot: -5, bg: '#E2AF48' }),
  ];
}

/** The feature graphic: the classic look, the icon in a bordered tile, the name, the first headline and a phone. */
function featureGraphic(lang, title) {
  const rtl = RTL.has(lang), t = COPY[lang];
  return `<div class="frame" style="inset:14px;border-width:5px"></div>
    <div class="rtl" style="position:absolute;top:0;bottom:0;${rtl ? 'right' : 'left'}:56px;width:560px;display:flex;flex-direction:column;justify-content:center">
      <img src="${uri(path.join(ROOT, 'store/icon-512.png'))}" style="width:104px;height:104px;border-radius:24px;border:5px solid ${INK};box-shadow:8px 8px 0 ${INK}">
      <div class="fit" style="font-size:66px;line-height:1;margin-top:28px;white-space:nowrap">${esc(title)}</div>
      <div><span class="fit" style="display:inline-block;margin-top:18px;padding:4px 16px 10px;font-size:38px;white-space:nowrap;background:${INK};
        color:#E3F1E6;border:5px solid ${INK};box-shadow:8px 8px 0 #E3F1E6">${esc(t.h1.join(' '))}</span></div>
    </div>
    ${phone(appScreen(shot(lang, '1_main')), { w: 270, x: rtl ? 70 : 684, y: 64, rot: rtl ? -6 : 6 })}`;
}

/** Shrinks the headline until it fits above the phones (and no word overflows), and .fit lines to their box. */
async function fit(tab, maxBottom) {
  return tab.evaluate(async (maxBottom) => {
    for (const img of document.querySelectorAll('img[data-strip]')) {
      await img.decode();
      const c = document.createElement('canvas'); c.width = c.height = 1;
      const x = c.getContext('2d'); x.drawImage(img, 2, 2, 1, 1, 0, 0, 1, 1);
      const [r, g, b] = x.getImageData(0, 0, 1, 1).data;
      img.previousElementSibling.style.background = `rgb(${r},${g},${b})`;
    }
    const h1 = document.querySelector('h1');
    let size = h1 ? parseFloat(getComputedStyle(h1).fontSize) : 0;
    const over = () => h1.getBoundingClientRect().bottom > maxBottom
      || [...h1.children].some((el) => el.scrollWidth > el.clientWidth + 1 || el.getBoundingClientRect().width > h1.clientWidth + 1);
    while (h1 && over() && size > 84) { size -= 4; h1.style.fontSize = size + 'px'; }
    for (const el of document.querySelectorAll('.fit')) {
      const box = el.parentElement.closest('.rtl') || el.parentElement;
      let s = parseFloat(getComputedStyle(el).fontSize);
      const width = () => Math.max(el.scrollWidth, el.getBoundingClientRect().width);
      while (width() > box.clientWidth && s > 20) { s -= 2; el.style.fontSize = s + 'px'; }
    }
    // A long sticker (a translation) shrinks until it sits inside the frame.
    for (const el of document.querySelectorAll('.sticker')) {
      let s = parseFloat(getComputedStyle(el).fontSize);
      const out = () => { const r = el.getBoundingClientRect(); return r.left < 24 || r.right > innerWidth - 24; };
      while (out() && s > 24) { s -= 2; el.style.fontSize = s + 'px'; }
    }
    return size;
  }, maxBottom);
}

const browser = await chromium.launch();
const tab = await (await browser.newContext({ deviceScaleFactor: 1 })).newPage();
// Pages load from a file next to the fonts: a page set with setContent (about:blank) can't read file:// fonts or CSS,
// and silently falls back to the system fonts.
const PAGE = path.join(FONTS, 'page.html');
async function save(html, size, file, maxBottom) {
  await tab.setViewportSize(size);
  fs.mkdirSync(FONTS, { recursive: true });
  fs.writeFileSync(PAGE, html);
  await tab.goto('file://' + PAGE, { waitUntil: 'networkidle' });
  await tab.evaluate(() => document.fonts.ready);
  const failed = await tab.evaluate(() => [...document.fonts].filter((f) => f.status === 'error').map((f) => f.family));
  if (failed.length) throw new Error('fonts failed to load: ' + [...new Set(failed)].join(', '));
  const h = await fit(tab, maxBottom);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  await tab.screenshot({ path: file, type: 'jpeg', quality: 92 });
  return h;
}

for (const [lang, locale] of Object.entries(LOCALES)) {
  if (only && lang !== only) continue;
  const dir = path.join(OUT, locale, 'images');
  fs.rmSync(path.join(dir, 'phoneScreenshots'), { recursive: true, force: true });
  const sizes = [];
  for (const [i, body] of screenshots(lang).entries()) {
    sizes.push(await save(page(lang, body + chrome(), LOOKS[i]), { width: W, height: H },
      path.join(dir, 'phoneScreenshots', `${i + 1}.jpg`), PHONE_TOP - 50));
  }
  const title = fs.readFileSync(path.join(OUT, locale, 'title.txt'), 'utf8').trim();
  await save(page(lang, featureGraphic(lang, title), LOOKS[0], { width: 1024, height: 500 }), { width: 1024, height: 500 },
    path.join(dir, 'featureGraphic.jpg'), 500);
  console.log(`${lang}: ${locale}  headline px ${sizes.join(' ')}`);
}
await browser.close();
