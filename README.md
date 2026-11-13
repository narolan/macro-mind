# MacroMind

A transparent overlay for League of Legends that suggests what to buy next, from rules over the live game state.
Personal-use tool. Not endorsed by Riot Games.

## Run

1. Backend (Java 25): `./gradlew run` (Windows: `gradlew.bat run`). Polls the local game client once a second and serves
   `http://127.0.0.1:8765/advice`. The first start downloads Riot's static item data into `~/.macromind/cache`.
2. Overlay (Node): `cd overlay && npm install && npm start`. Shows a click-through window top-right while there is advice.
3. Play in **borderless or windowed** mode. A transparent window cannot draw over exclusive fullscreen.

No game running? `/advice` returns `{"visible":false,"advice":[]}` and the overlay stays hidden.

## Test

`./gradlew test` (backend, coverage report in `build/reports/jacoco`) and `cd overlay && npm test`.

## Known limits

- Enemy armor, MR and HP are not exposed by the game client; they are estimated (advice is marked "est.").
- The overlay is Electron because no Rust toolchain was available; it has not been tried on Windows or alongside Vanguard.
- Rule thresholds and the champion crowd-control and shield lists are first guesses.
