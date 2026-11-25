# Shared by the tools: the TV's address, adb on it, and the app's identity.
set -euo pipefail
: "${TV_HOST:?set TV_HOST to the IP address of the TV, e.g. TV_HOST=192.168.1.50}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PKG=org.tvhome
TV="$TV_HOST:5555"
adb connect "$TV" >/dev/null 2>&1 || true
if [ "$(adb -s "$TV" get-state 2>/dev/null)" != "device" ]; then
  echo "Cannot reach the TV at $TV. Is it on, on the same network, with USB debugging on (and this computer allowed)?" >&2
  exit 1
fi
tv() { adb -s "$TV" shell "$@"; }
