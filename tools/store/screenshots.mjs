// Google Play images for every listing language: five phone screenshots (1080x2160 JPEG) and the feature graphic
// (1024x500 JPEG; tools/store/listing.py adds the icon), into store/listing/<locale>/images (fastlane layout).
// Sister locales (en-GB, es-419...) get copies at upload time (.github/workflows/play-listing.yml), so git keeps one
// set per language.
//
// Style: "Bento keynote grid" from the app-store-screenshots skill (github.com/ParthJadhav/app-store-screenshots),
// on a white page as the owner asked: a two-line headline with one gradient line, a crown of two rounded tiles
// (one stat or label in the brand green, one icon + short label), and a tall stage tile whose bottom edge crops
// an upright phone in a gap between UI rows. Everything snaps to a 12-column grid (56px margins, 40px gutters)
// on a 1320x2640 design canvas (Play allows up to 1:2), scaled to 1080x2160.
//
// The app screens come from the desktop preview (tools/preview, ONLY=store_ gradle -q run -> out/store_*.png).
//
//   PATH=/opt/node22/bin:$PATH NODE_PATH=$(npm root -g) node tools/store/screenshots.mjs [--only iw] [--png]
//
// Arabic, Hindi, Chinese and Japanese use Noto fonts from npm (@fontsource), fetched into tools/store/.fonts on the
// first run.
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
const arg = (name) => (process.argv.includes(name) ? process.argv[process.argv.indexOf(name) + 1] : null);
const only = arg('--only');

// Preview language -> its Google Play locale.
const LOCALES = {
  iw: 'iw-IL', en: 'en-US', es: 'es-ES', ar: 'ar', ru: 'ru-RU', fr: 'fr-FR', de: 'de-DE', pt: 'pt-BR', it: 'it-IT',
  tr: 'tr-TR', zh: 'zh-CN', ja: 'ja-JP', hi: 'hi-IN',
};
const RTL = new Set(['iw', 'ar']);
const CJK = new Set(['zh', 'ja']);

// The words on the screenshots. Headlines are two beats: a plain line and a gradient line. Every claim is true
// for the app: 160+ currencies, offline rates, a 3-day trial, 2 rewarded videos -> 24 hours, 6 colour sets
// that the widget follows, the selection menu and the share sheet.
const COPY = {
  en: {
    h1: ['Every price.', 'In your currency.'], cur: 'Currencies', off: 'Works offline.',
    h2: ['Convert right from', 'your home screen.'], free: 'days free', sizes: '2 widget sizes.',
    h3: ['Calculate first.', 'Convert as you go.'], hours: 'hours', vid: 'for 2 short videos', pct: 'Percent built in.',
    h4: ['Select a price.', 'See it converted.'], press: 'Long-press any price.', share: 'Or use Share.',
    h5: ['Your colors.', 'Light or dark.'], sets: 'color sets', match: 'The widget matches.',
  },
  iw: {
    h1: ['כל מחיר בעולם.', 'בשקלים, מיד.'], cur: 'מטבעות בעולם', off: 'גם בלי אינטרנט.',
    h2: ['ההמרה ישר', 'מהמסך הראשי.'], free: 'ימים חינם', sizes: "2 גדלים של וידג'ט.",
    h3: ['מחשבים קודם.', 'ממירים תוך כדי.'], hours: 'שעות', vid: 'תמורת 2 סרטונים קצרים', pct: 'אחוזים מובנים.',
    h4: ['מסמנים מחיר.', 'ההמרה קופצת.'], press: 'לחיצה ארוכה על מחיר.', share: 'או דרך שיתוף.',
    h5: ['הצבעים שלך.', 'בהיר או כהה.'], sets: 'ערכות צבעים', match: "גם הווידג'ט מתחלף.",
  },
  es: {
    h1: ['Cada precio.', 'En tu moneda.'], cur: 'Monedas', off: 'Funciona sin conexión.',
    h2: ['Convierte desde', 'tu pantalla de inicio.'], free: 'días gratis', sizes: '2 tamaños de widget.',
    h3: ['Calcula primero.', 'Convierte al momento.'], hours: 'horas', vid: 'por 2 vídeos cortos', pct: 'Porcentajes incluidos.',
    h4: ['Selecciona un precio.', 'Míralo convertido.'], press: 'Mantén pulsado un precio.', share: 'O usa Compartir.',
    h5: ['Tus colores.', 'Claro u oscuro.'], sets: 'temas de color', match: 'El widget combina.',
  },
  ar: {
    h1: ['كل سعر.', 'بعملتك أنت.'], cur: 'عملة', off: 'يعمل دون إنترنت.',
    h2: ['حوّل مباشرة من', 'الشاشة الرئيسية.'], free: 'أيام مجانًا', sizes: 'حجمان للأداة.',
    h3: ['احسب أولًا.', 'وحوّل فورًا.'], hours: 'ساعة', vid: 'مقابل فيديوهين قصيرين', pct: 'النسب المئوية مدمجة.',
    h4: ['حدّد سعرًا.', 'وشاهده محوّلًا.'], press: 'اضغط مطولًا على أي سعر.', share: 'أو استخدم المشاركة.',
    h5: ['ألوانك.', 'فاتح أو داكن.'], sets: 'سمات ألوان', match: 'والأداة تتبعها.',
  },
  ru: {
    h1: ['Любая цена.', 'В вашей валюте.'], cur: 'Валют', off: 'Работает без интернета.',
    h2: ['Конвертируйте', 'с главного экрана.'], free: 'дня бесплатно', sizes: '2 размера виджета.',
    h3: ['Сначала посчитайте.', 'Сразу в валюте.'], hours: 'часа', vid: 'за 2 коротких видео', pct: 'Проценты встроены.',
    h4: ['Выделите цену.', 'Она уже в рублях.'], press: 'Долгое нажатие на цену.', share: 'Или «Поделиться».',
    h5: ['Ваши цвета.', 'Светлые и тёмные.'], sets: 'цветовых тем', match: 'Виджет в тон.',
  },
  fr: {
    h1: ['Chaque prix.', 'Dans votre devise.'], cur: 'Devises', off: 'Fonctionne hors ligne.',
    h2: ['Convertissez depuis', "l'écran d'accueil."], free: 'jours offerts', sizes: '2 tailles de widget.',
    h3: ["Calculez d'abord.", 'Convertissez aussitôt.'], hours: 'heures', vid: 'pour 2 courtes vidéos', pct: 'Pourcentages intégrés.',
    h4: ['Sélectionnez un prix.', 'Voyez-le converti.'], press: 'Appui long sur un prix.', share: 'Ou via Partager.',
    h5: ['Vos couleurs.', 'Clair ou sombre.'], sets: 'thèmes de couleur', match: 'Le widget suit.',
  },
  de: {
    h1: ['Jeder Preis.', 'In deiner Währung.'], cur: 'Währungen', off: 'Funktioniert offline.',
    h2: ['Umrechnen direkt', 'vom Startbildschirm.'], free: 'Tage gratis', sizes: '2 Widget-Größen.',
    h3: ['Erst rechnen.', 'Dann umrechnen.'], hours: 'Stunden', vid: 'für 2 kurze Videos', pct: 'Prozente inklusive.',
    h4: ['Preis markieren.', 'Sofort umgerechnet.'], press: 'Lange auf einen Preis tippen.', share: 'Oder über Teilen.',
    h5: ['Deine Farben.', 'Hell oder dunkel.'], sets: 'Farbschemas', match: 'Das Widget passt sich an.',
  },
  pt: {
    h1: ['Cada preço.', 'Na sua moeda.'], cur: 'Moedas', off: 'Funciona offline.',
    h2: ['Converta direto da', 'tela inicial.'], free: 'dias grátis', sizes: '2 tamanhos de widget.',
    h3: ['Calcule primeiro.', 'Converta na hora.'], hours: 'horas', vid: 'por 2 vídeos curtos', pct: 'Porcentagem embutida.',
    h4: ['Selecione um preço.', 'Veja convertido.'], press: 'Toque e segure um preço.', share: 'Ou use Compartilhar.',
    h5: ['Suas cores.', 'Claro ou escuro.'], sets: 'temas de cores', match: 'O widget acompanha.',
  },
  it: {
    h1: ['Ogni prezzo.', 'Nella tua valuta.'], cur: 'Valute', off: 'Funziona offline.',
    h2: ['Converti dalla', 'schermata Home.'], free: 'giorni gratis', sizes: '2 misure di widget.',
    h3: ['Prima calcola.', 'Poi converti.'], hours: 'ore', vid: 'con 2 brevi video', pct: 'Percentuali incluse.',
    h4: ['Seleziona un prezzo.', 'Eccolo convertito.'], press: 'Tieni premuto un prezzo.', share: 'Oppure Condividi.',
    h5: ['I tuoi colori.', 'Chiaro o scuro.'], sets: 'temi colore', match: 'Anche il widget.',
  },
  tr: {
    h1: ['Her fiyat.', 'Kendi paranla.'], cur: 'Para birimi', off: 'İnternetsiz çalışır.',
    h2: ['Ana ekrandan', 'hemen çevir.'], free: 'gün ücretsiz', sizes: '2 widget boyutu.',
    h3: ['Önce hesapla.', 'Sonra çevir.'], hours: 'saat', vid: '2 kısa video karşılığı', pct: 'Yüzde hesabı dahil.',
    h4: ['Bir fiyat seç.', 'Çevrilmiş gör.'], press: 'Fiyata uzun bas.', share: "Ya da Paylaş'ı kullan.",
    h5: ['Senin renklerin.', 'Açık ya da koyu.'], sets: 'renk teması', match: 'Widget da uyar.',
  },
  zh: {
    h1: ['每一个价格，', '换成你的货币。'], cur: '种货币', off: '离线也能用。',
    h2: ['在主屏幕上', '直接换算。'], free: '天免费', sizes: '2 种小部件尺寸。',
    h3: ['先计算，', '再换算。'], hours: '小时', vid: '看 2 段短视频即可', pct: '内置百分比。',
    h4: ['选中价格，', '立即换算。'], press: '长按任意价格。', share: '或使用分享。',
    h5: ['你的颜色，', '浅色或深色。'], sets: '套颜色主题', match: '小部件同步变色。',
  },
  ja: {
    h1: ['どんな価格も、', 'あなたの通貨で。'], cur: '通貨', off: 'オフラインでも使える。',
    h2: ['ホーム画面から', 'すぐ換算。'], free: '日間無料', sizes: 'ウィジェットは2サイズ。',
    h3: ['まず計算して、', 'そのまま換算。'], hours: '時間', vid: '短い動画2本で', pct: 'パーセント計算も。',
    h4: ['価格を選んで、', 'その場で換算。'], press: '価格を長押し。', share: '共有からも使える。',
    h5: ['好きな色で、', 'ライトもダークも。'], sets: 'カラーテーマ', match: 'ウィジェットも連動。',
  },
  hi: {
    h1: ['हर क़ीमत,', 'आपकी मुद्रा में।'], cur: 'मुद्राएँ', off: 'ऑफ़लाइन भी चलता है।',
    h2: ['होम स्क्रीन से', 'सीधे बदलें।'], free: 'दिन मुफ़्त', sizes: 'विजेट के 2 साइज़।',
    h3: ['पहले हिसाब,', 'फिर बदलें।'], hours: 'घंटे', vid: '2 छोटे वीडियो देखकर', pct: 'प्रतिशत भी शामिल।',
    h4: ['कोई क़ीमत चुनें,', 'तुरंत बदली हुई।'], press: 'क़ीमत पर देर तक दबाएँ।', share: 'या शेयर से।',
    h5: ['आपके रंग,', 'लाइट या डार्क।'], sets: 'रंग थीम', match: 'विजेट भी साथ बदले।',
  },
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
const esc = (t) => t.replace(/&/g, '&amp;').replace(/</g, '&lt;');

// ---- The grid (design canvas 1320x2640) ----
const W = 1320, H = 2640, OUT_W = 1080, OUT_H = 2160;
const col = (n) => 56 + (n - 1) * 104, span = (k) => 64 * k + 40 * (k - 1);
const HEAD_TOP = 88, CROWN_TOP = 387, CROWN_H = 320, STAGE_TOP = CROWN_TOP + CROWN_H + 40, STAGE_BOTTOM = H - 56;
const STAGE_H = STAGE_BOTTOM - STAGE_TOP;

const C = {
  ground: '#FFFFFF', tile: '#F4F5F7', ink: '#1D1D1F', ink2: '#5E5E63', brand: '#1F7A4D', brandEyebrow: '#E3F4EA',
  sage: '#E6F0E6', sky: '#E5EDF7', warm: '#F6EBE1', night: '#1D1D1F',
};
// The finance stops of the style: every one clears 4.5:1 on white.
const GRADIENT = ['#1F7A4D', '#0B6E8A', '#1F57C3'];

// ---- The phone: a dark Android bezel with a punch-hole camera ----
const SHOT_RATIO = 1720 / 800; // the preview renders
const SCREEN_W = 640, SCREEN_H = 1376; // the home-screen mock's own coordinates

/** Phone geometry for a screen width: bezel, the status strip above the app (0 when the content fills it). */
function geometry(screenW, strip = true) {
  const bezel = Math.round(screenW * 0.022), bar = strip ? Math.round(screenW * 0.06) : 0;
  return { screenW, bezel, bar, width: screenW + 2 * bezel, height: Math.round(screenW * SHOT_RATIO) + bar + 2 * bezel };
}

/**
 * The phone inside a stage tile, as wide as fits: the stage's bottom edge cuts the app render at `cut` (a fraction
 * of its height, chosen in a gap between UI rows), with `topPad` above the phone. cut >= 1 shows the whole phone.
 */
function stagePhone(content, { cut, strip = true, topPad = 56, maxW = 1100 }) {
  const room = STAGE_H - topPad;
  const perW = (g) => (cut >= 1 ? g.height : g.bezel + g.bar + cut * g.screenW * SHOT_RATIO) / g.screenW;
  let screenW = Math.min(room / perW(geometry(1000, strip)), maxW / 1.044);
  const g = geometry(Math.floor(screenW), strip);
  const left = (span(12) - g.width) / 2;
  return {
    visible: Math.min(g.height, room),
    html: `<div class="phone" style="left:${left}px;top:${topPad}px;width:${g.width}px;height:${g.height}px;padding:${g.bezel}px;
      border-radius:${Math.round(g.width * 0.115)}px">
      <div class="screen" style="border-radius:${Math.round(g.width * 0.1)}px">
        ${content(g)}
        <div class="cam" style="width:${Math.round(g.screenW * 0.026)}px;height:${Math.round(g.screenW * 0.026)}px;top:${Math.round(g.screenW * 0.018)}px"></div>
      </div></div>`,
  };
}

/** An app render under a strip in the colour of its top row (sampled in the page). */
const appScreen = (src) => (g) =>
  `<div class="strip" style="height:${g.bar}px"></div><img class="app" data-strip src="${src}" style="top:${g.bar}px;width:${g.screenW}px">`;

/** The home screen: wallpaper, the clock, the large and the small widget (in the mock's 640-wide coordinates). */
function homeScreen(lang) {
  const dp = SCREEN_W / 400;
  return (g) => `<div style="position:absolute;left:0;top:0;width:${SCREEN_W}px;height:${SCREEN_H}px;transform:scale(${g.screenW / SCREEN_W});transform-origin:0 0">
    <div style="position:absolute;inset:0;background:linear-gradient(160deg,#f3d9c4 0%,#c98f7a 55%,#6d4a5c 100%)"></div>
    <div style="position:absolute;top:${40 * dp}px;width:100%;text-align:center;color:#fff;font-size:${56 * dp}px;font-weight:500;
      font-family:Roboto,sans-serif;text-shadow:0 2px 12px rgba(0,0,0,.2)" dir="ltr">09:41</div>
    <img src="${shot(lang, '2a_widget')}" style="position:absolute;top:${130 * dp}px;left:${50 * dp}px;width:${300 * dp}px;height:${380 * dp}px;
      filter:drop-shadow(0 8px 18px rgba(0,0,0,.25))">
    <img src="${shot(lang, '2b_widget')}" style="position:absolute;top:${528 * dp}px;left:${50 * dp}px;width:${300 * dp}px;height:${130 * dp}px;
      filter:drop-shadow(0 8px 18px rgba(0,0,0,.25))">
    <div style="position:absolute;bottom:${36 * dp}px;left:0;right:0;display:flex;justify-content:center;gap:${26 * dp}px">
      ${['#f4f1ec', '#cfe3f5', '#d9ead3', '#f7dccf'].map((c) => `<div style="width:${54 * dp}px;height:${54 * dp}px;border-radius:50%;background:${c};opacity:.9"></div>`).join('')}
    </div></div>`;
}

// ---- Line icons (24 viewBox, stroke 2, round caps), drawn at 58px in a 104px well ----
const ICON = {
  offline: '<path d="M4 4l16 16"/><path d="M8.5 16.4a5 5 0 0 1 7 0"/><path d="M5 12.9a10 10 0 0 1 5.2-2.7"/><path d="M14.2 10.3A10 10 0 0 1 19 12.9"/><path d="M1.8 9.4a14.5 14.5 0 0 1 4.6-2.9"/><path d="M10.6 5.6A14.5 14.5 0 0 1 22.2 9.4"/><path d="M12 19.6h.01"/>',
  widgets: '<rect x="3" y="3" width="18" height="10" rx="3"/><rect x="3" y="16" width="8" height="5" rx="2"/><rect x="13" y="16" width="8" height="5" rx="2"/>',
  percent: '<path d="M19 5L5 19"/><circle cx="6.5" cy="6.5" r="2.5"/><circle cx="17.5" cy="17.5" r="2.5"/>',
  select: '<path d="M5 9h14v6H5z"/><path d="M5 6v12"/><path d="M19 6v12"/><circle cx="5" cy="5" r="1.6"/><circle cx="19" cy="19" r="1.6"/>',
  share: '<circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><path d="M8.6 10.5l6.8-4M8.6 13.5l6.8 4"/>',
  contrast: '<circle cx="12" cy="12" r="9"/><path d="M12 3a9 9 0 0 1 0 18z" fill="currentColor"/>',
};
const icon = (name, color) => `<svg viewBox="0 0 24 24" width="58" height="58" fill="none" stroke="${color}" stroke-width="2"
  stroke-linecap="round" stroke-linejoin="round" style="color:${color}">${ICON[name]}</svg>`;

// ---- Tiles ----
/** Crown tiles: `side` 0 is the first column of the reading direction (the right one in RTL). */
function crownTile(rtl, side, cls, inner) {
  const x = (side === 0) === !rtl ? col(1) : col(7);
  return `<div class="tile ${cls}" style="left:${x}px;top:${CROWN_TOP}px;width:${span(6)}px;height:${CROWN_H}px"><div class="pad">${inner}</div></div>`;
}
const statTile = (eyebrows, num, unit) => `
  ${eyebrows.map((e) => `<div class="eyebrow fit">${esc(e)}</div>`).join('')}
  <div class="statrow"><span class="stat" dir="ltr">${num}</span>${unit ? `<span class="unit fit">${esc(unit)}</span>` : ''}</div>`;
const labelTile = (iconName, label, iconColor = C.ink) => `
  <div class="well">${icon(iconName, iconColor)}</div><div class="label fit2">${esc(label)}</div>`;

function page(lang, body, { width, height }) {
  const rtl = RTL.has(lang);
  const family = NOTO_FAMILY[lang] ? `'${NOTO_FAMILY[lang]}', ` : '';
  const font = (w, f) => `@font-face { font-family: Rubik; font-weight: ${w}; src: url('file://${RES}/font/rubik_${f}.ttf'); }`;
  const tight = CJK.has(lang) || NOTO_FAMILY[lang] ? 0 : -0.02;
  return `<!doctype html><html lang="${lang}"><head><meta charset="utf-8">
${notoCss(lang)}
<style>
${font(500, 'medium')} ${font(600, 'semibold')} ${font(700, 'bold')}
* { box-sizing: border-box; }
html, body { margin: 0; width: ${width}px; height: ${height}px; overflow: hidden; }
body { background: ${C.ground}; color: ${C.ink}; font-family: Rubik, ${family}sans-serif; position: relative; }
.rtl, .tile, h1 { direction: ${rtl ? 'rtl' : 'ltr'}; }
h1 { position: absolute; top: ${HEAD_TOP}px; left: ${col(1)}px; width: ${span(12)}px; margin: 0; text-align: center;
  font-size: 132px; font-weight: 700; line-height: ${CJK.has(lang) ? 1.12 : 0.98}; letter-spacing: ${tight}em; }
h1 div { white-space: nowrap; }
.g { background: linear-gradient(${rtl ? 270 : 90}deg, ${GRADIENT[0]} 0%, ${GRADIENT[1]} 52%, ${GRADIENT[2]} 100%);
  -webkit-background-clip: text; background-clip: text; color: transparent; padding: 0 .05em; }
.tile { position: absolute; border-radius: 60px; background: ${C.tile}; overflow: hidden; }
.tile .pad { position: absolute; inset: 40px 44px; display: flex; flex-direction: column; }
.tile.brand { background: ${C.brand}; color: #fff; }
.tile.brand .eyebrow { color: ${C.brandEyebrow}; }
.eyebrow { font-size: 32px; font-weight: 600; line-height: 1.15; color: ${C.ink2}; white-space: nowrap; }
.statrow { margin-top: auto; display: flex; align-items: baseline; gap: 16px; }
.stat { font-size: 190px; font-weight: 700; line-height: .8; letter-spacing: -.03em; unicode-bidi: isolate; }
.unit { font-size: 60px; font-weight: 700; line-height: 1; white-space: nowrap; }
.well { width: 104px; height: 104px; border-radius: 30px; background: ${C.ground}; display: flex; align-items: center; justify-content: center; }
.brand .well { background: rgba(255,255,255,.14); }
.label { margin-top: auto; font-size: 56px; font-weight: 700; line-height: 1.04; letter-spacing: ${tight}em; }
.stage { left: ${col(1)}px; top: ${STAGE_TOP}px; width: ${span(12)}px; height: ${STAGE_H}px; }
.phone { position: absolute; background: #16181A; box-shadow: inset 0 0 0 3px #2B2E31;
  filter: drop-shadow(0 34px 44px rgba(20,40,28,.16)) drop-shadow(0 4px 8px rgba(20,40,28,.12)); }
.night .phone { filter: drop-shadow(0 30px 60px rgba(0,0,0,.45)) drop-shadow(0 4px 8px rgba(0,0,0,.4)); box-shadow: inset 0 0 0 3px #3A3D41; }
.screen { position: relative; width: 100%; height: 100%; overflow: hidden; background: #000; }
.strip { position: absolute; top: 0; left: 0; right: 0; }
img.app { position: absolute; left: 0; display: block; }
.cam { position: absolute; left: 50%; transform: translateX(-50%); border-radius: 50%; background: #0B0C0D; z-index: 2; }
</style></head><body>${body}
</body></html>`;
}

/** Shrinks text that doesn't fit: headline lines together (both at one size), one-line .fit, two-line .fit2. */
async function fitText(tab) {
  return tab.evaluate(async () => {
    const report = [];
    // The strip above an app render takes the colour of the render's top-left pixel.
    for (const img of document.querySelectorAll('img[data-strip]')) {
      await img.decode();
      const c = document.createElement('canvas'); c.width = c.height = 1;
      const x = c.getContext('2d'); x.drawImage(img, 2, 2, 1, 1, 0, 0, 1, 1);
      const [r, g, b] = x.getImageData(0, 0, 1, 1).data;
      img.previousElementSibling.style.background = `rgb(${r},${g},${b})`;
    }
    const h1 = document.querySelector('h1');
    if (h1) {
      let size = parseFloat(getComputedStyle(h1).fontSize);
      const over = () => [...h1.querySelectorAll('span')].some((d) => d.getBoundingClientRect().width > h1.clientWidth * 0.96);
      while (over() && size > 96) { size -= 4; h1.style.fontSize = size + 'px'; }
      report.push('h1 ' + size);
    }
    for (const el of document.querySelectorAll('.fit')) {
      let size = parseFloat(getComputedStyle(el).fontSize);
      const box = el.closest('.pad');
      const over = () => el.closest('.statrow')
        ? el.closest('.statrow').scrollWidth > box.clientWidth : el.scrollWidth > box.clientWidth;
      while (over() && size > 22) { size -= 2; el.style.fontSize = size + 'px'; }
    }
    for (const el of document.querySelectorAll('.fit2')) {
      let size = parseFloat(getComputedStyle(el).fontSize);
      const lines = () => Math.round(el.getBoundingClientRect().height / (size * 1.04));
      while (lines() > 2 && size > 36) { size -= 2; el.style.fontSize = size + 'px'; }
      if (lines() > 2) report.push('label overflows: ' + el.textContent);
    }
    return report;
  });
}

function screenshots(lang) {
  const t = COPY[lang], s = strings(lang), rtl = RTL.has(lang);
  const premium = `★ ${s.premium}`;
  const headline = ([a, b]) => `<h1><div><span>${esc(a)}</span></div><div><span class="g">${esc(b)}</span></div></h1>`;
  const stage = (cls, p) => `<div class="tile stage ${cls}">${p.html}</div>`;
  // Cuts: the keypad gap between "4 5 6" and "1 2 3" (0.770-0.783), the home screen below the small widget, and
  // the whole bubble.
  const KEYPAD_GAP = 0.7765;
  return [
    headline(t.h1)
      + crownTile(rtl, 0, 'brand', statTile([t.cur], '160+'))
      + crownTile(rtl, 1, '', labelTile('offline', t.off))
      + stage('', stagePhone(appScreen(shot(lang, '1_main')), { cut: KEYPAD_GAP })) ,
    headline(t.h2)
      + crownTile(rtl, 0, 'brand', statTile([premium], '3', t.free))
      + crownTile(rtl, 1, '', labelTile('widgets', t.sizes))
      + stage('warm', stagePhone(homeScreen(lang), { cut: 0.86, strip: false })),
    headline(t.h3)
      + crownTile(rtl, 0, 'brand', statTile([premium, t.vid], '24', t.hours))
      + crownTile(rtl, 1, '', labelTile('percent', t.pct))
      + stage('sky', stagePhone(appScreen(shot(lang, '3_calc')), { cut: KEYPAD_GAP })),
    headline(t.h4)
      + crownTile(rtl, 0, 'brand', labelTile('select', t.press, '#fff'))
      + crownTile(rtl, 1, '', labelTile('share', t.share))
      + stage('', stagePhone(appScreen(shot(lang, '4_quick')), { cut: 1, topPad: 64 })),
    headline(t.h5)
      + crownTile(rtl, 0, 'brand', statTile([premium], '6', t.sets))
      + crownTile(rtl, 1, '', labelTile('contrast', t.match))
      + `<div class="tile stage night">${themes(lang)}</div>`,
  ];
}

/** Three colour sets fanned out on the night stage, the middle one in front. */
function themes(lang) {
  const g = geometry(560);
  const one = (name, rotate, dx, top, z) => `<div class="phone" style="left:${(span(12) - g.width) / 2 + dx}px;top:${top}px;
    width:${g.width}px;height:${g.height}px;padding:${g.bezel}px;border-radius:${Math.round(g.width * 0.115)}px;
    transform:rotate(${rotate}deg);transform-origin:50% 100%;z-index:${z}">
    <div class="screen" style="border-radius:${Math.round(g.width * 0.1)}px">${appScreen(shot(lang, name))(g)}
      <div class="cam" style="width:15px;height:15px;top:10px"></div></div></div>`;
  return one('5b_theme', -9, -300, 150, 1) + one('5c_theme', 9, 300, 150, 1) + one('5a_theme', 0, 0, 90, 2);
}

function featureGraphic(lang, title) {
  const rtl = RTL.has(lang), t = COPY[lang];
  const textSide = rtl ? 'right' : 'left', stageSide = rtl ? 'left' : 'right';
  const screenW = 320, g = geometry(screenW);
  return `<div class="rtl pad" style="position:absolute;top:0;bottom:0;${textSide}:56px;width:520px;display:flex;flex-direction:column;justify-content:center">
      <img src="${uri(path.join(ROOT, 'store/icon-512.png'))}" style="width:112px;height:112px;border-radius:26px;display:block">
      <div class="fit" style="font-size:52px;font-weight:700;line-height:1.1;margin-top:30px;white-space:nowrap">${esc(title)}</div>
      <div class="fit" style="font-size:34px;font-weight:700;line-height:1.3;margin-top:12px;white-space:nowrap"><span class="g">${esc(t.h1.join(' '))}</span></div>
    </div>
    <div class="tile" style="top:40px;${stageSide}:40px;width:392px;height:420px;background:${C.sage};border-radius:44px">
      <div class="phone" style="left:${(392 - g.width) / 2}px;top:40px;width:${g.width}px;height:${g.height}px;padding:${g.bezel}px;
        border-radius:${Math.round(g.width * 0.115)}px"><div class="screen" style="border-radius:${Math.round(g.width * 0.1)}px">
        ${appScreen(shot(lang, '1_main'))(g)}<div class="cam" style="width:9px;height:9px;top:6px"></div></div></div></div>`;
}

const browser = await chromium.launch();
const tab = await (await browser.newContext({ deviceScaleFactor: 1 })).newPage();
async function save(html, design, size, file) {
  await tab.setViewportSize(design);
  await tab.setContent(html, { waitUntil: 'networkidle' });
  await tab.evaluate(() => document.fonts.ready);
  const report = await fitText(tab);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  // Render at the design size, then let the browser scale the PNG down to the store size.
  const png = await tab.screenshot({ type: 'png' });
  const small = await browser.newPage({ viewport: size });
  await small.setContent(`<body style="margin:0"><img src="data:image/png;base64,${png.toString('base64')}" style="width:${size.width}px;height:${size.height}px;display:block"></body>`);
  await small.screenshot({ path: file, type: 'jpeg', quality: 92 });
  await small.close();
  return report;
}

for (const [lang, locale] of Object.entries(LOCALES)) {
  if (only && lang !== only) continue;
  const dir = path.join(OUT, locale, 'images');
  fs.rmSync(path.join(dir, 'phoneScreenshots'), { recursive: true, force: true });
  const notes = [];
  for (const [i, body] of screenshots(lang).entries()) {
    const r = await save(page(lang, body, { width: W, height: H }), { width: W, height: H }, { width: OUT_W, height: OUT_H },
      path.join(dir, 'phoneScreenshots', `${i + 1}.jpg`));
    notes.push(...r.filter((x) => !x.startsWith('h1 132')).map((x) => `${i + 1}: ${x}`));
  }
  const title = fs.readFileSync(path.join(OUT, locale, 'title.txt'), 'utf8').trim();
  await save(page(lang, featureGraphic(lang, title), { width: 1024, height: 500 }), { width: 1024, height: 500 },
    { width: 1024, height: 500 }, path.join(dir, 'featureGraphic.jpg'));
  console.log(`${lang}: ${locale}${notes.length ? '  ' + notes.join('; ') : ''}`);
}
await browser.close();
