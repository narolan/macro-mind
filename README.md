# MacroMind

A transparent overlay for League of Legends that suggests what to buy next, using rules over the live game state.
A personal-use tool. Not endorsed by Riot Games.

```
 League client (https://127.0.0.1:2999)
        │  once a second
        ▼
 LiveClientPoller ──► GameStateMapper ──► BuildAdvisor ──► AdviceServer (http://127.0.0.1:8765/advice)
                         ▲                (rules + ranking)         │  once a second
              StatEstimator + Riot's               ▼
              static item data (cached)     Electron overlay: a click-through window, top right
```

## Prerequisites

- **JDK 25** (Gradle can also provision it through its toolchain support).
- **Node 22.12 or newer** for the overlay.
- League running in **borderless or windowed** mode: a transparent window cannot draw over exclusive fullscreen.

## Run

```
./gradlew run                              # backend (Windows: gradlew.bat run)
cd overlay && npm install && npm start     # overlay, in a second terminal
```

The first backend start downloads Riot's static item data into `~/.macromind/cache`; later starts work offline.
With no game running, `/advice` answers `{"visible":false,"advice":[],...,"alerts":[],"timers":[]}` and the overlay stays hidden.
While you are alive the payload carries only CRITICAL advice; when you are dead it carries everything (see `docs/live-client-api.md`).

Besides `advice`, the payload carries `alerts` (`[{kind, message, priority}]`, shown only while fresh, about 20 s, or while you are dead) and `timers` (`[{name, secondsLeft}]`, only for Dragon, Baron and Rift Herald 1 to 30 s from spawning, see `ObjectiveSchedule`). The buy-now nudge (`BuyNowRule`) alerts when your current gold covers the top critical or recommended item. `SpikeRule` warns when the most dangerous living enemy hits level 6, 11 or 16 or has completed big items (at most 2 per poll). `DamageMixRule` warns when the enemy team is mostly AD or AP and you own no matching armor or magic resist. New alert rules go in `AlertAdvisor.RULES`; the overlay reads both in `overlay/src/viewModel.js`.

## Configuration

Everything has a working default. Backend settings are system properties, for example
`JAVA_OPTS="-Dmacromind.port=9000" build/install/MacroMind/bin/MacroMind` (set `MACROMIND_URL` to match):

| Setting | Default | Where |
|---|---|---|
| `macromind.port` | `8765` | `AppConfig` |
| `macromind.pollIntervalMillis` | `1000` | `AppConfig` |
| `macromind.liveClientUrl` | `https://127.0.0.1:2999` (loopback only) | `AppConfig` |
| `macromind.cacheDir` | `~/.macromind/cache` | `AppConfig` |
| `MACROMIND_URL` (overlay, env var) | `http://127.0.0.1:8765/advice` | `overlay/src/main.js` |

## Project layout

```
src/main/java/com/macromind/
  domain/model/    immutable records: GameState, Player, Item, BuildAdvice, ...
  domain/service/  the rules: ThreatRules, CompositionRules, CoreItemRules; AdviceRanker; StatEstimator
  infra/           everything that talks to the outside: the game client, Riot's static data, the on-disk cache
  api/             the /advice HTTP endpoint and its JSON shape
  app/             wiring: AppRunner, the poll loop, configuration
overlay/           the Electron window (view model, poller, renderer), plain JavaScript
docs/              the Live Client notes, the architecture decision, and the original plan (archive)
```

## Development

```
./gradlew check            # tests, compiler warnings as errors, and the 80% coverage gate
cd overlay && npm test     # overlay tests (no Electron binary needed)
```

The coverage report is written to `build/reports/jacoco/test/html/index.html`. See [CONTRIBUTING.md](CONTRIBUTING.md) and
[docs/live-client-api.md](docs/live-client-api.md) for what the game client does and does not expose.

## Known limits

- The game does not expose enemy armor, magic resist or health, so they are **estimated**; such advice is marked "est.".
- The overlay has not been tried on Windows or alongside Vanguard.
- Rule thresholds, and the lists of champions with heavy crowd control or shields (`TeamProfile`), are first guesses.
