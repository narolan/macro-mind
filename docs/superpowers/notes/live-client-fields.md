# Live client fields (Task 1)

Real response shape recorded from `c4ldas/lol-mock-gamedata` (public mock of an actual Summoner's Rift game, patch of Jan 2025),
anonymised, at `src/test/resources/fixtures/allgamedata-real.json`. Still TODO: record one from the current patch on the target machine.

Confirmed from the real payload:
- Enemies (and allies) in `allPlayers` expose NO armor, MR, max HP, AD, AP or gold: only championName, rawChampionName, level, items, scores, isDead, respawnTimer, team, position, runes, summonerSpells, summonerName.
- `activePlayer.championStats` is complete (armor, magicResist, maxHealth, attackDamage, physicalLethality, armorPenetrationFlat, omnivamp, lifeSteal, tenacity, ...).
- No `riotId` fields; `activePlayer.summonerName` is "Name#TAG" while the same player in `allPlayers` is "Name" (no tag).
- `items[].price` is NOT the item's total price (Trinity Force 183, Black Cleaver 400): do not sum it for gold.
- `position` is an empty string in this payload.
- `championName` is the display name ("Wukong"), `rawChampionName` the id ("game_character_displayname_MonkeyKing").
