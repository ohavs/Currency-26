#!/bin/sh
# The pictures launchers show in "Add to Home screen?" and their widget lists: renders the widgets in every app
# language (Render.kt, widgetPreviews) and writes them to res/drawable[-<lang>]-nodpi as WebP. English is the default;
# Hebrew goes to both -iw and -he, as the strings do.
set -e
DIR=$(cd "$(dirname "$0")" && pwd)
RES="$DIR/../../app/src/main/res"
cd "$DIR" && ./sync.sh && ONLY=widget_preview gradle -q run
python3 - "$DIR/out" "$RES" <<'PY'
import os, sys
from PIL import Image
out, res = sys.argv[1], sys.argv[2]
for lang in ["en", "iw", "es", "ar", "ru", "fr", "de", "pt", "it", "tr", "zh", "ja", "hi"]:
    folders = ["drawable-nodpi"] if lang == "en" else [f"drawable-{q}-nodpi" for q in (["iw", "he"] if lang == "iw" else [lang])]
    for size in ["large", "small"]:
        img = Image.open(f"{out}/widget_preview_{lang}_{size}.png").convert("RGBA")
        for folder in folders:
            os.makedirs(f"{res}/{folder}", exist_ok=True)
            img.save(f"{res}/{folder}/widget_preview_{size}.webp", "WEBP", quality=88, method=6)
PY
