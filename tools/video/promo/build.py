"""Hebrew promo v2 (see SPEC.md): the app in brief, then the widgets, the colour sets, converting from a real-looking
web page, 160+ currencies offline, end card. Writes index.html and compositions/*.html. Run: python3 build.py"""
import os

INK = '#1F2C35'
W, H = 1080, 1920
SAGE, SEA, SAND, NIGHT, SUNSET, GOLD = '#93C9A3', '#3796CF', '#E2AE6E', '#6252C2', '#F27E7C', '#E2AF48'

CSS = """
@font-face { font-family: Secular; src: url('assets/fonts/secular-he.woff2') format('woff2'); unicode-range: U+0590-05FF, U+FB1D-FB4F; }
@font-face { font-family: Secular; src: url('assets/fonts/secular-latin.woff2') format('woff2'); unicode-range: U+0000-00FF, U+2000-206F, U+20AA, U+20AC, U+2190-21FF, U+2605; }
@font-face { font-family: Rubik; font-weight: 700; src: url('assets/fonts/rubik_bold.ttf') format('truetype'); }
@font-face { font-family: Rubik; font-weight: 500; src: url('assets/fonts/rubik_medium.ttf') format('truetype'); }
#root { position: absolute; inset: 0; font-family: Secular, Rubik, sans-serif; color: INK; direction: rtl; overflow: hidden; }
.h { position: absolute; right: 80px; left: 80px; top: 210px; font-size: 128px; line-height: .95; text-align: right; }
.h .block { white-space: nowrap; }
.w { display: inline-block; }
.block { display: inline-block; margin-top: 16px; padding: 0 .14em .08em; border: 8px solid INK; box-shadow: 14px 14px 0 INK; }
.phone { position: absolute; width: 608px; height: 1301px; border: 10px solid INK; border-radius: 56px;
  background: INK; padding: 14px; box-shadow: 24px 24px 0 INK; }
.screen { position: relative; width: 560px; height: 1253px; border-radius: 40px; overflow: hidden; background: #000; }
.screen > img { position: absolute; left: 0; top: 0; width: 560px; height: 1253px; }
.sticker { position: absolute; padding: 8px 26px 14px; border: 7px solid INK; box-shadow: 10px 10px 0 INK;
  white-space: nowrap; font-size: 58px; z-index: 5; transform-origin: 50% 50%; }
bdi { unicode-bidi: isolate; }
#root { -webkit-text-stroke: .035em currentColor; paint-order: stroke fill; }
#pg, #an-menu { -webkit-text-stroke: 0; }
.tier { position: absolute; right: 80px; top: 110px; padding: 6px 22px 10px; background: INK; color: #fff; font-size: 46px;
  border: 5px solid INK; box-shadow: 8px 8px 0 rgba(31,44,53,.35); }
.tier b { color: #F2C14E; font-weight: 400; }
""".replace('INK', INK)


def words(text, cls):
    return ' '.join(f'<span class="w {cls}">{w}</span>' for w in text.split(' '))


def scene(sid, body, script, css=''):
    return f"""<template>
<style>{CSS}{css}</style>
<div id="root" data-composition-id="{sid}" data-width="{W}" data-height="{H}">
{body}
</div>
<script>
const tl = gsap.timeline({{ paused: true }});
{script}
window.__timelines["{sid}"] = tl;
</script>
</template>
"""


TIER = '<div class="tier" id="{0}"><b>★</b> פרימיום</div>'


def tier_in(sel, at):
    return f'tl.fromTo("{sel}", {{ clipPath: "inset(0 0 0 100%)" }}, {{ clipPath: "inset(0 0 0 0%)", duration: 0.3, ease: "power2.inOut" }}, {at});\n'


def blur_in(sel, at, gap=0.08):
    return f'tl.fromTo("{sel}", {{ opacity: 0, filter: "blur(6px)" }}, {{ opacity: 1, filter: "blur(0px)", duration: 0.13, stagger: {gap}, ease: "power2.out" }}, {at});\n'


def grey_in(sel, at, gap=0.09):
    """Per word from a light tint to ink, no position change (a second title entrance)."""
    return f'tl.fromTo("{sel}", {{ opacity: 0.18 }}, {{ opacity: 1, duration: 0.12, stagger: {gap}, ease: "none" }}, {at});\n'


def blur_out(sel, at, dur=0.18):
    return f'tl.to("{sel}", {{ opacity: 0, filter: "blur(8px)", duration: {dur}, ease: "power3.in" }}, {at});\n'


def pop(sel, at, rot=-5):
    return f'tl.fromTo("{sel}", {{ opacity: 0, scale: 0.4, rotation: {rot} }}, {{ opacity: 1, scale: 1, rotation: {rot}, duration: 0.45, ease: "back.out(1.6)" }}, {at});\n'


def flap_board(prefix, rows, cell_w, at, x0, y0, gap=12, flip=0.07, stagger=0.045, bg='#FFFFFF', fg=INK, font='Secular, Rubik', row_gap=None):
    """A brutalist split-flap board. Each cell walks its drum forward (the drum order of the registry's split-flap
    block) until it lands on its character; the glyph is set from timeline time, so any frame renders alone. A cell
    starts at most 12 positions before its target, so a board resolves in about half a second."""
    alphabet = ' 0123456789.+€₪$¥£ABCDEFGHIJKLMNOPQRSTUVWXYZ'
    cells, script = [], []
    ch = round(cell_w * 1.42)
    for r, text in enumerate(rows):
        n = len(text)
        x_start = x0 - (n * cell_w + (n - 1) * gap) / 2
        for i, c in enumerate(text):
            cid = f'{prefix}-{r}-{i}'
            x, y = x_start + i * (cell_w + gap), y0 + r * (ch + (gap * 2 if row_gap is None else row_gap))
            cells.append(f'<div id="{cid}" class="flap" style="left:{x:.0f}px;top:{y:.0f}px;width:{cell_w}px;height:{ch}px;'
                         f'line-height:{ch}px;font-size:{round(cell_w * 0.8)}px;background:{bg};color:{fg};font-family:{font}"> </div>')
            target = alphabet.index(c) if c in alphabet else 0
            first = max(1, target - 12) if target else 0
            cells[-1] = cells[-1].replace('"> </div>', f'">{alphabet[first]}</div>')
            start = at + (r * n + i) * stagger
            if target:
                script.append(f'(function(){{ const el = document.getElementById("{cid}"); const o = {{ p: {first} }};'
                              f' tl.fromTo(o, {{ p: {first} }}, {{ p: {target}, duration: {(target - first) * flip:.3f}, ease: "none", onUpdate: () => {{'
                              f' el.textContent = A[Math.min({target}, Math.floor(o.p + 1e-6))]; }} }}, {start:.3f}); }})();')
    js = f'var A = {list(alphabet)!r};\n' + '\n'.join(script) + '\n'
    return '\n'.join(cells), js


FLAP_CSS = f""".flap {{ position: absolute; text-align: center; border: 6px solid {INK}; box-shadow: 8px 8px 0 {INK}; overflow: hidden;
  direction: ltr; }}
.flap::after {{ content: ""; position: absolute; left: 0; right: 0; top: 50%; height: 4px; margin-top: -2px; background: {INK}; opacity: .85; }}"""

PHONE_X = 236
# The pointing hand of the store screenshots (tools/store/scenes.mjs, hand()), fingertip at (0.39, 0.12).
HAND = f'''<svg viewBox="0 0 24 24" width="130" height="130" style="overflow:visible;filter:drop-shadow(6px 6px 0 {INK})">
  <path fill="#fff" stroke="{INK}" stroke-width="1.3" stroke-linejoin="round"
  d="M9.4 2.6c.9 0 1.6.7 1.6 1.6v7.1l1-.1c.4-1.1 1.8-1.4 2.6-.6.6-.8 2-.8 2.6.1.8-.5 2.2-.1 2.4 1.1l.2 4.4c.1 2.9-1.4 5.3-4.3 6.1H11c-1.3 0-2.4-.6-3.1-1.6l-3.6-5c-.6-.8-.3-1.9.6-2.3.6-.3 1.4-.1 1.9.4l1 1V4.2c0-.9.7-1.6 1.6-1.6z"/></svg>'''
scenes = []

# ---- ground: one colour field that floods between worlds ----
floods = [(2.55, SAND), (9.05, NIGHT), (10.25, GOLD), (11.45, SUNSET), (12.65, SAGE), (17.95, SEA), (21.0, SAGE)]
g = ''.join(f'tl.to("#gf", {{ backgroundColor: "{c}", duration: 0.4, ease: "power2.inOut" }}, {t});\n' for t, c in floods)
scenes.append(('ground', 0, 24.5, 0, scene('ground', f'<div id="gf" style="position:absolute;inset:0;background:{SAGE}"></div>', g)))

# ---- A. intro (0-2.9): the board flips the conversion, the app rises in 3D ----
cells, flap_js = flap_board('in', ['€189', '₪763.38'], 104, 0.0, 540, 210, flip=0.05, stagger=0.03, row_gap=170)
intro_body = f"""
<div id="in-board">{cells}
  <div id="in-swap" style="position:absolute;left:{540 - 60}px;top:{210 + 148 + 85 - 44}px;width:120px;height:88px;border-radius:30px;background:{INK};
    border:6px solid #fff;box-shadow:8px 8px 0 {INK};display:flex;align-items:center;justify-content:center">
    <svg viewBox="0 0 24 24" width="62" height="62"><path fill="#fff" d="M16 17.01V10h-2v7.01h-3L15 21l4-3.99h-3zM9 3L5 6.99h3V14h2V6.99h3L9 3z"/></svg></div></div>
<div id="in-stage" style="position:absolute;inset:0;perspective:1600px;perspective-origin:50% 40%">
  <div class="phone" id="in-phone" style="left:{PHONE_X}px;top:760px;transform-origin:50% 0%">
    <div class="screen"><img src="assets/img/1_main.png"></div></div></div>
<div id="in-st" class="sticker" style="left:80px;top:700px;background:{GOLD}"><bdi dir="ltr">160+</bdi> מטבעות</div>
"""
# Frame 0 already shows the board's first flaps landing (the thumbnail is never empty): the cells are drawn
# with their outline from the start.
intro_js = flap_js + f"""
tl.fromTo("#in-swap", {{ scale: 0, rotation: -180 }}, {{ scale: 1, rotation: 0, duration: 0.45, ease: "back.out(1.8)" }}, 0.2);
tl.fromTo("#in-phone", {{ rotationX: 62, y: 900, opacity: 0 }}, {{ rotationX: 0, y: 0, opacity: 1, duration: 0.8, ease: "expo.out" }}, 0.75);
""" + pop('#in-st', 1.35, -6) + f"""
tl.to("#in-board", {{ y: -500, duration: 0.45, ease: "power3.in" }}, 2.45);
tl.to("#in-phone", {{ scale: 2.4, y: -380, duration: 0.45, ease: "power3.in" }}, 2.45);
tl.to("#in-st", {{ opacity: 0, duration: 0.12 }}, 2.45);
"""
scenes.append(('intro', 0, 2.95, 10, scene('intro', intro_body, intro_js, FLAP_CSS + '#in-phone{transform-style:preserve-3d}')))

# ---- B. widgets (2.6-9.2): the home screen tilts, its widgets lift off as layers, then come forward ----
wid_body = f"""
<div class="h" id="wd-h">{words('הווידג\'ט', 'wdw')}<br><span class="block w wdw" style="background:#FBEBC6">על המסך הראשי.</span></div>
<div id="wd-stage" style="position:absolute;inset:0;perspective:1800px;perspective-origin:50% 55%">
  <div id="wd-rig" style="position:absolute;left:{PHONE_X}px;top:700px;width:608px;height:1301px;transform-style:preserve-3d">
    <div class="phone" style="left:0;top:0"><div class="screen"><img src="assets/img/home_bare.png"></div></div>
    <img id="wd-big" src="assets/img/widget_large.png" style="position:absolute;left:{24 + 44 * 1.4:.0f}px;top:{24 + 126 * 1.4:.0f}px;width:437px;height:553px;
      filter:drop-shadow(14px 14px 0 {INK})">
    <img id="wd-small" src="assets/img/widget_small.png" style="position:absolute;left:{24 + 44 * 1.4:.0f}px;top:{24 + 536 * 1.4:.0f}px;width:437px;height:189px;
      filter:drop-shadow(14px 14px 0 {INK})">
  </div></div>
{TIER.format("wd-tier")}
<div id="wd-st2" class="sticker" style="right:70px;top:1500px;background:#fff">בלי לפתוח את האפליקציה</div>
"""
wid_js = f"""
tl.fromTo("#wd-rig", {{ y: 1300 }}, {{ y: 0, duration: 0.6, ease: "expo.out" }}, 0.0);
""" + blur_in('#wd-h .wdw', 0.35) + f"""
tl.to("#wd-rig", {{ rotationY: -28, rotationX: 14, duration: 0.7, ease: "power2.inOut" }}, 1.0);
tl.to("#wd-big", {{ z: 220, duration: 0.6, ease: "expo.out" }}, 1.45);
tl.to("#wd-small", {{ z: 140, duration: 0.6, ease: "expo.out" }}, 1.6);
""" + tier_in('#wd-tier', 0.3) + f"""
tl.to("#wd-rig", {{ rotationY: 0, rotationX: 0, duration: 0.6, ease: "power2.inOut" }}, 3.1);
tl.to("#wd-big", {{ z: 0, x: -10, y: -6, scale: 1.6, duration: 0.6, ease: "power2.inOut" }}, 3.1);
tl.to("#wd-small", {{ z: 0, x: 40, y: 372, scale: 1.3, duration: 0.6, ease: "power2.inOut" }}, 3.15);
tl.to("#wd-rig .phone", {{ opacity: 0, filter: "blur(10px)", duration: 0.35, ease: "power2.in" }}, 3.1);
""" + pop('#wd-st2', 3.9, -4) + f"""
tl.to(["#wd-big", "#wd-small", "#wd-st2"], {{ x: "+=1300", filter: "blur(18px)", duration: 0.32, ease: "power3.in", stagger: 0.03 }}, 6.25);
""" + blur_out('#wd-h,#wd-tier', 6.25)
scenes.append(('widgets', 2.6, 6.6, 11, scene('widgets', wid_body, wid_js,
                                              '#wd-big,#wd-small{transform-origin:50% 50%} #wd-rig .phone{transform-style:preserve-3d}')))

# ---- C. custom (9.0-13.55): the colour sets glide past on a flat carousel; the ground floods with the one in front ----
# Flat on purpose: a perspective ring stretched the screens. The strip moves from the first frame (no still image
# before the motion), easing off as each set passes the middle: k counts the phones, o = k - a*sin(2*pi*k)/(2*pi)
# keeps it always moving (a < 1) and slowest on whole k. Phone 1 is in the middle at 0.25 s, then one every 1.2 s,
# in step with the ground's floods (9.05 night, 10.25 gold, 11.45 sunset, 12.65 classic).
CAR = ['3_calc.png', '5a_theme.png', '5b_theme.png', '5c_theme.png', '1_main.png', '3_calc.png']
CAR_SP, CAR_T0, CAR_STEP, CAR_DUR = 560, 0.25, 1.2, 4.55
car_html = ''.join(f'<div class="phone car-ph" id="car-{n}" style="left:{540 - 304}px;top:0">'
                   f'<div class="screen"><img src="assets/img/{img}"></div></div>' for n, img in enumerate(CAR))
cus_body = f"""
<div class="h" id="cs-h" style="color:#fff">{words('הצבעים שלך.', 'csw')}<br><span class="block w csw" style="background:#FFE7A6;color:{INK}"><bdi dir="ltr">6</bdi> ערכות צבעים</span></div>
<div id="cs-strip" style="position:absolute;left:0;top:700px;width:1080px;height:1301px">{car_html}</div>
{TIER.format("cs-tier")}
"""
k0, k1 = 1 - CAR_T0 / CAR_STEP, 1 + (CAR_DUR - CAR_T0) / CAR_STEP
cus_js = f"""
const car = [...document.querySelectorAll(".car-ph")], cv = {{ k: {k0:.4f} }};
function placeCar() {{
  const o = cv.k - 0.7 * Math.sin(2 * Math.PI * cv.k) / (2 * Math.PI);
  car.forEach((el, n) => {{
    const d = o - n, a = Math.min(Math.abs(d), 1.6);
    gsap.set(el, {{ x: d * {CAR_SP}, y: a * 70, scale: 0.84 - 0.2 * a, rotation: d * 5, zIndex: 10 - Math.round(a * 4) }});
  }});
}}
placeCar();
tl.fromTo(cv, {{ k: {k0:.4f} }}, {{ k: {k1:.4f}, duration: {CAR_DUR}, ease: "none", onUpdate: placeCar }}, 0);
tl.fromTo("#cs-strip", {{ opacity: 0 }}, {{ opacity: 1, duration: 0.25, ease: "power1.out" }}, 0.0);
""" + grey_in('#cs-h .csw', 0.3) + tier_in('#cs-tier', 0.3) + f"""
tl.to("#cs-h", {{ color: "{INK}", duration: 0.3 }}, 1.2);
tl.to("#cs-strip", {{ opacity: 0, filter: "blur(10px)", duration: 0.3, ease: "power2.in" }}, 4.15);
""" + blur_out('#cs-h,#cs-tier', 4.15)
scenes.append(('custom', 9.0, CAR_DUR, 12, scene('custom', cus_body, cus_js, '.car-ph{transform-origin:50% 40%}')))

# ---- D. any app (13.4-18.2): a tour page; long press on the price; the menu; the real bubble ----
# The page is a fictional booking site drawn at a 400 px phone viewport, scaled 1.4 into the 560 px screen.
page = f"""
<div id="pg" style="position:absolute;left:0;top:0;width:400px;transform:scale(1.4);transform-origin:0 0;background:#fff;
  font-family:Rubik, sans-serif;color:#1a1a1a;direction:ltr;text-align:left">
  <div style="height:50px;background:#fff"></div>
  <div style="display:flex;align-items:center;gap:8px;padding:6px 14px 10px">
    <div style="font:700 19px Rubik;color:#0b6e4f">tourvia</div>
    <div style="flex:1;height:30px;border-radius:15px;background:#f1f3f4;font:500 11px Rubik;color:#777;line-height:30px;padding-left:12px">Beijing · Tue 12 Nov · 2 adults</div></div>
  <div style="position:relative;height:250px;overflow:hidden"><img src="assets/img/palace.jpg" style="width:400px;height:267px;object-fit:cover">
    <div style="position:absolute;right:10px;bottom:10px;background:rgba(0,0,0,.6);color:#fff;font:500 11px Rubik;padding:3px 8px;border-radius:10px">1 / 24</div>
    <div style="position:absolute;left:10px;top:10px;background:#fff;font:700 11px Rubik;padding:4px 8px;border-radius:6px;color:#c0392b">Likely to sell out</div></div>
  <div style="display:flex;gap:6px;padding:8px 14px 0">
    <img src="assets/img/palace2.jpg" style="width:118px;height:78px;object-fit:cover;border-radius:6px">
    <img src="assets/img/flower.jpg" style="width:118px;height:78px;object-fit:cover;border-radius:6px">
    <img src="assets/img/coffee.jpg" style="width:118px;height:78px;object-fit:cover;border-radius:6px"></div>
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
        <div style="font:700 26px Rubik"><span id="pg-price" style="position:relative;padding:0 2px">€189</span></div>
        <div style="font:500 11px Rubik;color:#777">per person</div></div>
      <div style="background:#ff6b2c;color:#fff;font:700 14px Rubik;padding:10px 14px;border-radius:10px">Check availability</div></div>
    <div style="font:700 15px Rubik;margin-top:16px">Highlights</div>
    <div style="font:400 12px/1.6 Rubik;color:#444;margin-top:4px">• Walk the Long Corridor to the Tower of Buddhist Incense<br>
      • Rickshaw ride through the old hutongs<br>• Traditional tea tasting in a courtyard house<br>• Lunch included, small group of 12</div>
    <div style="font:700 15px Rubik;margin-top:14px">What travellers say</div>
    <div style="font:400 12px/1.5 Rubik;color:#444;margin-top:4px">"Our guide Li made the palace come alive. The tea house was the best part."</div>
  </div></div>"""
bub_top = 886 - 20
any_body = f"""
<div class="h" id="an-h">{words('מכל אתר', 'anw')}<br><span class="block w anw" style="background:#E2F1FA">ומכל אפליקציה.</span></div>
<div class="phone" id="an-phone" style="left:{PHONE_X}px;top:650px"><div class="screen" style="background:#fff">{page}
  <div id="an-sel" style="position:absolute;left:@PX@px;top:@PY@px;width:@PW@px;height:@PH@px;background:rgba(55,150,207,.35)"></div>
  <div id="an-menu" style="position:absolute;left:40px;top:@MY@px;display:flex;gap:22px;align-items:center;background:#fff;border-radius:16px;
    padding:16px 24px;box-shadow:0 6px 18px rgba(0,0,0,.25);font:500 30px Rubik;direction:rtl;white-space:nowrap">
    <span>העתקה</span><span>שיתוף</span><span id="an-item" style="color:#0b6e4f;font-weight:700">המרת מטבע</span></div>
  <div id="an-bubble" style="position:absolute;left:0;top:{bub_top}px;width:560px;height:{1253 - bub_top}px;overflow:hidden">
    <img src="assets/img/4_quick.png" style="position:absolute;left:0;top:{-bub_top}px;width:560px;height:1253px"></div>
</div></div>
<div id="an-finger" style="position:absolute;left:@FX@px;top:@FY@px;width:0;height:0;transform-origin:0 0">
  <div style="position:absolute;left:{-0.39 * 130:.0f}px;top:{-0.12 * 130:.0f}px">@HAND@</div></div>
"""
# measure.mjs renders the page alone and writes the price's box (page px) to measure.json; the page then scrolls so
# the price sits at y = 700 on the screen, where the long press happens.
any_js = f"""
const pr = {{ x: 0, y: 0 }};
tl.fromTo("#an-phone", {{ scale: 0.6, opacity: 0 }}, {{ scale: 1, opacity: 1, duration: 0.55, ease: "expo.out" }}, 0.0);
""" + blur_in('#an-h .anw', 0.25) + f"""
tl.fromTo("#pg", {{ y: 0 }}, {{ y: -SCROLL, duration: 0.7, ease: "power2.inOut" }}, 0.5);
tl.set("#an-sel", {{ opacity: 0 }}, 0); tl.set("#an-menu", {{ opacity: 0 }}, 0); tl.set("#an-bubble", {{ y: 520 }}, 0);
tl.fromTo("#an-finger", {{ opacity: 0, scale: 1.3 }}, {{ opacity: 1, scale: 1, duration: 0.25, ease: "expo.out" }}, 1.2);
tl.to("#an-finger", {{ scale: 0.82, duration: 0.35, ease: "power2.inOut" }}, 1.45);
tl.to("#an-sel", {{ opacity: 1, duration: 0.08 }}, 1.8);
tl.fromTo("#an-menu", {{ opacity: 0, scale: 0.85 }}, {{ opacity: 1, scale: 1, duration: 0.18, ease: "power2.out" }}, 1.85);
tl.to("#an-finger", {{ x: MENU_DX, y: MENU_DY, scale: 1, duration: 0.3, ease: "power2.inOut" }}, 2.15);
tl.to("#an-finger", {{ scale: 0.85, duration: 0.1, yoyo: true, repeat: 1 }}, 2.5);
tl.to(["#an-menu", "#an-sel"], {{ opacity: 0, duration: 0.1 }}, 2.68);
tl.to("#an-finger", {{ opacity: 0, duration: 0.15 }}, 2.7);
tl.to("#an-bubble", {{ y: 0, duration: 0.5, ease: "expo.out" }}, 2.72);
tl.to("#an-phone", {{ scale: 1.25, y: -170, duration: 0.6, ease: "power2.inOut" }}, 3.3);
""" + pop('#an-st', 3.45, 5) + f"""
tl.to("#an-phone", {{ clipPath: "circle(0% at 50% 80%)", duration: 0.35, ease: "power3.in" }}, 4.45);
tl.to("#an-st", {{ opacity: 0, duration: 0.12 }}, 4.45);
""" + blur_out('#an-h', 4.4)
any_body += f'\n<div id="an-st" class="sticker" style="left:70px;top:1560px;background:{GOLD}">לחיצה ארוכה, וזהו.</div>'
scenes.append(('anyapp', 13.4, 4.85, 13, (any_body, any_js)))
open('page.html', 'w').write(f'<!doctype html><meta charset="utf-8"><style>{CSS.split("#root")[0]}body{{margin:0}}</style>' + page.replace('transform:scale(1.4);', ''))

# ---- E. claim (18.2-21.35): the whole screen is a departures board of currency codes, rows sliding both ways, with
# gold coins tumbling up behind them; then 160+ and offline land in front. ----
CODES = ('USD EUR GBP JPY ILS THB CHF CAD AUD CNY INR TRY BRL MXN KRW SEK NOK PLN CZK HUF AED SGD HKD ZAR NZD DKK '
         'RON EGP MAD JOD PHP VND IDR MYR ARS CLP COP PEN UAH GEL ISK KES NGN QAR SAR BGN RSD KZT TWD LKR NPR').split()


def rnd(seed):
    """A small seeded generator: the same video on every render."""
    x = [seed]
    def f():
        x[0] = (x[0] * 1103515245 + 12345) % 2147483648
        return x[0] / 2147483648
    return f


R = rnd(26)
CL_ALPHA = ' ABCDEFGHIJKLMNOPQRSTUVWXYZ'
CL_CW, CL_GAP, CL_ROWS, CL_PITCH, CL_Y0 = 78, 8, 8, 236, 40
CL_CH = round(CL_CW * 1.42)
rows_html, rows_js = [], []
for r in range(CL_ROWS):
    codes = []
    while len(codes) < 5:
        c = CODES[int(R() * len(CODES))]
        if c not in codes:
            codes.append(c)
    text = ' '.join(codes)
    cells = []
    for c, ch in enumerate(text):
        x = c * (CL_CW + CL_GAP)
        if ch == ' ':
            continue
        target = CL_ALPHA.index(ch)
        first = max(1, target - 9)
        cid = f'cr{r}-{c}'
        cells.append(f'<div id="{cid}" class="flap" style="left:{x}px;top:0;width:{CL_CW}px;height:{CL_CH}px;line-height:{CL_CH}px;'
                     f'font-size:{round(CL_CW * 0.8)}px;background:#fff;color:{INK};font-family:Secular, Rubik">{CL_ALPHA[first]}</div>')
        start = 0.05 + r * 0.05 + c * 0.018
        rows_js.append(f'(function(){{ const el = document.getElementById("{cid}"); const o = {{ p: {first} }};'
                       f' tl.fromTo(o, {{ p: {first} }}, {{ p: {target}, duration: {(target - first) * 0.045:.3f}, ease: "none", onUpdate: () => {{'
                       f' el.textContent = B[Math.min({target}, Math.floor(o.p + 1e-6))]; }} }}, {start:.3f}); }})();')
    width = len(text) * (CL_CW + CL_GAP)
    left = -int(R() * 260) - 60
    rows_html.append(f'<div class="cl-row" id="cl-row{r}" style="position:absolute;left:{left}px;top:{CL_Y0 + r * CL_PITCH}px;width:{width}px;height:{CL_CH}px">'
                     + ''.join(cells) + '</div>')
    drift = (1 if r % 2 else -1) * (140 + int(R() * 80))
    rows_js.append(f'tl.fromTo("#cl-row{r}", {{ x: {-drift / 2} }}, {{ x: {drift / 2}, duration: 3.15, ease: "none" }}, 0);')

# Coins: a gold disc, an inner ring and a currency sign, tumbling up behind the rows (rotationY flips them).
COIN_SIGNS = ['₪', '$', '€', '£', '¥']
coins_html, coins_js = [], []
# Spread evenly: x by column (shuffled), the start heights from y = 150 down to 3000 so the rise always keeps the
# screen filled.
COIN_N = 18
cols = [(n * 7) % 6 for n in range(COIN_N)]
for n in range(COIN_N):
    size = 110 + int(R() * 100)
    x = int(cols[n] * 180 + R() * 70) - 30
    y = int(150 + n * 2850 / COIN_N + R() * 90)
    sign = COIN_SIGNS[n % len(COIN_SIGNS)]
    coins_html.append(
        f'<div class="coin" id="coin{n}" style="left:{x}px;top:{y}px;width:{size}px;height:{size}px;font-size:{round(size * 0.52)}px;'
        f'line-height:{size - 12}px;border-width:{max(5, size // 22)}px;box-shadow:{size // 14}px {size // 14}px 0 {INK}">'
        f'<div class="coin-in" style="border-width:{max(3, size // 40)}px"></div><bdi>{sign}</bdi></div>')
    rise = 1050 + int(R() * 200)
    spins = 1 + int(R() * 2)
    coins_js.append(f'tl.fromTo("#coin{n}", {{ y: 0, rotation: {int(R() * 40 - 20)} }}, {{ y: {-rise}, rotation: {int(R() * 60 - 30)}, duration: 3.15, ease: "none" }}, 0);')
    coins_js.append(f'tl.fromTo("#coin{n}", {{ rotationY: 0 }}, {{ rotationY: {360 * spins}, duration: 3.15, ease: "none" }}, 0);')

cl_body = f"""
<div id="cl-coins" style="position:absolute;inset:0;perspective:1400px">{''.join(coins_html)}</div>
<div id="cl-rows">{''.join(rows_html)}</div>
<div id="cl-claim" style="position:absolute;left:0;right:0;top:560px;height:900px">
  <div id="cl-num">@NUM@</div>
  <div style="position:absolute;left:0;right:0;top:450px;text-align:center"><span class="block" id="cl-cur" style="font-size:120px;background:#fff">מטבעות.</span></div>
  <div style="position:absolute;left:0;right:0;top:690px;text-align:center"><span class="block" id="cl-off" style="font-size:84px;background:{INK};color:#E3F1E6;box-shadow:14px 14px 0 #fff">{words('גם בלי אינטרנט.', 'clw')}</span></div>
</div>
"""
cells3, flap3 = flap_board('cn', ['160+'], 190, 1.2, 540, 0, flip=0.05, stagger=0.05, bg=INK, fg='#E3F1E6')
cl_body = cl_body.replace('@NUM@', cells3)
cl_js = 'var B = ' + repr(list(CL_ALPHA)) + ';\n' + '\n'.join(coins_js + rows_js) + '\n' + flap3 + f"""
tl.set("#cl-num", {{ opacity: 0 }}, 0);
tl.to("#cl-rows", {{ opacity: 0.32, duration: 0.3, ease: "power2.inOut" }}, 1.05);
tl.set("#cl-num", {{ opacity: 1 }}, 1.18);
tl.fromTo("#cl-cur", {{ clipPath: "inset(0 0 0 100%)" }}, {{ clipPath: "inset(0 0 0 0%)", duration: 0.35, ease: "power2.inOut" }}, 1.6);
tl.fromTo("#cl-off", {{ opacity: 0, scale: 0.7 }}, {{ opacity: 1, scale: 1, duration: 0.35, ease: "back.out(1.6)" }}, 1.95);
""" + blur_out('#cl-claim,#cl-rows,#cl-coins', 2.95)
CL_CSS = FLAP_CSS + f"""
.coin {{ position: absolute; border-radius: 50%; background: #F2C14E; border: 6px solid {INK}; color: {INK}; text-align: center;
  transform-style: preserve-3d; }}
.coin-in {{ position: absolute; inset: 9%; border-radius: 50%; border: 4px solid rgba(31,44,53,.45); }}
#cl-off .w {{ display: inline; }}"""
scenes.append(('claim', 18.2, 3.15, 14, scene('claim', cl_body, cl_js, CL_CSS)))

# ---- F. end card (21.2-24.5) ----
end_body = f"""
<img id="en-icon" src="assets/img/icon.png" style="position:absolute;left:390px;top:520px;width:300px;height:300px;border-radius:68px;
  border:10px solid {INK};box-shadow:20px 20px 0 {INK};transform-origin:50% 50%">
<div id="en-name" style="position:absolute;left:40px;right:40px;top:900px;text-align:center;font-size:92px;line-height:1;white-space:nowrap">{words("ממיר מטבעות + וידג'ט", 'enw')}</div>
<div style="position:absolute;left:0;right:0;top:1080px;text-align:center">
  <span id="en-cta" class="block" style="font-size:76px;background:{INK};color:#E3F1E6;box-shadow:12px 12px 0 #fff">חינם ב-Google Play</span></div>
"""
end_js = f"""
tl.fromTo("#en-icon", {{ y: -900, rotation: -14 }}, {{ y: 0, rotation: 0, duration: 0.55, ease: "back.out(1.2)" }}, 0.05);
""" + grey_in('#en-name .enw', 0.55, 0.1) + f"""
tl.fromTo("#en-cta", {{ opacity: 0, scale: 0.6 }}, {{ opacity: 1, scale: 1, duration: 0.4, ease: "back.out(1.6)" }}, 1.1);
"""
scenes.append(('end', 21.25, 3.25, 15, scene('end', end_body, end_js)))


def write(measure=None):
    os.makedirs('compositions', exist_ok=True)
    hosts = []
    for i, (sid, start, dur, z, html) in enumerate(scenes):
        if isinstance(html, tuple):  # the any-app scene: place the selection, menu and finger on the measured price
            body, js = html
            m = measure or {'x': 26, 'y': 400, 'w': 50, 'h': 30}
            k = 1.4
            scroll = m['y'] * k - 700
            px, py, pw, ph = m['x'] * k - 4, 700 - 4, m['w'] * k + 8, m['h'] * k + 8
            fx, fy = PHONE_X + 24 + px + pw / 2, 650 + 24 + py + ph / 2
            item_cx = 40 + 24 + 85   # the menu's leftmost item ("המרת מטבע") in RTL order
            for key, v in {'@PX@': px, '@PY@': py, '@PW@': pw, '@PH@': ph, '@MY@': py - 92, '@FX@': fx, '@FY@': fy}.items():
                body = body.replace(key, f'{v:.0f}')
            body = body.replace('@HAND@', HAND)
            js = js.replace('SCROLL', f'{scroll:.0f}').replace('MENU_DX', f'{PHONE_X + 24 + item_cx - fx:.0f}').replace(
                'MENU_DY', f'{(650 + 24 + py - 92 + 32) - fy:.0f}')
            html = scene(sid, body, js, '#an-phone{transform-origin:50% 70%}')
        open(f'compositions/{sid}.html', 'w').write(html)
        hosts.append(f'      <div id="{sid}" data-composition-id="{sid}" data-composition-src="compositions/{sid}.html" data-start="{start}"'
                     f' data-duration="{dur}" data-track-index="{i}" data-track-kind="graphics" data-width="{W}" data-height="{H}"'
                     f' style="z-index:{z}"></div>')
    open('index.html', 'w').write(f"""<!doctype html>
<html lang="he" data-resolution="portrait">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width={W}, height={H}" />
    <script src="assets/gsap.min.js"></script>
    <style>
      * {{ margin: 0; padding: 0; box-sizing: border-box; }}
      html, body {{ margin: 0; width: {W}px; height: {H}px; overflow: hidden; background: {SAGE}; }}
      #root {{ position: relative; width: 100%; height: 100%; }}
    </style>
  </head>
  <body>
    <div id="root" data-composition-id="main" data-start="0" data-duration="24.5" data-width="{W}" data-height="{H}">
{chr(10).join(hosts)}
      <audio id="sound" class="clip" src="assets/audio/mix.wav" data-start="0" data-duration="24.5" data-track-index="20" data-volume="1"></audio>
    </div>
    <script>
      window.__timelines["main"] = gsap.timeline({{ paused: true }});
    </script>
  </body>
</html>
""")


if __name__ == '__main__':
    import json, sys
    m = json.load(open('measure.json')) if os.path.exists('measure.json') else None
    write(m)
    print('wrote', len(scenes), 'scenes', 'measured' if m else 'unmeasured')
