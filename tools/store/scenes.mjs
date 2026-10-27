// Pieces drawn in HTML for the store screenshots (tools/store/screenshots.mjs) and the promo video: a phone home
// screen around the app's widgets, a booking page to long-press a price on, and a pointing-hand cursor.
// Everything is laid out in dp on a 400 dp wide phone and scaled to the size asked for.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const asset = (name) => 'data:image/jpeg;base64,' + fs.readFileSync(path.join(HERE, 'assets', name)).toString('base64');

export const SCREEN_RATIO = 0.447; // phone screen width / height
export const DP_W = 400, DP_H = Math.round(DP_W / SCREEN_RATIO);

// Where the widgets sit on the home screen, in dp: the video lifts them off as layers at these places.
export const WIDGET_LARGE = { x: 44, y: 126, w: 312, h: 395 };
export const WIDGET_SMALL = { x: 44, y: 536, w: 312, h: 135 };

// App icons: a coloured squircle with a white glyph (24 viewBox).
const GLYPHS = {
  phone: '<path fill="#fff" d="M6.6 10.8a15 15 0 0 0 6.6 6.6l2.2-2.2c.3-.3.7-.4 1-.2 1.1.4 2.3.6 3.6.6.6 0 1 .4 1 1V20c0 .6-.4 1-1 1A17 17 0 0 1 3 4c0-.6.4-1 1-1h3.5c.6 0 1 .4 1 1 0 1.3.2 2.5.6 3.6.1.3 0 .7-.2 1z"/>',
  chat: '<path fill="#fff" d="M5 4h14a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H9l-4 4v-4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z"/>',
  camera: '<path fill="#fff" d="M4 7h3l2-2.5h6L17 7h3a1.5 1.5 0 0 1 1.5 1.5v10A1.5 1.5 0 0 1 20 20H4a1.5 1.5 0 0 1-1.5-1.5v-10A1.5 1.5 0 0 1 4 7z"/><circle cx="12" cy="13.2" r="3.8" fill="#9aa3ad"/><circle cx="12" cy="13.2" r="2.4" fill="#fff"/>',
  globe: '<g fill="none" stroke="#fff" stroke-width="1.8"><circle cx="12" cy="12" r="8.5"/><path d="M3.5 12h17M12 3.5c2.6 2.4 3.8 5.2 3.8 8.5S14.6 18.1 12 20.5M12 3.5C9.4 5.9 8.2 8.7 8.2 12s1.2 6.1 3.8 8.5"/></g>',
  calendar: '<rect x="4" y="5" width="16" height="15" rx="2.5" fill="#fff"/><rect x="4" y="5" width="16" height="4.5" rx="2" fill="#e5484d"/><text x="12" y="17.6" font-family="Rubik, sans-serif" font-size="8" font-weight="700" text-anchor="middle" fill="#333">31</text>',
  photos: '<circle cx="12" cy="7.5" r="3.6" fill="#f4b400"/><circle cx="16.5" cy="12" r="3.6" fill="#0f9d58"/><circle cx="12" cy="16.5" r="3.6" fill="#4285f4"/><circle cx="7.5" cy="12" r="3.6" fill="#db4437"/>',
  map: '<path fill="#fff" d="M12 3a6 6 0 0 1 6 6c0 4.5-6 11-6 11S6 13.5 6 9a6 6 0 0 1 6-6z"/><circle cx="12" cy="9" r="2.3" fill="#2b9b6d"/>',
  mail: '<rect x="3" y="5.5" width="18" height="13" rx="2" fill="#fff"/><path d="M3.8 7l8.2 6 8.2-6" fill="none" stroke="#e5484d" stroke-width="1.8"/>',
  music: '<path fill="#fff" d="M9 17.5V6.8l10-2.3v10.4a2.6 2.6 0 1 1-1.6-2.4V8.1L10.6 9.7v7.8A2.6 2.6 0 1 1 9 15.1z"/>',
  notes: '<rect x="5" y="3.5" width="14" height="17" rx="2" fill="#fff"/><path d="M8 8h8M8 11.5h8M8 15h5" stroke="#f0b429" stroke-width="1.6"/>',
  settings: '<circle cx="12" cy="12" r="7.2" fill="none" stroke="#fff" stroke-width="3" stroke-dasharray="3.2 2.4"/><circle cx="12" cy="12" r="3" fill="#fff"/>',
  weather: '<circle cx="9" cy="9" r="3.8" fill="#ffd54a"/><path fill="#fff" d="M8.5 19a4 4 0 0 1-.4-8 5 5 0 0 1 9.5 1.6A3.2 3.2 0 0 1 17.5 19z"/>',
};
const ICON_BG = { mail: '#ffffff', music: '#ef5466', notes: '#ffffff', settings: '#7b8794',
  phone: '#2bb673', chat: '#3d8bfd', camera: '#5b6470', globe: '#ff7a45', calendar: '#ffffff', photos: '#ffffff', map: '#2b9b6d', weather: '#4aa3df' };

function icon(name, x, y, size = 56) {
  return `<div style="position:absolute;left:${x}px;top:${y}px;width:${size}px;height:${size}px;border-radius:${size * 0.3}px;
    background:${ICON_BG[name]};box-shadow:0 2px 6px rgba(0,0,0,.18);display:flex;align-items:center;justify-content:center">
    <svg viewBox="0 0 24 24" width="${size * 0.62}" height="${size * 0.62}">${GLYPHS[name]}</svg></div>`;
}

/** The status bar: the time on the left, signal, wifi and battery on the right (white, over the wallpaper). */
function statusBar(color = '#fff') {
  return `<div style="position:absolute;left:0;right:0;top:0;height:30px;display:flex;align-items:center;justify-content:space-between;
      padding:0 20px;font:500 14px Rubik, sans-serif;color:${color};direction:ltr">
    <span>9:41</span>
    <svg viewBox="0 0 62 14" width="62" height="14" fill="${color}">
      <rect x="0" y="9" width="3" height="5" rx="1"/><rect x="5" y="6" width="3" height="8" rx="1"/><rect x="10" y="3" width="3" height="11" rx="1"/><rect x="15" y="0" width="3" height="14" rx="1"/>
      <path d="M31 13.5l-2.2-2.6a3.4 3.4 0 0 1 4.4 0zM26.6 8.3a6.8 6.8 0 0 1 8.8 0l-1.4 1.6a4.6 4.6 0 0 0-6 0zM24.4 5.7a10.2 10.2 0 0 1 13.2 0l-1.4 1.6a8 8 0 0 0-10.4 0z"/>
      <rect x="42" y="2" width="17" height="10" rx="2.5" fill="none" stroke="${color}" stroke-width="1.4"/><rect x="44" y="4" width="11" height="6" rx="1"/><rect x="59.6" y="5" width="1.6" height="4" rx=".8"/>
    </svg></div>`;
}

/**
 * The phone's home screen: wallpaper, status bar, the clock, the app's large and small widgets (images, or left out
 * with widgets: false so the video can lift them off as layers), a row of apps and the dock.
 */
export function homeScreen({ width, large, small, widgets = true }) {
  const k = width / DP_W;
  const W = WIDGET_LARGE, S = WIDGET_SMALL;
  const row = ['calendar', 'photos', 'map', 'weather'], dock = ['phone', 'chat', 'globe', 'camera'];
  const xs = [36, 124, 212, 300];
  return `<div style="width:${width}px;height:${width / SCREEN_RATIO}px;overflow:hidden;position:relative">
    <div style="position:absolute;left:0;top:0;width:${DP_W}px;height:${DP_H}px;transform:scale(${k});transform-origin:0 0">
      <div style="position:absolute;inset:0;background:linear-gradient(160deg,#f3d9c4 0%,#c98f7a 55%,#6d4a5c 100%)"></div>
      ${statusBar()}
      ${widgets ? `<img src="${large}" style="position:absolute;left:${W.x}px;top:${W.y}px;width:${W.w}px;height:${W.h}px;filter:drop-shadow(0 6px 14px rgba(0,0,0,.25))">
      <img src="${small}" style="position:absolute;left:${S.x}px;top:${S.y}px;width:${S.w}px;height:${S.h}px;filter:drop-shadow(0 6px 14px rgba(0,0,0,.25))">` : ''}
      ${row.map((n, i) => icon(n, xs[i], 48)).join('')}
      ${['mail', 'music', 'notes', 'settings'].filter((n) => GLYPHS[n]).map((n, i) => icon(n, xs[i], 696)).join('')}
      <div style="position:absolute;left:20px;right:20px;top:782px;height:80px;border-radius:30px;background:rgba(255,255,255,.22)"></div>
      ${dock.map((n, i) => icon(n, xs[i], 794)).join('')}
      <div style="position:absolute;left:140px;top:${DP_H - 14}px;width:120px;height:5px;border-radius:3px;background:rgba(255,255,255,.75)"></div>
    </div></div>`;
}

// System labels of the text-selection menu, in the phone's language (the app's own item comes from its strings).
export const SELECTION = {
  iw: ['העתקה', 'שיתוף'], en: ['Copy', 'Share'], es: ['Copiar', 'Compartir'], ar: ['نسخ', 'مشاركة'], ru: ['Копировать', 'Поделиться'],
  fr: ['Copier', 'Partager'], de: ['Kopieren', 'Teilen'], pt: ['Copiar', 'Compartilhar'], it: ['Copia', 'Condividi'],
  tr: ['Kopyala', 'Paylaş'], zh: ['复制', '分享'], ja: ['コピー', '共有'], hi: ['कॉपी करें', 'शेयर करें'],
};

// The price's box on the page, in dp (measured once from the layout below: the "€189" line under "From").
export const PRICE = { x: 27, y: 606, w: 72, h: 31 };

/**
 * A booking page from a made-up site ("tourvia") with real photos (assets/CREDITS.md), scrolled by `scroll` dp.
 * selected: the price is selected and the selection menu shows, with `menu` = [copy, share, app item].
 */
export function tourPage({ width, height, scroll = 0, selected = false, menu = SELECTION.en.concat('Convert currency'), rtl = false }) {
  const k = width / DP_W;
  const p = PRICE, mx = 20, my = p.y - 64;
  return `<div style="position:relative;width:${width}px;height:${height}px;overflow:hidden;background:#fff">
  <div style="position:absolute;left:0;top:0;width:${DP_W}px;transform:scale(${k}) translateY(${-scroll}px);transform-origin:0 0;background:#fff;
    font-family:Rubik, sans-serif;color:#1a1a1a;direction:ltr;text-align:left;font-weight:500">
    ${statusBar('#1a1a1a')}
    <div style="height:50px"></div>
    <div style="display:flex;align-items:center;gap:8px;padding:6px 14px 10px">
      <div style="font:700 19px Rubik;color:#0b6e4f">tourvia</div>
      <div style="flex:1;height:30px;border-radius:15px;background:#f1f3f4;font:500 11px Rubik;color:#777;line-height:30px;padding-left:12px">Beijing · Tue 12 Nov · 2 adults</div></div>
    <div style="position:relative;height:250px;overflow:hidden"><img src="${asset('palace.jpg')}" style="width:400px;height:267px;object-fit:cover">
      <div style="position:absolute;right:10px;bottom:10px;background:rgba(0,0,0,.6);color:#fff;font:500 11px Rubik;padding:3px 8px;border-radius:10px">1 / 24</div>
      <div style="position:absolute;left:10px;top:10px;background:#fff;font:700 11px Rubik;padding:4px 8px;border-radius:6px;color:#c0392b">Likely to sell out</div></div>
    <div style="display:flex;gap:6px;padding:8px 14px 0">
      <img src="${asset('palace2.jpg')}" style="width:118px;height:78px;object-fit:cover;border-radius:6px">
      <img src="${asset('flower.jpg')}" style="width:118px;height:78px;object-fit:cover;border-radius:6px">
      <img src="${asset('coffee.jpg')}" style="width:118px;height:78px;object-fit:cover;border-radius:6px"></div>
    <div style="padding:12px 14px 0">
      <div style="font:500 11px Rubik;color:#0b6e4f;letter-spacing:.04em">DAY TRIP · 8 HOURS</div>
      <div style="font:700 21px/1.2 Rubik;margin-top:4px">Beijing: Summer Palace, Hutongs &amp; Tea House Day Tour</div>
      <div style="font:500 13px Rubik;margin-top:6px"><span style="color:#f5a623">★★★★★</span> 4.8 <span style="color:#777">(2,314 reviews)</span></div>
      <div style="display:flex;gap:6px;flex-wrap:wrap;margin-top:10px;font:500 11px Rubik">
        <span style="background:#e8f5ee;color:#0b6e4f;padding:4px 8px;border-radius:12px">Free cancellation</span>
        <span style="background:#f1f3f4;padding:4px 8px;border-radius:12px">Skip the line</span>
        <span style="background:#f1f3f4;padding:4px 8px;border-radius:12px">Hotel pickup</span></div>
      <div style="display:flex;align-items:flex-end;justify-content:space-between;margin-top:16px;padding:12px;border:1px solid #e3e3e3;border-radius:12px">
        <div><div style="font:500 11px Rubik;color:#777">From</div>
          <div style="font:700 26px Rubik"><span style="position:relative;padding:0 2px">€189</span></div>
          <div style="font:500 11px Rubik;color:#777">per person</div></div>
        <div style="background:#ff6b2c;color:#fff;font:700 14px Rubik;padding:10px 14px;border-radius:10px">Check availability</div></div>
      <div style="font:700 15px Rubik;margin-top:16px">Highlights</div>
      <div style="font:400 12px/1.6 Rubik;color:#444;margin-top:4px">• Walk the Long Corridor to the Tower of Buddhist Incense<br>
        • Rickshaw ride through the old hutongs<br>• Traditional tea tasting in a courtyard house<br>• Lunch included, small group of 12</div>
      <div style="font:700 15px Rubik;margin-top:14px">What travellers say</div>
      <div style="font:400 12px/1.5 Rubik;color:#444;margin-top:4px">"Our guide Li made the palace come alive. The tea house was the best part."</div>
    </div>
    ${selected ? `<div style="position:absolute;left:${p.x - 3}px;top:${p.y - 3}px;width:${p.w + 6}px;height:${p.h + 6}px;background:rgba(55,150,207,.32)"></div>
    <div style="position:absolute;left:${p.x - 9}px;top:${p.y + p.h + 2}px;width:12px;height:12px;border-radius:0 50% 50% 50%;background:#3796cf"></div>
    <div style="position:absolute;left:${p.x + p.w + 1}px;top:${p.y + p.h + 2}px;width:12px;height:12px;border-radius:50% 0 50% 50%;background:#3796cf"></div>
    <div style="position:absolute;left:${mx}px;top:${my}px;display:flex;gap:16px;align-items:center;background:#fff;border-radius:12px;
      padding:10px 16px;box-shadow:0 4px 14px rgba(0,0,0,.22);font:500 14px Rubik;white-space:nowrap;direction:${rtl ? 'rtl' : 'ltr'}">
      <span>${menu[0]}</span><span>${menu[1]}</span><span style="color:#0b6e4f;font-weight:700">${menu[2]}</span>
      <span style="color:#777">⋮</span></div>` : ''}
  </div></div>`;
}

/** Where the selection menu's app item is, in page dp, for a cursor to tap (approximate centre). */
export function menuItemDp(menu, rtl) {
  const p = PRICE;
  // RTL: the app item is the third from the right, i.e. near the left end; LTR: third from the left.
  return { x: rtl ? 20 + 52 : 20 + 16 + (menu[0].length + menu[1].length) * 8 + 32 + menu[2].length * 4, y: p.y - 64 + 19 };
}

/** A pointing hand (Material "touch_app" outline), white with an ink edge; its fingertip is at (0.39, 0.12) of its box. */
export function hand(size, ink = '#1F2C35') {
  return `<svg viewBox="0 0 24 24" width="${size}" height="${size}" style="overflow:visible;filter:drop-shadow(4px 4px 0 ${ink})">
    <path fill="#fff" stroke="${ink}" stroke-width="1.3" stroke-linejoin="round"
      d="M9.4 2.6c.9 0 1.6.7 1.6 1.6v7.1l1-.1c.4-1.1 1.8-1.4 2.6-.6.6-.8 2-.8 2.6.1.8-.5 2.2-.1 2.4 1.1l.2 4.4c.1 2.9-1.4 5.3-4.3 6.1H11c-1.3 0-2.4-.6-3.1-1.6l-3.6-5c-.6-.8-.3-1.9.6-2.3.6-.3 1.4-.1 1.9.4l1 1V4.2c0-.9.7-1.6 1.6-1.6z"/>
  </svg>`;
}
