# Changes to CleanFrame

`web/cleanframe` is CleanFrame's `app/` folder at commit [`CLEANFRAME_UPSTREAM`](CLEANFRAME_UPSTREAM), with
these changes. The full diff is [`cleanframe.patch`](cleanframe.patch); regenerate it after any change, and use it
to move to a newer CleanFrame.

| File | Change | Why |
|---|---|---|
| `device.js` | Replaced | Answers CleanFrame's device calls through the Android bridge, and exposes the TV's profile (`window.TVProfile`) and the Headphones calls |
| `main.js` | Apps from the profile | `PRIMARY_APPS` reads `window.TVProfile.apps` instead of Samsung app IDs |
| `main.js` | Top-right controls | Inputs, Headphones, Network, All apps, Settings in place of Art Mode, All apps, Settings |
| `main.js` | Headphones panel | A settings detail group like Inputs: *Wait for me to connect*, each pair (connect or disconnect), Forget, Pair headphones |
| `main.js` | Panel focus fix | A panel with no setting axis (Inputs, Headphones) treated every row as the applied choice and opened on its last row |
| `main.js`, `style.css` | No Home preview in Inputs and Headphones | The small preview of Home belongs to the look settings; these panels show the list alone |
| `main.js` | `window.FrameHome()` | The remote's Home button closes any panel (called from `HomeActivity.onNewIntent`) |
| `main.js` | Clock | 12 or 24 hours and the date beside the time, from the profile |
| `main.js` | Wording | "Samsung" settings and the Back sheet point to the TV's settings; icons-only controls by default |
| `main.js`, `assets/` | Google Sans font | Google Sans Flex (OFL), trimmed to Latin as woff2, as a font choice; the profile's `font` sets the default |
| `style.css` | Added | Styles for the date beside the clock |
| `index.html` | Viewport `width=1920`; private fonts link removed | The TV's 4K density otherwise doubles the page; the private fonts file is an empty placeholder |
| `assets/icons/` | Added `digitalb.svg`, `livetv.svg`, `ibplayer.svg` (IBO Player), `klani.svg` and their metrics | Marks for apps CleanFrame doesn't ship |
| `assets/icons/` | Removed marks for apps not offered here (ABC iview, SBS, Apple Music, Apple TV, Spotify, Tubi), the Art Mode, home, apps and settings files, and the old Australian icon licence | Unused on Android TV |
| `config.xml` | Removed | Tizen packaging |

## Toward upstream

The cleanest long-term shape is CleanFrame with a device-adapter interface (Tizen and Android) and its app list,
controls and clock taken from configuration. Then this repo would carry no patch to `main.js` at all.
