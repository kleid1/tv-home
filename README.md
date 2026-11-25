# tv-home

[CleanFrame](https://github.com/nthnclrk/CleanFrame)'s Home screen on an Android TV, on a TV cleaned of
tracking and ads with [CleanYourTV](https://github.com/Gathin23/cleanyourtv)'s tested lists and scripts.

CleanFrame was made for Samsung's Frame TVs. Here its own interface (HTML, CSS and JavaScript, unchanged in look)
runs in a small Android app, and one file, `device.js`, answers its Samsung calls with Android ones.

**Built for:** TCL Google TV on the G08 board (C7K, C79K, C6K, P8K series). Other Android TVs need their own
device profile; the app itself is not TCL-specific.

## What you get

- CleanFrame's Home: a photograph, the time and date, and a frosted row of your apps with a glass selection plate.
- Your apps in your order, with their marks. Live TV opens the antenna directly.
- The remote's buttons, mapped your way (for example the TCL button opens Live TV).
- **Headphones**, top right: connect, disconnect or forget them, and *Wait for me to connect*, so the TV stops
  taking your AirPods away from your phone every time it starts.
- Tracking, ads, the shop demo and the apps you don't use, switched off. Nothing is uninstalled.

## Set up a TV

Open this repo in Claude Code (or another coding agent) and paste [`PROMPT.md`](PROMPT.md). It follows
[`CLAUDE.md`](CLAUDE.md): it asks your choices first, then backs up, debloats, installs and verifies, in about
15 minutes.

By hand:

```
cp profiles/people/example.json profiles/people/me.json      # edit: apps, buttons, Live TV, headphones
TV_HOST=<tv ip> tools/backup.sh me
TV_HOST=<tv ip> tools/debloat.sh profiles/people/me.json
./gradlew assembleRelease
TV_HOST=<tv ip> tools/install.sh profiles/people/me.json
TV_HOST=<tv ip> tools/verify.sh --reboot
```

Undo everything: `TV_HOST=<tv ip> tools/restore.sh profiles/people/me.json`.

## Layout

| Path | What it is |
|---|---|
| `app/` | The Android app: `HomeActivity` (CleanFrame in a WebView), `Bridge` (Android answers to CleanFrame), `RemoteKeys` (the remote's buttons), `Guard` (headphones), `Profile` (reads the TV's profile) |
| `web/cleanframe/` | CleanFrame's web interface, pinned at [`docs/CLEANFRAME_UPSTREAM`](docs/CLEANFRAME_UPSTREAM) with the changes in [`docs/CLEANFRAME_PATCHES.md`](docs/CLEANFRAME_PATCHES.md) |
| `profiles/devices/` | Per TV model: remote key codes, Live TV app, what is safe to switch off |
| `profiles/people/` | Per owner: apps, buttons, choices. Only `example.json` is committed |
| `tools/` | backup, debloat, install, verify, restore |

## Credits and licences

- [CleanFrame](https://github.com/nthnclrk/CleanFrame) by Nathan Clark, MIT: the Home interface, fonts, icons
  and wallpapers (their own licences in [`docs/licenses`](docs/licenses)).
- [CleanYourTV](https://github.com/Gathin23/cleanyourtv) by Gathin, MIT: the debloat lists and scripts, and the
  Bluetooth add-on the Headphones panel grew from.
- Service marks belong to their owners and only identify the apps.
