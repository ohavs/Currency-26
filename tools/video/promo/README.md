# Promo video (13 languages, 9:16, 24.5 s, with sound)

HyperFrames 0.8.155 project. `build.py` writes `index.html` and `compositions/*.html` (generated, not in git).

- App screens in `assets/img` come from the desktop preview (`tools/preview`, `store_iw_*.png`); `home_bare.png`
  from `node home_bare.mjs assets/img/home_bare.png` (the store screenshots' home screen, `tools/store/scenes.mjs`,
  without widgets). The tour page photos and their credits: `tools/store/assets/CREDITS.md`
  (china/flower are CC BY 2.0: credit them in the video description).
- `node measure.mjs` measures the price on the page (measure.json) for the long press.
- Render (no Chrome download, jsdelivr is blocked so GSAP is local):
  `python3 build.py && HYPERFRAMES_SKIP_SKILLS=1 HYPERFRAMES_BROWSER_PATH=$(ls /opt/pw-browsers/chromium_headless_shell-1194/*/headless_shell) npx hyperframes render --quality delivery --output ../../../store/video/promo-he.mp4`

## Languages (v5)

`python3 build.py <lang>` (iw en es ar ru fr de pt it tr zh ja hi; no argument = Hebrew, the original) writes the
project for that language: the words are in `langs.py` (store screenshot texts and the app's strings where they exist),
the app screens come from `tools/preview/out/store_<lang>_*.png` into `assets/img/<lang>` (not in git: first run
`cd tools/preview && ./sync.sh && ONLY=store_ gradle -q run`), the Noto fonts for ar/hi/zh/ja from `tools/store/.fonts`
(fetched by `tools/store/screenshots.mjs`). Left-to-right languages mirror the stickers and the Premium tag; Latin and
Cyrillic are in bold Rubik, the other scripts in bold Noto, as in the store screenshots. Words that would run past
their width shrink: `node measure.mjs <lang>` measures `measure_<lang>.html` (and the price and the menu item) into
`measure/<lang>.json` (in git), then build again. Hebrew keeps its designed sizes (`measure.json`).
Render each language in turn (index.html is shared) to `store/video/promo-<lang>.mp4` (Hebrew: `promo-he.mp4`).

## Sound (v4)

- `audio/mix.py` writes `assets/audio/mix.wav` (the one track the video plays; in git) from two outside sources that
  are not in git: [SFX Kit](https://github.com/wyzowl/sfx-kit) (CC0, real money sounds: coins pouring, a till, a coin
  drop; swipes, taps, whooshes; the split-flap clatter is built from its single ticks, one per turning flap) and the
  music bed. Every cue sits on an on-screen action, placed by the kit's sync points (`sounds.json`).
  `git clone --depth 1 https://github.com/wyzowl/sfx-kit /home/user/snd/sfx-kit`, then
  `python3 audio/mix.py /home/user/snd/sfx-kit <music.mp3> <start s>`.
- v5 (2026-10-27): the owner kept the sound effects and turned the music down; every language is rendered with the
  effects only (`python3 audio/mix.py /home/user/snd/sfx-kit`, no music arguments: about -26 LUFS) until the owner
  finds a track.
- v6 (2026-10-27): the owner's track, "Edge of Motion" by Aetheric from freetouse.com (Upbeat), trimmed by the owner
  to 24 s: `audio/BG.mp3` (in git). `python3 audio/mix.py /home/user/snd/sfx-kit audio/BG.mp3 0` (mix about -16 LUFS,
  peak -1.3 dBFS; the fade-out ends where the track ends). Only the sound changed, so the 13 videos were not rendered
  again: their audio was swapped (`ffmpeg -i promo-<lang>.mp4 -i assets/audio/mix.wav -map 0:v -map 1:a -c:v copy
  -c:a aac -b:a 192k`). Free To Use asks for a credit in the description (check their terms page; the container can't
  reach freetouse.com).
- Music (v4 draft, rejected by the owner): "Daybreak" by Tanner Helland from 10 s,
  [github.com/tannerhelland/free-music](https://github.com/tannerhelland/free-music), CC BY 4.0: the video's
  description must say "Music: Daybreak by Tanner Helland (CC BY 4.0)". Candidates sent to the owner: Daybreak, Thugs,
  Clowns, Syntheticity, Honky-Tonk Villain (same repo). The container reaches only GitHub, npm and PyPI (Openverse,
  Freesound, Pixabay, itch.io, OpenGameArt are blocked); a track the owner sends works the same way.
- Levels: bed at -16.5 LUFS, hits up to -1.4 dBFS, the sum through a limiter: about -16 LUFS, true peak -1.5 dB
  (measured with ffmpeg ebur128, not heard).
