#!/bin/bash
# Smoke test of the Play build (R8, ads, billing, consent) on an emulator: start the app, open Settings and
# the Premium sheet, start a rewarded video, open the quick-convert bubble and pick another currency in it,
# then follow the tour's "show me how" to the price in Settings' "Convert from any app", select it and convert it
# from the selection menu, and add the widget from Settings. Fails on any crash.
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

# The tour's "Convert from any app" card -> "Show me how": Settings opens on the sample price in a spotlight, and a
# long press on it (through the spotlight) opens the system selection menu.
adb shell am start -W -n $PKG/com.example.MainActivity
sleep 6
tap "Settings"
tap "Widget"
# "Show the quick tour" is at the end of the widget section.
adb shell input swipe 540 1700 540 700 400; sleep 2
tap "Show the quick tour"
tap "Next"
shot 9-tour-select
tap "Show me how" 4
shot 10-select-guide
python3 .github/smoke/tap.py "€120" --long || echo "(no sample price on screen)"
sleep 3
shot 11-select-menu
python3 .github/smoke/tap.py "Convert currency" || { python3 .github/smoke/tap.py "More options" && sleep 2 && python3 .github/smoke/tap.py "Convert currency"; } || echo "(no 'Convert currency' in the menu)"
sleep 5
shot 12-select-bubble

# Settings > Widget > "Add widget to home screen": the launcher's pin dialog, then the widget on the home screen.
adb shell input keyevent KEYCODE_BACK; sleep 2
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/com.example.MainActivity
sleep 6
tap "Settings"
tap "Widget"
tap "Add widget to home screen" 4
shot 13-widget-pin
tap "Add to home screen|ADD TO HOME SCREEN|Add automatically|ADD AUTOMATICALLY|Add" 4
adb shell input keyevent KEYCODE_HOME; sleep 10
shot 14-widget-home
adb shell dumpsys appwidget | grep -A3 "com.ohav.currency" | head -20

adb logcat -d > smoke/logcat.txt
echo "--- widget log ---"
grep -iE "glance|appwidget|RemoteViews|widget" smoke/logcat.txt | grep -iE "error|exception|fail|can't|unable|W/|E/" | head -40

# Only the app's own crashes count: uiautomator (the screen reads above) now and then dies on its way out.
if grep -A2 "FATAL EXCEPTION" smoke/logcat.txt | grep -q "Process: $PKG"; then
  echo "::error::The app crashed"
  grep -A40 "FATAL EXCEPTION" smoke/logcat.txt | head -120
  exit 1
fi
if grep -q "FATAL EXCEPTION" smoke/logcat.txt; then
  echo "A crash outside the app (not counted):"
  grep -A4 "FATAL EXCEPTION" smoke/logcat.txt | head -20
fi
grep -iE "Ads|UserMessagingPlatform|BillingClient" smoke/logcat.txt | grep -E "$PKG|I/Ads|W/Ads|E/Ads|BillingClient" | head -40
echo "No crash."
