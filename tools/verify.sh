#!/usr/bin/env bash
# Check the TV is set up right. --reboot restarts it first (needed once after installing or updating).
# Usage: TV_HOST=<ip> tools/verify.sh [--reboot]
source "$(dirname "$0")/common.sh"
if [ "${1:-}" = "--reboot" ]; then
  tv reboot || true; sleep 25
  for _ in $(seq 1 60); do
    adb connect "$TV" >/dev/null 2>&1
    [ "$(adb -s "$TV" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && break
    sleep 5
  done
  [ "$(adb -s "$TV" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] || { echo "The TV did not come back within 5 minutes." >&2; exit 1; }
  sleep 20
fi
ok() { printf '  %-28s %s\n' "$1" "$2"; }
ok "Home" "$(tv cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN | tail -1 | tr -d '\r')"
ok "In front" "$(tv dumpsys activity activities | grep -m1 topResumedActivity | grep -o '[a-z.]*/[A-Za-z.]*' | head -1)"
ok "Remote buttons service" "$(tv dumpsys accessibility | grep -q 'Bound services:{Service\[label=CleanFrame' && echo bound || echo NOT BOUND)"
ok "Crashed services" "$(tv dumpsys accessibility | grep -o 'Crashed services:{[^}]*}' | head -1)"
ok "Auto-start allowed" "$(tv cmd appops get $PKG AUTO_START | cut -d';' -f1 | tr -d '\r')"
ok "Profile on the TV" "$(tv ls /sdcard/Android/data/$PKG/files/tv-profile.json >/dev/null 2>&1 && echo yes || echo MISSING)"
ok "Screensaver" "$(tv settings get secure screensaver_components | tr -d '\r')"
ok "App crashes since boot" "$(adb -s "$TV" logcat -d -b crash | grep -c 'FATAL EXCEPTION')"
tv input keyevent KEYCODE_HOME; sleep 3
tv "dumpsys gfxinfo $PKG reset" >/dev/null
for k in RIGHT RIGHT RIGHT RIGHT LEFT LEFT LEFT LEFT; do tv input keyevent DPAD_$k; sleep 0.6; done
ok "Dropped frames (scrolling)" "$(tv dumpsys gfxinfo $PKG | grep -m1 'Janky frames:' | sed 's/.*: //')"
echo "Remote buttons can only be tested with the real remote: adb's keys skip the button service."
