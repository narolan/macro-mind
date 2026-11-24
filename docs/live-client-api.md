# The League client's Live Client Data API

MacroMind reads one endpoint, `GET https://127.0.0.1:2999/liveclientdata/allgamedata`, served by the game client while a
match is running. It is local only, needs no key, and uses a self-signed certificate (see `LoopbackTlsContext`).
Riot's reference: <https://developer.riotgames.com/docs/lol#game-client-api>.

## Where the sample data comes from

`src/test/resources/fixtures/allgamedata-real.json` is a **real** response, taken from the public mock
[`c4ldas/lol-mock-gamedata`](https://github.com/c4ldas/lol-mock-gamedata) (a Summoner's Rift game, January 2025) and
anonymised. `allgamedata.json` next to it is a small **hand-written** one for unit tests.

It is not from the current patch, so the shape may have drifted. To refresh it, run
`curl --insecure https://127.0.0.1:2999/liveclientdata/allgamedata` during a practice game, replace the player names,
and keep the file name.

## What the game does and does not tell us

| Data | Active player | Everyone else |
|---|---|---|
| Champion, level, items, scores, dead or alive | yes | yes |
| Armor, magic resist, max health, AD, AP | **yes** (`activePlayer.championStats`) | **no** |
| Gold | **yes** (`currentGold`) | **no** |
| Penetration, lethality, omnivamp, tenacity | yes | no |

Enemy stats therefore have to be **estimated** (`StatEstimator`: Data Dragon base stats scaled by level, plus the stats
of the items they are visibly carrying). Advice built on estimates is marked `ESTIMATED`.

## Quirks the code handles

- **Names differ between `activePlayer` and `allPlayers`.** The active player is `"Name#TAG"`; the same person in
  `allPlayers` is just `"Name"`. `GameStateMapper` matches on every identity and strips the tag.
- **`items[].price` is the cost of the last purchase step, not the item's total** (Trinity Force reads 183), so enemy
  gold is the sum of catalogue prices, not of this field.
- **`position` is an empty string** in the recorded payload, so roles default to MID.
- **`championName` is the display name** (`Wukong`), Data Dragon keys by id (`MonkeyKing`); both are indexed.
- **No "in base" flag exists.** The overlay treats "the active player is dead" as the buy moment.

## Objective timers

`events.Events[]` carries `DragonKill` (with `DragonType`, `Elder` for the Elder Dragon), `BaronKill` and `HeraldKill`
(`EventTime` in game seconds). `GameStateMapper` keeps only those; a missing `events` key or other events are ignored.
`ObjectiveTracker` shows a countdown only when an objective is 1 to 30 s away, from the most recent kill of each type.

Checked 2026-11-24 on the League wiki (patch V26.12), numbers live in `ObjectiveSchedule`:

| Objective | First spawn | After a kill | Source |
|---|---|---|---|
| Dragon | 5:00 | 5:00 (Elder Dragon: 6:00) | https://wiki.leagueoflegends.com/en-us/Dragon |
| Baron Nashor | 20:00 | 6:00 | https://wiki.leagueoflegends.com/en-us/Baron_Nashor |
| Rift Herald | 15:00 | never respawns (despawns 19:45) | https://wiki.leagueoflegends.com/en-us/Rift_Herald |

The memory numbers "Herald 8:00 with a 6:00 respawn" are outdated and were not used. Not modelled: the Voidgrubs
(`HordeKill`), and whether the pit holds a normal or Elder Dragon before the first kill (the timer says "Dragon").
