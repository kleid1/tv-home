#!/usr/bin/env bash
# Switch off the device profile's lists and the person's own "remove" list, with CleanYourTV's safety-checked
# disable script (backs up state first; tools/restore.sh undoes it). Usage: TV_HOST=<ip> tools/debloat.sh profiles/people/<name>.json
source "$(dirname "$0")/common.sh"
PERSON="${1:?usage: tools/debloat.sh profiles/people/<name>.json}"
CYT="$ROOT/.cleanyourtv"
if [ ! -d "$CYT" ]; then
  git clone -q https://github.com/Gathin23/cleanyourtv "$CYT"
  git -C "$CYT" checkout -q "$(cat "$ROOT/docs/CLEANYOURTV_UPSTREAM")"
  python3 -m venv "$CYT/tools/venv" && "$CYT/tools/venv/bin/pip" install -q -r "$CYT/tools/requirements.txt"
fi
DEVICE=$(python3 -c "import json,sys;print(json.load(open(sys.argv[1]))['device'])" "$PERSON")
DIR="$ROOT/profiles/devices/$DEVICE"
LISTS=$(python3 - "$PERSON" "$DIR" <<'PY'
import json, sys, pathlib
person, d = json.load(open(sys.argv[1])), pathlib.Path(sys.argv[2])
device = json.load(open(d / "device.json"))
lists = [str(d / f) for f in device["debloat"]]
if person.get("liveTv", "antenna") == "antenna" and device.get("antennaOnly"):
    lists.append(str(d / device["antennaOnly"]))
if person.get("remove"):
    own = d.parent.parent / "people" / (pathlib.Path(sys.argv[1]).stem + ".remove.txt")
    own.write_text("# " + pathlib.Path(sys.argv[1]).name + ": apps this owner does not use\n" + "\n".join(person["remove"]) + "\n")
    lists.append(str(own))
print("\n".join(lists))
PY
)
# The setup wizard often owns the Wi-Fi saved at first setup; on Ethernet the guard can be overridden safely.
ETH=$(tv 'ip addr show eth0 2>/dev/null | grep -c "inet "' | tr -d '\r')
while IFS= read -r list; do
  echo "== $(basename "$list")"
  if [ "$ETH" = "1" ]; then FORCE_SETUPWRAITH=1 TV_HOST="$TV_HOST" "$CYT/tools/disable.sh" "$list"
  else TV_HOST="$TV_HOST" "$CYT/tools/disable.sh" "$list"; fi | grep -E 'new state|refus|Skipping' || true
done <<< "$LISTS"
