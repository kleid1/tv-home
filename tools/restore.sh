#!/usr/bin/env bash
# Undo everything: Google TV's own Home back, every switched-off app back on, this app removed.
# Usage: TV_HOST=<ip> tools/restore.sh profiles/people/<name>.json
source "$(dirname "$0")/common.sh"
PERSON="${1:?usage: tools/restore.sh profiles/people/<name>.json}"
CYT="$ROOT/.cleanyourtv"
DEVICE=$(python3 -c "import json,sys;print(json.load(open(sys.argv[1]))['device'])" "$PERSON")
STOCK=$(python3 -c "import json,sys;print(json.load(open(sys.argv[1]))['stockHome'])" "$ROOT/profiles/devices/$DEVICE/device.json")
tv pm enable "$STOCK" >/dev/null
for list in "$ROOT/profiles/devices/$DEVICE"/debloat*.txt "$ROOT/profiles/people/$(basename "$PERSON" .json).remove.txt"; do
  [ -f "$list" ] && TV_HOST="$TV_HOST" "$CYT/tools/restore.sh" "$list" "$STOCK" | tail -1
done
tv "settings put secure enabled_accessibility_services ''"
adb -s "$TV" uninstall "$PKG" >/dev/null && echo "Removed. The TV is back to Google TV."
