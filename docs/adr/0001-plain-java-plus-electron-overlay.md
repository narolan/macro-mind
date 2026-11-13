# 1. A plain Java backend and an Electron overlay

Status: accepted. Date: 2026-11-13.

## Context

MacroMind needs two things: logic that reads the League client and decides what to advise, and a transparent
click-through window that shows the result over the game. The project began as a Spring Boot app.

## Decision

- **Backend: plain Java 25, no Spring.** The work is one poll per second against a local API and a pure rules engine, so
  a framework adds startup time and dependencies for nothing. The JDK's `HttpServer` and `HttpClient` are enough.
- **Frontend: a separate Electron window** that polls the backend's `GET /advice` on loopback.
- **Overlay technology: Electron, not Tauri.** The design preferred Tauri for its smaller footprint. There was no Rust
  toolchain available to build or test it, and Electron runs on plain Node on both macOS (where it is developed) and
  Windows (where League runs).

## Consequences

- The overlay is heavier than a Tauri one would be. Its UI is plain HTML and CSS plus a pure view-model module, so a
  later port to Tauri only replaces `overlay/src/main.js` and `preload.js`.
- Frontend and backend only share the JSON shape of `/advice`, pinned by `AdviceContractTest`.
- Not yet verified: that the overlay draws over League on Windows, or that Vanguard accepts it.
