import sys
from PIL import Image
out, gap, height = sys.argv[1], 24, int(sys.argv[2])
paths = sys.argv[3:]
ims = []
for p in paths:
    im = Image.open(p).convert("RGB")
    w = int(im.width * height / im.height)
    ims.append(im.resize((w, height), Image.LANCZOS))
W = sum(i.width for i in ims) + gap * (len(ims) + 1)
sheet = Image.new("RGB", (W, height + 2 * gap), (236, 240, 236))
x = gap
for i in ims:
    sheet.paste(i, (x, gap)); x += i.width + gap
sheet.save(out)
print(out, sheet.size)
