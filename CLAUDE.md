# Instructions for Claude (and other coding agents)

You are setting up a TV with this repo: CleanFrame as the Home screen, the TV cleaned of tracking and ads, the
remote's app buttons and headphones handled. Work through the steps in order. Every lesson below cost real time
once; don't rediscover it.

## 0. Ask everything first, in one message

Before touching the TV, ask the owner all of this at once and write the answers into
`profiles/people/<name>.json` (copy `example.json`):

1. **Home apps, in order** (CleanFrame holds up to nine; the first five or six show without scrolling).
2. **Remote buttons:** what the TCL button, the purple button and the app buttons should open.
3. **Live TV:** antenna, cable, or none. Antenna switches off TCL's internet channels so Live TV opens the tuner.
4. **Apps to switch off** that they don't use (`remove`): Alexa, Chromecast, AirPlay, Assistant, unused streaming
   apps. Never guess; list what is installed and let them choose.
5. **Headphones:** should the TV wait to be told before connecting them (`headphones.waitForMe`, default true)?
6. **Clock:** 12 or 24 hours, date beside it or not.
7. **Font:** CleanFrame's Manrope (default) or Google Sans, the font of Google's screensaver clock (`"font": "googlesans"`).

## 1. Connect

- The owner turns on Developer options (press OK on *Build* seven times) and **USB debugging**.
- On Ethernet, USB debugging alone opens port 5555; *Wireless debugging* is not needed and may refuse to turn on.
- `adb connect <ip>:5555`, then the owner accepts *Allow USB debugging?* on the TV (tick *Always allow*).
- Check the firmware matches the device profile (`profiles/devices/*/device.json`, `match`):
  `adb shell getprop ro.build.fingerprint`. A different build may need a new device profile.

## 2. Back up, then debloat

```
TV_HOST=<ip> tools/backup.sh <name>
TV_HOST=<ip> tools/debloat.sh profiles/people/<name>.json
```

Everything is **disabled, not uninstalled**, by CleanYourTV's safety-checked script; `tools/restore.sh` undoes it.

## 3. Install

```
./gradlew assembleRelease            # needs JDK 17 (JAVA_HOME, e.g. /opt/homebrew/opt/openjdk@17); or use the release APK
TV_HOST=<ip> tools/install.sh profiles/people/<name>.json [apk]
TV_HOST=<ip> tools/verify.sh --reboot
```

To change apps or buttons later, edit the person file and run `install.sh` again. No rebuild is needed.

## 4. Test with the owner

- Ask them to press **the real remote's** TCL and purple buttons: adb's simulated keys skip the button service,
  so only real presses prove it works.
- Open Headphones (top right) and check the list.
- Don't judge speed right after a restart: apps start cold and Android recompiles new apps in the background.

## 5. Picture (optional)

The TV's picture settings can't be read over adb. Open them over a playing film with
`adb shell am start -a android.settings.DISPLAY_SETTINGS`, then use single key presses with a screenshot after
each. Batched presses get dropped and land on the wrong row. For HDR: Filmmaker Mode, Brightness 100, Local
Contrast High, Adaptive Brightness off.

## TCL G08 quirks (Google TV 14, UTT2.250416.001 AU02)

| Quirk | What to do |
|---|---|
| `TclAppBoot` blocks apps from starting as a service or on a broadcast | `cmd appops set <pkg> AUTO_START allow; cmd appops write-settings` (install.sh does it) |
| The setup wizard (`setupwraith`) owns the Wi-Fi saved at first setup | On Ethernet, disabling it is safe (debloat.sh uses `FORCE_SETUPWRAITH=1`); on Wi-Fi, forget and rejoin the network first |
| An accessibility service is marked *crashed* after an app update | Restart the TV (`verify.sh --reboot`) |
| The service must ask for key filtering at runtime | Done in `RemoteKeys.onServiceConnected` |
| The 4K panel's density doubles a web page | index.html's viewport is fixed at `width=1920` with wide viewport on |
| The TV reconnects paired headphones at every start | The guard (`Guard.java`) disconnects them unless connected from the Headphones panel |
| Android 14's app freezer can kill a background app (seen with Stremio) | Normal; reopening the app fixes it |
| TCL's own Settings key ignores adb key presses | Open screens with intents instead |
| Google's launcher keeps taking Home even though this app holds the Home role | install.sh switches it off after the new Home is in place (`stock-home.txt`); wait ~15 s before a restart, or Android forgets the switch-off |
| TCL's gear menu (Picture → Motion → Game Direct Sync, i.e. VRR) belongs to Google's launcher, and RemoteKeys takes the gear key | For console settings: `settings put secure enabled_accessibility_services ''` and `pm enable com.google.android.apps.tv.launcherx`, let the owner use the gear menu, then run install.sh again |
| TCL menus hide switch states from `uiautomator` | Read menu names with it, but have the owner check on/off values on screen |
| `adb connect` says *No route to host* while the TV answers ping and port 5555 | The Mac's adb server is stale: `adb kill-server`, then connect again |
| Screenshots come out empty while Live TV plays | The tuner picture is protected; check the input with `dumpsys tv_input` instead |

## Rules

- Back up first. Disable, never uninstall. Keep every change reversible.
- Ask before anything the owner didn't request (forgetting a Wi-Fi network, overriding a safety check).
- Keep personal data out of git: backups and people's profiles are in `.gitignore`.
