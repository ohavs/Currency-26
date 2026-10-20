"""Taps the first view whose text or content description starts with argv[1] (via uiautomator).

argv[1] may list alternatives as "A|B"; --long long-presses instead (to select text)."""
import re
import subprocess
import sys

labels = sys.argv[1].split("|")
long_press = "--long" in sys.argv[2:]
subprocess.run(["adb", "shell", "uiautomator", "dump", "/sdcard/ui.xml"], capture_output=True)
xml = subprocess.run(["adb", "shell", "cat", "/sdcard/ui.xml"], capture_output=True, text=True).stdout
for node in re.finditer(r"<node [^>]*>", xml):
    attrs = dict(re.findall(r'(\w[\w-]*)="([^"]*)"', node.group(0)))
    label = next((l for l in labels if attrs.get("text", "").startswith(l) or attrs.get("content-desc", "").startswith(l)), None)
    if label is not None:
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", attrs["bounds"]))
        x, y = str((x1 + x2) // 2), str((y1 + y2) // 2)
        if long_press:
            subprocess.run(["adb", "shell", "input", "swipe", x, y, x, y, "900"])
        else:
            subprocess.run(["adb", "shell", "input", "tap", x, y])
        print(f"{'long-pressed' if long_press else 'tapped'} '{label}' at {attrs['bounds']}")
        sys.exit(0)
sys.exit(1)
