#!/usr/bin/env bash
# Install the Home app, make it the TV's Home and send it this person's profile.
# Usage: TV_HOST=<ip> tools/install.sh profiles/people/<name>.json [path/to/app-release.apk]
source "$(dirname "$0")/common.sh"
PERSON="${1:?usage: tools/install.sh profiles/people/<name>.json [apk]}"
APK="${2:-$ROOT/app/build/outputs/apk/release/app-release.apk}"
[ -f "$APK" ] || { echo "No APK at $APK. Download the release APK, or build it: ./gradlew assembleRelease"; exit 1; }
mkdir -p "$ROOT/build"
python3 "$ROOT/tools/make-profile.py" "$PERSON" > "$ROOT/build/tv-profile.json"
SAVER=$(python3 -c "import json,sys;d=json.load(open(sys.argv[1]));print(json.load(open('$ROOT/profiles/devices/'+d['device']+'/device.json')).get('screensaver',''))" "$PERSON")
adb -s "$TV" install -r "$APK" | tail -1
adb -s "$TV" push "$ROOT/build/tv-profile.json" "/sdcard/Android/data/$PKG/files/tv-profile.json" >/dev/null 2>&1 \
  || { tv mkdir -p "/sdcard/Android/data/$PKG/files"; adb -s "$TV" push "$ROOT/build/tv-profile.json" "/sdcard/Android/data/$PKG/files/tv-profile.json" >/dev/null; }
tv pm grant "$PKG" android.permission.BLUETOOTH_CONNECT
# TCL blocks apps from starting on a broadcast or as a service unless allowed (TclAppBoot).
tv "cmd appops set $PKG AUTO_START allow; cmd appops write-settings"
tv "settings put secure enabled_accessibility_services $PKG/$PKG.RemoteKeys; settings put secure accessibility_enabled 1"
tv cmd package set-home-activity "$PKG/.HomeActivity" >/dev/null
[ -n "$SAVER" ] && tv settings put secure screensaver_components "$SAVER"
# Google TV keeps answering Home while its own launcher is on. Switch it off now that this app is installed
# and holds Home; CleanYourTV's disable script keeps its Home guard and an undo record (restore.sh turns it on).
STOCK_LIST="$ROOT/profiles/devices/$(python3 -c "import json,sys;print(json.load(open(sys.argv[1]))['device'])" "$PERSON")/stock-home.txt"
if [ -f "$STOCK_LIST" ] && tv pm list packages -e | tr -d '\r' | grep -qxF "package:$(grep -v '^#' "$STOCK_LIST" | head -1)"; then
  [ -d "$ROOT/.cleanyourtv" ] || { echo "Run tools/debloat.sh first: it sets up CleanYourTV, which switches off the stock Home."; exit 1; }
  TV_HOST="$TV_HOST" "$ROOT/.cleanyourtv/tools/disable.sh" "$STOCK_LIST" | grep -E 'new state|refus|Skipping' || true
  sleep 15   # Android saves package states a few seconds later; a restart before that brings the launcher back
fi
tv input keyevent KEYCODE_HOME
echo "Installed. Home is now $(tv cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN | tail -1 | tr -d '\r')."
echo "After an update Android marks the button service as crashed until a restart: run tools/verify.sh --reboot."
