# Promo video (Hebrew, 9:16, 24.5 s, with sound)

HyperFrames 0.8.155 project. `build.py` writes `index.html` and `compositions/*.html` (generated, not in git).

- App screens in `assets/img` come from the desktop preview (`tools/preview`, `store_iw_*.png`); `home_bare.png`
  from `node home_bare.mjs assets/img/home_bare.png` (the store screenshots' home screen, `tools/store/scenes.mjs`,
  without widgets). The tour page photos and their credits: `tools/store/assets/CREDITS.md`
  (china/flower are CC BY 2.0: credit them in the video description).
- `node measure.mjs` measures the price on the page (measure.json) for the long press.
- Render (no Chrome download, jsdelivr is blocked so GSAP is local):
  `python3 build.py && HYPERFRAMES_SKIP_SKILLS=1 HYPERFRAMES_BROWSER_PATH=$(ls /opt/pw-browsers/chromium_headless_shell-1194/*/headless_shell) npx hyperframes render --quality delivery --output ../../../store/video/promo-he.mp4`

## Sound (v4)

- `audio/mix.py` writes `assets/audio/mix.wav` (the one track the video plays; in git) from two outside sources that
  are not in git: [SFX Kit](https://github.com/wyzowl/sfx-kit) (CC0, real money sounds: coins pouring, a till, a coin
  drop; swipes, taps, whooshes; the split-flap clatter is built from its single ticks, one per turning flap) and the
  music bed. Every cue sits on an on-screen action, placed by the kit's sync points (`sounds.json`).
  `git clone --depth 1 https://github.com/wyzowl/sfx-kit /home/user/snd/sfx-kit`, then
  `python3 audio/mix.py /home/user/snd/sfx-kit <music.mp3> <start s>`.
- Music (draft, the owner picks): "Daybreak" by Tanner Helland from 10 s,
  [github.com/tannerhelland/free-music](https://github.com/tannerhelland/free-music), CC BY 4.0: the video's
  description must say "Music: Daybreak by Tanner Helland (CC BY 4.0)". Candidates sent to the owner: Daybreak, Thugs,
  Clowns, Syntheticity, Honky-Tonk Villain (same repo). The container reaches only GitHub, npm and PyPI (Openverse,
  Freesound, Pixabay, itch.io, OpenGameArt are blocked); a track the owner sends works the same way.
- Levels: bed at -16.5 LUFS, hits up to -1.4 dBFS, the sum through a limiter: about -16 LUFS, true peak -1.5 dB
  (measured with ffmpeg ebur128, not heard).
