# Promo video (Hebrew, 9:16, 24.5 s)

HyperFrames 0.8.155 project. `build.py` writes `index.html` and `compositions/*.html` (generated, not in git).

- App screens in `assets/img` come from the desktop preview (`tools/preview`, `store_iw_*.png`); `home_bare.png`
  from `node home_bare.mjs assets/img/home_bare.png` (the store screenshots' home screen, `tools/store/scenes.mjs`,
  without widgets). The tour page photos and their credits: `tools/store/assets/CREDITS.md`
  (china/flower are CC BY 2.0: credit them in the video description).
- `node measure.mjs` measures the price on the page (measure.json) for the long press.
- Render (no Chrome download, jsdelivr is blocked so GSAP is local):
  `python3 build.py && HYPERFRAMES_SKIP_SKILLS=1 HYPERFRAMES_BROWSER_PATH=$(ls /opt/pw-browsers/chromium_headless_shell-1194/*/headless_shell) npx hyperframes render --quality delivery --output ../../../store/video/promo-he.mp4`
