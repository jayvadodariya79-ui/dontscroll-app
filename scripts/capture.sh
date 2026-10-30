#!/usr/bin/env bash
# Runs on the CI emulator: records the Accessibility consent flow, loads sample
# data, and takes store screenshots. Output goes to ./capture.
set -u
PKG=com.jayv.swipestop
ACT=$PKG/com.jayv.dontscroll.MainActivity
OUT=capture
mkdir -p $OUT
log() { echo "== $*"; }

adb root >/dev/null 2>&1; sleep 4; adb wait-for-device
read W H < <(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1 | tr x ' ')
log "screen ${W}x${H}"

# Evening in India, so the day's chart is full.
adb shell settings put global auto_time 0
adb shell settings put global auto_time_zone 0
adb shell cmd alarm set-timezone Asia/Kolkata || adb shell service call alarm 3 s16 Asia/Kolkata
adb shell date "$(TZ=Asia/Kolkata date +%m%d)2140$(TZ=Asia/Kolkata date +%Y).00"
adb shell date

# Clean status bar.
adb shell settings put global sysui_demo_allowed 1
db() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null; }
db enter
db clock -e hhmm 2140
db battery -e level 100 -e plugged false
db network -e wifi show -e level 4
db network -e mobile show -e datatype none -e level 4
db notifications -e visible false

ui() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1; }
tap() {
  for i in $(seq 1 10); do
    ui
    p=$(python3 scripts/ui_find.py /tmp/ui.xml "$1" "$W" "$H")
    if [ -n "$p" ]; then log "tap '$1' at $p"; adb shell input tap $p; sleep 2; return 0; fi
    if [ $i -ge 3 ]; then adb shell input swipe $((W/2)) $((H*7/10)) $((W/2)) $((H*3/10)) 400; fi
    sleep 1
  done
  log "NOT FOUND: $1"; cp /tmp/ui.xml "$OUT/notfound-$(echo "$1" | tr -c 'a-zA-Z0-9' _).xml"
  adb exec-out screencap -p > "$OUT/notfound-$(echo "$1" | tr -c 'a-zA-Z0-9' _).png"
  return 1
}
shot() { sleep 2; adb exec-out screencap -p > "$OUT/$1.png"; log "shot $1"; }
top() { for i in 1 2 3; do adb shell input swipe $((W/2)) $((H*3/10)) $((W/2)) $((H*8/10)) 200; done; sleep 1; }

adb install -r app/build/outputs/apk/debug/app-debug.apk

# ---- 1. Consent video ----
adb shell screenrecord --bit-rate 6000000 --time-limit 170 /sdcard/consent.mp4 &
REC=$!
sleep 2
adb shell am start -n $ACT; sleep 5
tap "Get started"
tap "re:Set \d+ reels a day"
sleep 5                                   # time to read the disclosure
shot onboarding-disclosure
tap "I understand and agree"
tap "Agree and open settings"
sleep 3
ui; cp /tmp/ui.xml $OUT/dbg-settings-1.xml; adb exec-out screencap -p > $OUT/dbg-settings-1.png
tap "SwipeStop reel tracker"
sleep 2
tap "re:Use SwipeStop reel tracker|SwipeStop reel tracker shortcut|Use service" || true
ui; cp /tmp/ui.xml $OUT/dbg-settings-2.xml
tap "Allow"
sleep 3
shot settings-enabled
adb shell am start -n $ACT; sleep 3
shot onboarding-enabled
tap "Continue"
tap "Allow notifications" && tap "Allow"
tap "Continue"
tap "Start"
sleep 4
adb shell pkill -INT screenrecord; wait $REC 2>/dev/null; sleep 3
adb pull /sdcard/consent.mp4 $OUT/consent.mp4
SVC=$PKG/com.jayv.dontscroll.service.ReelService
if ! adb shell settings get secure enabled_accessibility_services | grep -q ReelService; then
  log "consent flow did not enable the service; enabling directly for screenshots"
  adb shell settings put secure enabled_accessibility_services $SVC
  adb shell settings put secure accessibility_enabled 1
fi
adb shell settings get secure enabled_accessibility_services

# ---- 2. Sample data + screenshots ----
adb shell am broadcast -a com.jayv.swipestop.DEMO_SEED -p $PKG
sleep 3
adb shell am start --activity-clear-task -n $ACT; sleep 4
top; shot 01-today
adb shell input swipe $((W/2)) $((H*7/10)) $((W/2)) $((H*3/10)) 600; shot 02-today-more
tap "Insights" && { top; shot 03-insights; adb shell input swipe $((W/2)) $((H*7/10)) $((W/2)) $((H*3/10)) 600; shot 04-insights-more; }
tap "Goals" && { top; shot 05-goals; }
tap "Today"; top
tap "Settings" && { shot 06-settings; tap "Limits, blocking & schedules" && { top; shot 07-blocking; }; }
ls -la $OUT
