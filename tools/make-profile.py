#!/usr/bin/env python3
"""Merge a device profile and a person profile into the one file the TV app reads.

Usage: tools/make-profile.py profiles/people/<name>.json  > build/tv-profile.json
"""
import json
import pathlib
import sys

root = pathlib.Path(__file__).resolve().parent.parent
person = json.loads(pathlib.Path(sys.argv[1]).read_text())
device = json.loads((root / "profiles/devices" / person["device"] / "device.json").read_text())
ids = {a["id"] for a in person["apps"]}
keys = {}
for name, target in person.get("buttons", {}).items():
    if name not in device["keys"]:
        sys.exit(f"unknown remote button '{name}' for {device['id']}: {sorted(device['keys'])}")
    if target != "settings" and target not in ids:
        sys.exit(f"button '{name}' opens '{target}', which is not in apps")
    keys[str(device["keys"][name])] = target
json.dump({
    "apps": person["apps"],
    "keys": keys,
    "liveTv": {"package": device["liveTv"]["package"], "source": person.get("liveTv", "antenna")},
    "clock": person.get("clock", {"hours": 24, "date": True}),
    "headphones": person.get("headphones", {"waitForMe": True}),
}, sys.stdout, indent=2)
print()
