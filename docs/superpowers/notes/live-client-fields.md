# Live client fields (Task 1)

**Status: NOT verified against a live game.** No League client is available on the dev (macOS) machine.

- `src/test/resources/fixtures/allgamedata.json` is a **synthetic** fixture built from Riot's documented shape.
- Per Riot's docs (read via a summarizer), `allPlayers` entries carry championName, level, items, scores, isDead, team, position, but no armor/MR/max HP/AD/AP/gold. Enemy stats are therefore estimated (Task 5/6).
- TODO on Windows: run `curl --insecure https://127.0.0.1:2999/liveclientdata/allgamedata` in a practice game, replace the fixture with the real response (anonymised), and confirm the enemy fields above.
