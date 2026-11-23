# Packaging (Windows installer)

Output: one NSIS installer, `MacroMind-Setup.exe`, with the Electron app, the backend jars and a trimmed Java 25 runtime.

## Build

Needs JDK 25 (with `jlink`) and Node 22 on PATH.

```
cd overlay
npm ci
npm run dist:dir   # unpacked app in dist/, works on any OS; proves the staging
npm run dist:win   # installer in dist/ (build this on Windows)
```

`prepare:backend` (run by both) runs `gradlew installDist`, copies the jars to `overlay/build/backend/lib` and builds a jlink runtime in `overlay/build/backend/runtime`. electron-builder copies both to `resources/backend/` (outside the asar).

CI: `.github/workflows/release.yml` builds on `windows-latest` for `v*` tags or manual dispatch, uploads the artifact and attaches it to the GitHub Release.

## Size

Staged: runtime about 86 MB (macOS arm64), jars about 2.3 MB. Electron adds about 250 MB unpacked. Expect an installer of roughly 100 to 130 MB (estimate, not measured on Windows).

## Limits

- Not yet built or tested on Windows.
- The installer is unsigned, so Windows SmartScreen will warn.
- Auto-update (electron-updater) reads GitHub Releases of `narolan/macro-mind` and needs the release to be public. It checks at start and every 4 hours, downloads in the background and installs on quit. The release version comes from `overlay/package.json`, so bump it to match the `v*` tag before tagging. Untested on Windows; unsigned installers may make updates trip SmartScreen.
- No custom icon (none exists); the default Electron icon is used.
