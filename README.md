# PERPETUAL

A permanent Android shell around one frozen 8-bit pixel-art progress bar.
Everything around the bar is **cartridge-driven** and hot-swaps without
rebuilding the app.

## How it works

- The app is a single fullscreen `StageView`. The progress bar is drawn by
  `drawPixelBar()` with **frozen geometry and style** — cartridges may only
  change its label text, fill fraction and accent color.
- On launch, on every resume, and every 60 seconds, the app fetches
  `cartridge.json` from the shared Google Drive folder (`Wren run/perpetual`)
  via a direct download link baked into `BuildConfig.MANIFEST_URL`.
- When the manifest's `version` is newer than the cached one, the app
  downloads the listed PNG assets, swaps them in, and redraws. Works offline
  from the local cache; ships with a bundled fallback cartridge.

## Cartridge format (v1)

`cartridge.json` in the Drive folder:

```json
{
  "cartridge": "wind-waker-items",
  "version": 1,
  "title": "ITEMS",
  "tabs": ["ITEMS", "MAP", "BOTTLES"],
  "activeTab": 0,
  "barLabel": "LOADING",
  "progress": 0.99,
  "backgroundColor": "#d9c895",
  "backgroundImage": "bg.png",
  "accent": "#e33d2e",
  "hint": "CARTRIDGE 01 - WIND WAKER",
  "grid": { "columns": 4, "items": [
    { "icon": "sword.png", "label": "SWORD" }
  ]},
  "files": { "bg.png": "<drive-file-id>", "sword.png": "<drive-file-id>" }
}
```

To ship a new cartridge: bump `version`, upload the new PNGs to the Drive
folder, and **update the same `cartridge.json` file in place** (its file ID
never changes, so the app's baked-in URL keeps working).

## Building

CI (`.github/workflows/build.yml`) builds and signs the release APK on every
push to `main` and publishes a GitHub Release. Signed with the shared
persistent keystore (same identity as the other apps).

Required repo secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`,
`ANDROID_KEY_PASSWORD`, `KEY_ALIAS`.
