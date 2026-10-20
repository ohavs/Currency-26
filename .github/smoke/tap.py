"""Taps the first view whose text or content description starts with argv[1] (via uiautomator).

argv[1] may list alternatives as "A|B"; --long long-presses instead (to select text)."""
import re
import subprocess
import sys
import time

labels = sys.argv[1].split("|")
long_press = "--long" in sys.argv[2:]


def dump():
    # --windows takes in popups too, like the text-selection toolbar (older versions read it as a file name,
    # which the real one after it replaces).
    subprocess.run(["adb", "shell", "uiautomator", "dump", "--windows", "/sdcard/ui.xml"], capture_output=True)
    return subprocess.run(["adb", "shell", "cat", "/sdcard/ui.xml"], capture_output=True, text=True).stdout


xml = dump()
# Another app's "isn't responding" dialog (the emulator's launcher, now and then) hides everything: wait it out.
if "t responding" in xml:
    wait = re.search(r'<node [^>]*text="Wait"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    if wait:
        x1, y1, x2, y2 = map(int, wait.groups())
        subprocess.run(["adb", "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2)])
        print("dismissed an 'isn't responding' dialog")
        time.sleep(2)
        xml = dump()
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
