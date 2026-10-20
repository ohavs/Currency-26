"""Taps the first view whose text or content description starts with argv[1] (via uiautomator)."""
import re
import subprocess
import sys

label = sys.argv[1]
subprocess.run(["adb", "shell", "uiautomator", "dump", "/sdcard/ui.xml"], capture_output=True)
xml = subprocess.run(["adb", "shell", "cat", "/sdcard/ui.xml"], capture_output=True, text=True).stdout
for node in re.finditer(r"<node [^>]*>", xml):
    attrs = dict(re.findall(r'(\w[\w-]*)="([^"]*)"', node.group(0)))
    if attrs.get("text", "").startswith(label) or attrs.get("content-desc", "").startswith(label):
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", attrs["bounds"]))
        subprocess.run(["adb", "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2)])
        print(f"tapped '{label}' at {attrs['bounds']}")
        sys.exit(0)
sys.exit(1)
