# MacroMind Overlay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A local Java backend that polls League's Live Client Data API, estimates enemy stats, runs item-advice rules, and serves JSON to a transparent Tauri overlay on Windows.

**Architecture:** Plain Java 25 process (no Spring) polls `https://127.0.0.1:2999/liveclientdata/allgamedata` about once a second, maps it to `GameState` (enemy stats estimated from Data Dragon), runs `BuildAdvisor`, and serves `GET /advice` through the JDK `HttpServer`. A separate Tauri window polls `/advice` about once a second and shows or hides itself.

**Tech Stack:** Java 25, Gradle, JUnit 5, JDK `HttpClient` and `HttpServer`, Jackson for JSON, Tauri (Windows) for the overlay.

**Spec:** `docs/superpowers/specs/2026-11-13-macromind-overlay-design.md`

## Global Constraints

- Java 25 toolchain (already in `build.gradle`); base package `com.macromind`.
- Domain model stays immutable records; no mutation of existing objects.
- TDD: failing test first, then the minimal code. Target 80% coverage.
- No Spring dependencies in the final build; remove the Spring Boot, dependency-management and GraalVM plugins (the native image is only an escape hatch).
- Live client: `https://127.0.0.1:2999/liveclientdata/*`, self-signed cert, no auth. Skip cert verification **only** for `127.0.0.1`.
- Enemy `armor`, `magicResist`, `maxHealth`, `abilityPower`, `attackDamage` and gold are **not** exposed by the API (to be verified in Task 1). They must be estimated, and the advice flagged `estimated`.
- Overlay targets Windows, in borderless or windowed mode. The backend must also run on macOS against recorded fixtures.
- Never commit `mise.local.toml`. It may hold credentials. Stage files by explicit path.
- Commit style: `<type>: <description>`.

## Review Focus

1. Game not running (connection refused on 2999): the poller returns "no game" and `/advice` returns an empty advice list with `visible=false`. It must not crash or spam errors.
2. Enemy team empty or partial (practice tool, bots, loading screen): the rules treat an empty enemy list as no advice. No `NullPointerException`.
3. Champion missing from Data Dragon (new champion, patch mismatch): the estimator falls back to zero base stats and marks the player `estimated` without throwing.
4. `findFirst().orElse(null)` target enemy: `BuildAdvice.targetEnemy` may be null. The JSON serializer and overlay must tolerate it.
5. The player has already bought the recommended item: no advice for items already owned.

---

### Task 1: Verification spike (manual, Windows or any live/practice game)

**Files:**
- Create: `src/test/resources/fixtures/allgamedata.json` (a recorded real response)
- Create: `docs/superpowers/notes/live-client-fields.md`

**Interfaces:**
- Produces: the recorded fixture used by Tasks 6 and 7, and a written answer on which enemy fields exist.

- [ ] **Step 1:** Start a practice-tool game. Run `curl --insecure https://127.0.0.1:2999/swagger/v3/openapi.json` and `curl --insecure https://127.0.0.1:2999/liveclientdata/allgamedata`.
- [ ] **Step 2:** Save the `allgamedata` response, with the riotId and summoner names anonymised, as the fixture.
- [ ] **Step 3:** In the notes file, list per `allPlayers` entry which stat fields are present. State yes or no for enemy armor, magic resist, max health, AD, AP and gold. If enemies do expose more than assumed, adjust Task 5 before starting it.
- [ ] **Step 4:** Commit: `git add docs/superpowers/notes src/test/resources/fixtures && git commit -m "docs: record live client fields and fixture"`

### Task 2: Drop Spring and fix known rule bugs

**Files:**
- Modify: `build.gradle`, `src/main/resources/application.properties` (delete), `src/main/java/com/macromind/domain/service/BuildAdvisor.java`
- Delete: `MacroMindApplication.java`, `MacroMindApplicationTests.java`
- Test: `src/test/java/com/macromind/domain/service/BuildAdvisorTest.java`

**Interfaces:**
- Consumes: existing `BuildAdvisor(GameState, List<Item>).generateBuildAdvices() -> List<BuildAdvice>`.
- Produces: JUnit 5 test setup, plus test builders `TestData.player(...)`, `TestData.item(...)`, `TestData.gameState(...)` in `src/test/java/com/macromind/TestData.java`.

- [ ] **Step 1:** In `build.gradle` remove the three plugins and the Spring dependencies. Add `testImplementation 'org.junit.jupiter:junit-jupiter:5.13.4'`, `testRuntimeOnly 'org.junit.platform:junit-platform-launcher'`, and the `application` plugin with `mainClass = 'com.macromind.Main'`. Delete the Spring app and context test. Add an empty `Main` with `static void main(String[] args)`.
- [ ] **Step 2: Write failing tests** in `BuildAdvisorTest`:
  - `usesMagicPenetrationNotArmorPenetrationForMagicResistRule()`: the active player has an item with `flatArmorPen=200` and `flatMagicPen=0`. The enemy has `magicResist=200`. Assert a `MAGIC_PENETRATION_PERCENT` item is advised.
  - `apThreatAdviceForTankUsesApMessageAndApTarget()`: an AP-heavy enemy team and a TANK active player. Assert the reasoning contains "AP" and `targetEnemy` equals `gameState.highestApThreat()`. Do the same for BRUISER.
  - `returnsNoAdviceForEmptyEnemyList()`.
- [ ] **Step 3:** Run `./gradlew test`. Expected: the first two FAIL.
- [ ] **Step 4:** Fix `BuildAdvisor`: use `getFlatMagicPen()` (add it to `Player`, summing `item.stats().flatMagicPen()`), and correct the TANK and BRUISER AP branches' message and target.
- [ ] **Step 5:** Run `./gradlew test`. Expected: PASS.
- [ ] **Step 6:** Commit: `git add build.gradle src && git rm` the removed files, then `git commit -m "fix: remove Spring and correct magic-resist and AP-threat rules"`

### Task 3: Collapse duplicated rules and skip owned items

**Files:**
- Modify: `BuildAdvisor.java`; Create: `src/main/java/com/macromind/domain/service/AdviceRule.java`
- Test: `BuildAdvisorTest.java` (existing tests must stay green)

**Interfaces:**
- Produces: `record AdviceRule(Predicate<Item> itemMatcher, String reasoning, AdvicePriority priority, AdviceCategory category)`, and a private `adviceFor(AdviceRule, Player target)` in `BuildAdvisor` that builds the `BuildAdvice` list from `possibleItems`.

- [ ] **Step 1: Write failing test** `doesNotAdviseItemsTheActivePlayerAlreadyOwns()`.
- [ ] **Step 2:** Run it. Expected: FAIL.
- [ ] **Step 3:** Replace the repeated stream blocks with one `adviceFor` helper that also filters out `activePlayer.items()`. Keep the existing public behaviour.
- [ ] **Step 4:** Run `./gradlew test`. Expected: all PASS.
- [ ] **Step 5:** Commit: `refactor: collapse rule duplication and skip owned items`

### Task 4: Confidence flag

**Files:**
- Modify: `BuildAdvice.java`, `Player.java`, `BuildAdvisor.java`
- Test: `BuildAdvisorTest.java`

**Interfaces:**
- Produces: `enum Confidence { EXACT, ESTIMATED }` in `domain/model`; `Player` gains `boolean isEstimated`; `BuildAdvice` gains `Confidence confidence`. It is `ESTIMATED` whenever the triggering rule read an enemy's estimated stats.

- [ ] **Step 1: Write failing test** `marksAdviceEstimatedWhenEnemyStatsAreEstimated()`.
- [ ] **Step 2:** Run it. Expected: FAIL.
- [ ] **Step 3:** Add the field and set it from `targetEnemy.isEstimated()`. Update the `TestData` builders.
- [ ] **Step 4:** Run `./gradlew test`. Expected: PASS.
- [ ] **Step 5:** Commit: `feat: flag advice built on estimated enemy stats`

### Task 5: Data Dragon client and `StatEstimator`

**Files:**
- Create: `infra/DataDragonClient.java`, `domain/service/StatEstimator.java`, `domain/model/ChampionBaseStats.java`
- Test: `StatEstimatorTest.java`, using small fixture JSON in `src/test/resources/fixtures/`

**Interfaces:**
- Produces:
  - `record ChampionBaseStats(double hp, double hpPerLevel, double armor, double armorPerLevel, double magicResist, double magicResistPerLevel, double attackDamage, double attackDamagePerLevel)`.
  - `DataDragonClient.championBaseStats(String championName) -> Optional<ChampionBaseStats>`, `DataDragonClient.items() -> List<Item>`. Fetch the latest version from `https://ddragon.leagueoflegends.com/api/versions.json`, then cache the result on disk per patch.
  - `StatEstimator.estimate(String championName, int level, List<Item> items) -> PlayerStats`, using Riot's growth formula `base + growth * (L-1) * (0.7025 + 0.0175 * (L-1))` plus item stats.

- [ ] **Step 1: Write failing tests:**
  - `estimatesLevelOneArmorAsBasePlusItems()`.
  - `appliesGrowthFormulaAtLevelEighteen()` (assert against a hand-computed value).
  - `fallsBackToZeroBaseStatsForUnknownChampion()` (Review Focus 3).
- [ ] **Step 2:** Run them. Expected: FAIL.
- [ ] **Step 3:** Implement. Accept a `Function<String, Optional<ChampionBaseStats>>` in the constructor so tests don't hit the network.
- [ ] **Step 4:** Run `./gradlew test`. Expected: PASS.
- [ ] **Step 5:** Commit: `feat: estimate enemy stats from Data Dragon base stats and items`

### Task 6: Live client poller and mapper

**Files:**
- Create: `infra/LiveClientPoller.java`, `infra/GameStateMapper.java`
- Test: `GameStateMapperTest.java` (uses the Task 1 fixture), `LiveClientPollerTest.java` (connection refused)

**Interfaces:**
- Consumes: the `StatEstimator` and `DataDragonClient.items()` from Task 5.
- Produces: `LiveClientPoller.poll() -> Optional<GameState>` (empty when no game is running); `GameStateMapper.toGameState(String allGameDataJson) -> GameState`. The active player gets exact stats from `activePlayer.championStats` with `isEstimated=false`. All others come from `StatEstimator` with `isEstimated=true`. Enemy gold is estimated as the sum of their item prices.

- [ ] **Step 1: Write failing tests:**
  - `mapsFixtureToGameStateWithExactActiveStatsAndEstimatedEnemies()`.
  - `pollReturnsEmptyWhenNoGameIsRunning()`.
- [ ] **Step 2:** Run them. Expected: FAIL.
- [ ] **Step 3:** Implement with the JDK `HttpClient`. Build an `SSLContext` that trusts all certificates, and use it only for the `127.0.0.1` host. Parse with Jackson.
- [ ] **Step 4:** Run `./gradlew test`. Expected: PASS.
- [ ] **Step 5:** Commit: `feat: poll live client API and map to GameState`

### Task 7: `/advice` endpoint and visibility rule

**Files:**
- Create: `api/AdviceServer.java`, `domain/service/OverlayVisibility.java`; Modify: `Main.java`
- Test: `OverlayVisibilityTest.java`, `AdviceServerTest.java`

**Interfaces:**
- Produces:
  - `OverlayVisibility.shouldShow(GameState, List<BuildAdvice>, Set<Item> previouslySeenCritical) -> boolean`: true if the active player is dead, or at base, or there is a new CRITICAL advice, and the advice list is not empty. Detect "at base" with the available proxy: no `position` is exposed for the active player, so use `isDead` plus the event feed; document the chosen proxy in the class.
  - `GET /advice` returns `{"visible": boolean, "advice": [{"item": str, "reasoning": str, "priority": str, "category": str, "confidence": str, "target": str|null}]}`.
  - `Main` polls every 1000 ms and serves on `127.0.0.1:8765` (bind to localhost only).

- [ ] **Step 1: Write failing tests:**
  - `showsWhenDeadAndAdviceExists()`.
  - `hidesWhenNoAdvice()`.
  - `showsOnNewCriticalAdviceOnly()`.
  - `adviceEndpointReturnsEmptyHiddenWhenNoGame()` (Review Focus 1).
  - `serializesNullTargetEnemy()` (Review Focus 4).
- [ ] **Step 2:** Run them. Expected: FAIL.
- [ ] **Step 3:** Implement with the JDK `HttpServer`. Keep the latest result in an `AtomicReference`. Replace it with a new immutable value on each poll.
- [ ] **Step 4:** Run `./gradlew test`. Expected: PASS.
- [ ] **Step 5:** Smoke-run `./gradlew run` with no game, then `curl localhost:8765/advice`. Expected: `{"visible":false,"advice":[]}`.
- [ ] **Step 6:** Commit: `feat: serve advice over local HTTP`

### Task 8: Tauri overlay (Windows)

**Files:**
- Create: `overlay/` (Tauri project: `src-tauri/tauri.conf.json`, `src/index.html`, `src/overlay.js`, `src/overlay.css`)

**Interfaces:**
- Consumes: `GET http://127.0.0.1:8765/advice` from Task 7.

- [ ] **Step 1: Spike first.** Create a Tauri window with `transparent: true`, `decorations: false`, `alwaysOnTop: true`, and call `setIgnoreCursorEvents(true)`. On Windows, with League in borderless mode, confirm it is visible over the game, that clicks pass through, and that Vanguard raises no complaint. Record the result in `docs/superpowers/notes/overlay-spike.md`. If it fails, stop and report. Do not build the UI on a broken window.
- [ ] **Step 2:** `overlay.js` polls `/advice` once a second. It hides the window when `visible=false` or the request fails, and otherwise renders up to 3 items, CRITICAL first, each with its reasoning and an "est." badge when `confidence=ESTIMATED`.
- [ ] **Step 3:** Manual check, backend and overlay together in a practice game: advice appears when dead, and disappears when `visible` turns false.
- [ ] **Step 4:** Commit: `feat: add Tauri overlay`
