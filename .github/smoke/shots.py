"""Prints smoke/*.png shrunk to 300px JPEGs as base64 lines ("SHOT <name> <chunk>"), so the screenshots can be
read back from the job log (artifacts are not reachable from every environment)."""
import base64
import glob
import io
import os

from PIL import Image

for path in sorted(glob.glob("smoke/*.png")):
    name = os.path.splitext(os.path.basename(path))[0]
    image = Image.open(path).convert("RGB")
    image.thumbnail((300, 700))
    buffer = io.BytesIO()
    image.save(buffer, "JPEG", quality=55)
    data = base64.b64encode(buffer.getvalue()).decode()
    for i in range(0, len(data), 900):
        print(f"SHOT {name} {data[i:i + 900]}")
