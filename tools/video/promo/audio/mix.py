"""The promo's sound: sound effects from SFX Kit (CC0, github.com/wyzowl/sfx-kit) placed on the picture's actions, the
split-flap clatter built from its single ticks, and a music bed. Writes assets/audio/sfx.wav, music.wav and their limited sum mix.wav (the one the video plays).

    python3 audio/mix.py <sfx-kit checkout> <music file> <music start s>

Each cue is (moment on screen, sound, dB); a sound starts at moment - its sync point (sounds.json), so its hit lands
on the frame. Times are the root timeline's (build.py)."""
import json, subprocess, sys
import numpy as np

SR, LEN = 48000, 24.5
KIT, MUSIC, MUSIC_AT = sys.argv[1], sys.argv[2], float(sys.argv[3])
SOUNDS = json.load(open(f'{KIT}/sounds.json'))['sounds']
SYNC = {s['file'].split('/')[-1][:-4]: s['sync']['at'] for s in SOUNDS}
FILES = {s['file'].split('/')[-1][:-4]: s['file'] for s in SOUNDS}


def load(path, start=0.0, dur=None):
    cmd = ['ffmpeg', '-v', 'error', '-ss', str(start)] + (['-t', str(dur)] if dur else []) + ['-i', path, '-ac', '2', '-ar', str(SR), '-f', 'f32le', '-']
    return np.frombuffer(subprocess.run(cmd, capture_output=True, check=True).stdout, dtype=np.float32).reshape(-1, 2).copy()


def sound(name):
    return load(f'{KIT}/{FILES[name]}')


out = np.zeros((int(SR * LEN), 2), np.float32)


def put(at, clip, db=0.0, pan=0.0):
    i = int(round(at * SR))
    if i < 0:
        clip, i = clip[-i:], 0
    clip = clip[: len(out) - i] * (10 ** (db / 20))
    clip = clip * np.array([1 - max(0, pan), 1 + min(0, pan)], np.float32)
    out[i:i + len(clip)] += clip


def cue(t, name, db=0.0, pan=0.0):
    put(t - SYNC[name], sound(name), db, pan)


# A split-flap board: every flap that turns is one tick, slightly varied in level, pitch-free and panned by column.
rng = np.random.default_rng(26)
tick, tap = sound('data-tick'), sound('click-tap-soft')


def clatter(t0, t1, rate, db):
    n = int((t1 - t0) * rate)
    for t in np.sort(rng.uniform(t0, t1, n)):
        put(t, tick if rng.random() < 0.7 else tap, db + rng.uniform(-6, 0), rng.uniform(-0.6, 0.6))


# A. intro: the board flips €189 / ₪763.38, the swap button, the phone rises, the 160+ sticker, the zoom out.
clatter(0.0, 0.85, 55, -4)
cue(0.62, 'pop-small-in', -4)
cue(0.95, 'whoosh-medium-soft', -6)
cue(1.65, 'pop-medium-in', -2)
cue(2.79, 'whoosh-fast-medium', -4)
# B. widgets: the widgets lift off, come forward, the sticker, out to the right.
cue(4.05, 'swipe-medium-soft', -8)
cue(6.0, 'whoosh-fast-soft', -6)
cue(6.8, 'pop-small-in', -3)
cue(9.09, 'whoosh-fast-medium', -5)
# C. colour sets: a soft swipe as each set reaches the middle.
for k, t in enumerate([9.05, 10.25, 11.45, 12.65]):
    cue(t, ['swipe-medium-soft', 'swipe-fast-soft', 'swipe-medium-firm', 'swipe-medium-soft'][k], -7)
# D. any app: the phone in, the page scrolls, the long press, the tap on the menu item, the bubble, the sticker.
cue(13.5, 'whoosh-medium-soft', -7)
cue(13.9, 'swipe-slow-soft', -9)
cue(15.2, 'click-tap-soft', 0)
cue(15.9, 'click-tap-firm', 0)
cue(16.35, 'zip-in-soft', -4)
cue(17.15, 'pop-medium-in', -2)
cue(18.2, 'reverse-whoosh-short-soft', -6)
# E. claim: the board of codes, coins pouring up, 160+ lands with the till, the offline block pops.
clatter(18.22, 19.2, 70, -6)
cue(18.25, 'money-coins-pour', -8)
clatter(19.4, 19.95, 40, -3)
cue(19.95, 'money-register', -2)
cue(20.45, 'pop-medium-in', -2)
cue(21.3, 'whoosh-fast-soft', -6)
# F. end card: the icon lands, the name, the call to action.
cue(21.75, 'impact-light-firm', -2)
cue(21.8, 'money-coin-drop', -10)
cue(22.65, 'pop-large-in', -2)

peak = np.abs(out).max()
sfx = out * (0.85 / peak)  # the loudest hit at -1.4 dBFS; the bed sits under it (below)

music = load(MUSIC, MUSIC_AT, LEN)
music = np.pad(music, ((0, max(0, len(out) - len(music))), (0, 0)))[: len(out)]
ramp = np.ones(len(out), np.float32)
fi, fo = int(0.25 * SR), int(1.6 * SR)
ramp[:fi] = np.linspace(0, 1, fi)
ramp[-fo:] = np.linspace(1, 0, fo)
music *= ramp[:, None]


def write(a, path):
    subprocess.run(['ffmpeg', '-v', 'error', '-y', '-f', 'f32le', '-ar', str(SR), '-ac', '2', '-i', '-', path], input=a.astype(np.float32).tobytes(), check=True)


write(sfx, 'assets/audio/sfx.wav')
write(music, 'assets/audio/music-raw.wav')
# The bed at -16.5 LUFS (true peak -2 dB): the hits stand 3-6 dB over it, the mix lands near -15 LUFS (measured).
subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', 'assets/audio/music-raw.wav', '-af', 'loudnorm=I=-16.5:TP=-2:LRA=11',
                '-ar', str(SR), 'assets/audio/music.wav'], check=True)
subprocess.run(['rm', 'assets/audio/music-raw.wav'], check=True)
print('sfx peak', round(float(peak), 3))

# One track for the video: bed + hits through a limiter (true peak about -1.5 dBFS), so the sum never clips.
subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', 'assets/audio/music.wav', '-i', 'assets/audio/sfx.wav', '-filter_complex',
                'amix=inputs=2:normalize=0,alimiter=limit=0.84:attack=2:release=60:level=disabled', '-ar', str(SR),
                'assets/audio/mix.wav'], check=True)
