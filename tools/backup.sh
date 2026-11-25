#!/usr/bin/env bash
# Save the TV's app list and settings before any change. Usage: TV_HOST=<ip> tools/backup.sh <name>
source "$(dirname "$0")/common.sh"
OUT="$ROOT/.backup-${1:?give the owner a name, e.g. alex}"
mkdir -p "$OUT"
tv pm list packages -f > "$OUT/all-packages.txt"
tv pm list packages -d > "$OUT/already-disabled.txt"
tv cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN > "$OUT/home.txt"
for ns in global secure system; do tv settings list $ns > "$OUT/settings-$ns.txt"; done
tv getprop > "$OUT/getprop.txt"
echo "Backed up to $OUT (kept out of git)."
