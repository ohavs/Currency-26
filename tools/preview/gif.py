"""Frames from renderFrames (out/<name>/NNN.png) -> an animated GIF.

    python3 gif.py out/v23_demo_he_he demo.gif [--crop x,y,w,h] [--width 480] [--ms 40]
"""
import argparse
import glob
from PIL import Image

p = argparse.ArgumentParser()
p.add_argument("frames")
p.add_argument("out")
p.add_argument("--crop", help="x,y,w,h in pixels of the frames")
p.add_argument("--width", type=int, default=480)
p.add_argument("--ms", type=int, default=40)
a = p.parse_args()

frames = []
for path in sorted(glob.glob(f"{a.frames}/*.png")):
    im = Image.open(path).convert("RGB")
    if a.crop:
        x, y, w, h = map(int, a.crop.split(","))
        im = im.crop((x, y, x + w, y + h))
    im = im.resize((a.width, round(im.height * a.width / im.width)), Image.LANCZOS)
    frames.append(im.quantize(colors=128, method=Image.MEDIANCUT, dither=Image.NONE))
frames[0].save(a.out, save_all=True, append_images=frames[1:], duration=a.ms, loop=0, optimize=True, disposal=1)
print(a.out, len(frames), "frames", frames[0].size)
