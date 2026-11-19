# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

Electron, with plain HTML, CSS and JavaScript (no framework, no build step), working offline. The backend is a separate
Java process; the window only reads its `GET /advice` JSON.

## Users

League of Legends players on Windows. They open the app window rarely and briefly: on first launch to see that it works,
and later when the on-screen overlay is not showing and they want to know why, or to change a setting. Most of the time
the app runs in the system tray and the click-through overlay does the work.

The user wants it to feel like a real companion app in the vein of op.gg or Blitz: something with a window you can
interact with and tweak, not a bare background process. Simple is fine for now.

## Product Purpose

MacroMind suggests what to buy next in a League of Legends match, from rules over the live game state, and shows it on a
transparent overlay. The app window exists so a player can trust it: it proves the app is alive, says what it is doing in
plain words, explains what it can and cannot know, and offers a few settings. Success is a player who never has to guess
whether it is working.

## Positioning

Advice that comes from rules the player can read ("enemy team has heavy healing, so buy Grievous Wounds"), always with the
reason, and honest about which enemy numbers are estimated. It is a small local tool, not a stats website or a build
database.

## Operating Context

The game client exposes a local API during a match. It gives exact stats for the player and none for enemies, so enemy
armor, magic resist and health are estimated; advice that depends on estimates is marked "est.". Riot's static item data
is downloaded once and cached, so the app works offline after the first start. The overlay only draws over League in
borderless or windowed mode.

## Capabilities and Constraints

- App states the window must show: starting, waiting for a game, in game, cannot load item data, something went wrong.
- First-version scope (confirmed): status, a short explanation, and four settings: overlay on/off, screen corner,
  opacity, start with Windows.
- Not in the first version: a game-history or stats page, accounts, anything that needs a server.
- Overlay is click-through and must never take focus from the game.

## Brand Commitments

Name: MacroMind. The window must say plainly that it is unofficial and not endorsed by Riot Games, and that advice marked
"est." rests on estimated enemy stats (confirmed). No other brand assets exist yet; there is no logo.

## Evidence on Hand

- A real recorded game payload (`src/test/resources/fixtures/allgamedata-real.json`) and real item data in the cache.
- No user research, no testimonials, no usage numbers. None may be invented.
- Not yet verified: how the app looks or behaves on Windows, over a real game, or alongside Vanguard.

## Product Principles

- **Say what is happening.** Nothing on screen must never be ambiguous; every state has a plain sentence.
- **Be honest about estimates.** Never present an estimate as a fact.
- **Stay out of the way.** The window is for checking and tweaking; the overlay and the tray do the everyday work.
- **Few settings, each one real.** A setting exists only if it changes behaviour the player can see.

## Accessibility & Inclusion

Status must never rely on colour alone (the user may be colour-blind): every state has words and a distinct shape or
motion. Keyboard-operable settings with visible focus; reduced-motion respected.

<!-- Assumptions recorded without a direct answer: the "rarely opened, one-glance" usage pattern is inferred from how the
tray-based app works; the user said only that it should feel like op.gg or Blitz and be simple for now. -->
