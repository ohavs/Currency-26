#!/bin/bash
# Smoke test of the Play build (R8, ads, billing, consent) on an emulator: start the app, open Settings and
# the Premium sheet, start a rewarded video, open the quick-convert bubble and pick another currency in it,
# then select the price in Settings' "Convert from any app" and convert it from the selection menu. Fails on
# any crash.
# Screenshots go to smoke/ (and, shrunk, into the job log: see shots.py).
set -u
APK="$1"
PKG=com.ohav.currency
mkdir -p smoke
adb install -r "$APK" || exit 1
adb logcat -c

tap() { python3 .github/smoke/tap.py "$1" || echo "(no '$1' on screen)"; sleep "${2:-3}"; }
shot() { adb exec-out screencap -p > "smoke/$1.png"; }

adb shell am start -W -n $PKG/com.example.MainActivity
sleep 15
shot 1-main
tap "Calculator"
shot 2-calculator
tap "Settings"
shot 3-settings
tap "Premium"
shot 4-premium
tap "24 hours free" 20
shot 5-video
adb shell input keyevent KEYCODE_BACK; sleep 2
adb shell am force-stop $PKG
# One quoted string: adb shell splits its arguments again on the device.
adb shell "am start -W -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT 'Hotel room €120 per night' -n $PKG/com.example.QuickConvertActivity"
sleep 6
shot 6-quick
# The currency it converts to (its name) opens the picker; the first currency in the list replaces it.
tap "Israeli New Shekel|US Dollar"
shot 7-quick-picker
tap "United Arab Emirates Dirham"
shot 8-quick-chosen
adb shell input keyevent KEYCODE_BACK; sleep 2

# Settings -> "Convert from any app": a long press on the sample price opens the system selection menu.
adb shell am start -W -n $PKG/com.example.MainActivity
sleep 6
tap "Settings"
tap "Convert from any app"
shot 9-select-section
python3 .github/smoke/tap.py "€120" --long || echo "(no sample price on screen)"
sleep 3
shot 10-select-menu
python3 .github/smoke/tap.py "Convert currency" || { python3 .github/smoke/tap.py "More options" && sleep 2 && python3 .github/smoke/tap.py "Convert currency"; } || echo "(no 'Convert currency' in the menu)"
sleep 5
shot 11-select-bubble

adb logcat -d > smoke/logcat.txt
if grep -q "FATAL EXCEPTION" smoke/logcat.txt; then
  echo "::error::The app crashed"
  grep -A40 "FATAL EXCEPTION" smoke/logcat.txt | head -120
  exit 1
fi
grep -iE "Ads|UserMessagingPlatform|BillingClient" smoke/logcat.txt | grep -E "$PKG|I/Ads|W/Ads|E/Ads|BillingClient" | head -40
echo "No crash."
