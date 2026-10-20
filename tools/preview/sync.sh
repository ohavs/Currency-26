#!/bin/sh
# Copies the app sources the desktop preview can compile (ui/, utils/, the widget sizing and their tests)
# and writes stand-ins for Android's R class. Run it before every build: ./sync.sh && gradle -q run
set -e
DIR=$(cd "$(dirname "$0")" && pwd)
REPO=$(cd "$DIR/../.." && pwd)
SRC="$REPO/app/src/main/java/com/example"
TEST="$REPO/app/src/test/java/com/example"
rm -rf "$DIR/app" && mkdir -p "$DIR/app/com/example/data" "$DIR/app/com/example/widget"
cp -r "$SRC/ui" "$SRC/utils" "$DIR/app/com/example/"
cp "$SRC/widget/WidgetSizing.kt" "$DIR/app/com/example/widget/"
cp "$SRC/data/AutoUpdateSettings.kt" "$SRC/data/AppRelease.kt" "$SRC/data/CurrencySlot.kt" "$DIR/app/com/example/data/"
# Premium rules and the Monetization interface (PremiumStore needs SharedPreferences: the stub has a fake).
mkdir -p "$DIR/app/com/example/premium"
cp "$SRC/premium/Premium.kt" "$SRC/premium/Monetization.kt" "$DIR/app/com/example/premium/"
# The real fonts file loads Android resources; stubs/ has a desktop one.
rm "$DIR/app/com/example/ui/theme/Fonts.kt"
rm -rf "$DIR/apptest" && mkdir -p "$DIR/apptest/widget"
cp -r "$TEST/utils" "$DIR/apptest/"
cp "$TEST/widget/WidgetSizingTest.kt" "$DIR/apptest/widget/"
mkdir -p "$DIR/apptest/premium" && cp "$TEST/premium/"*.kt "$DIR/apptest/premium/" 2>/dev/null || true
# R.string ids from the default strings.xml, R.font, and R.drawable mapped to the files (PNG and XML).
python3 - "$DIR" "$REPO" <<'PY'
import os, re, sys
d, repo = sys.argv[1], sys.argv[2]
res = f"{repo}/app/src/main/res"
names = re.findall(r'<string name="([^"]+)"', open(f"{res}/values/strings.xml", encoding="utf-8").read())
fonts = ["rubik_regular", "rubik_medium", "rubik_semibold", "rubik_bold"]
entries = []
for folder in ("drawable-nodpi", "drawable-xxhdpi", "drawable"):
    if os.path.isdir(f"{res}/{folder}"):
        entries += [(x.rsplit(".", 1)[0], f"{res}/{folder}/{x}") for x in sorted(os.listdir(f"{res}/{folder}"))]
with open(f"{d}/app/com/example/R.kt", "w") as f:
    f.write("package com.example\n\nobject R {\n    object string {\n")
    for i, n in enumerate(names):
        f.write(f"        const val {n} = {0x7f100000 + i}\n")
    f.write("        val names = listOf(" + ", ".join(f'"{n}"' for n in names) + ")\n    }\n")
    f.write("    object font {\n" + "".join(f"        const val {n} = {0x7f080000 + i}\n" for i, n in enumerate(fonts)) + "    }\n")
    f.write("    object drawable {\n" + "".join(f"        const val {n} = {0x7f0a0000 + i}\n" for i, (n, _) in enumerate(entries)))
    f.write("        val files = mapOf(" + ", ".join(f'{0x7f0a0000 + i} to "{path}"' for i, (_, path) in enumerate(entries)) + ")\n    }\n}\n")
PY
