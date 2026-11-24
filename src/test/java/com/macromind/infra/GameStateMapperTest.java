package com.macromind.infra;

import com.macromind.Fixtures;
import com.macromind.domain.model.*;
import com.macromind.domain.service.StatEstimator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class GameStateMapperTest {

    private static final Item THORNMAIL = item("Thornmail", withArmor(70));
    private static final Item INFINITY_EDGE = item("Infinity Edge", NO_STATS);

    private final GameStateMapper mapper = new GameStateMapper(
            new StatEstimator(name -> Optional.of(new ChampionBaseStats(600, 100, 30, 4, 32, 2, 60, 3))),
            List.of(new Item(3075, "Thornmail", 2700, THORNMAIL.stats(), java.util.Set.of()),
                    new Item(3031, "Infinity Edge", 3450, INFINITY_EDGE.stats(), java.util.Set.of())),
            name -> ChampionClass.MARKSMAN);

    private static String fixture() {
        return Fixtures.read(Fixtures.ALL_GAME_DATA);
    }

    @Test
    void readsTheGameModeFromTheMapNumberAndFallsBackToSummonersRift() {
        String aram = fixture().replace("\"mapNumber\": 11", "\"mapNumber\": 12");
        String arena = fixture().replace("\"mapNumber\": 11", "\"mapNumber\": 30");

        assertEquals(GameMode.SUMMONERS_RIFT, mapper.toGameState(fixture()).gameMode());
        assertEquals(GameMode.ARAM, mapper.toGameState(aram).gameMode());
        assertEquals(GameMode.SUMMONERS_RIFT, mapper.toGameState(arena).gameMode());
    }

    @Test
    void mapsFixtureToGameStateWithExactActiveStatsAndEstimatedEnemies() {
        GameState state = mapper.toGameState(fixture());

        assertEquals(912, state.gameTimeInSeconds());
        assertFalse(state.activePlayer().isEstimated());
        assertEquals(90, state.activePlayer().stats().armor());
        assertEquals(1, state.activePlayer().items().size());
        assertEquals(2, state.enemies().size());
        assertTrue(state.enemies().stream().allMatch(Player::isEstimated));
        assertTrue(state.enemies().stream().allMatch(Player::isEnemy));
    }

    @Test
    void estimatesEnemyGoldFromItemPricesAndStatsFromItems() {
        GameState state = mapper.toGameState(fixture());

        Player malphite = state.enemies().stream().filter(p -> p.championName().equals("Malphite")).findFirst().orElseThrow();
        assertEquals(2700, malphite.totalGold());
        assertTrue(malphite.isDead());
        assertTrue(malphite.stats().armor() >= 70);
    }

    @Test
    void mapsActivePlayerKda() {
        GameState state = mapper.toGameState(fixture());

        assertEquals(new KDA(5, 1, 3), state.activePlayer().kda());
    }

    @Test
    void toleratesEmptyAllPlayers() {
        String json = "{\"activePlayer\":{\"championStats\":{},\"riotId\":\"Me#EUW\",\"summonerName\":\"Me\"},\"allPlayers\":[],\"gameData\":{\"gameTime\":1.0,\"gameMode\":\"CLASSIC\"}}";

        GameState state = mapper.toGameState(json);

        assertTrue(state.enemies().isEmpty());
    }

    @Test
    void matchesActivePlayerBySummonerNameWhenRiotIdIsMissingInPlayerList() {
        String json = "{\"activePlayer\":{\"championStats\":{},\"riotId\":\"Me#EUW\",\"summonerName\":\"Me\"},"
                + "\"allPlayers\":[{\"championName\":\"Jinx\",\"team\":\"ORDER\",\"summonerName\":\"Me\",\"items\":[],\"level\":3},"
                + "{\"championName\":\"Soraka\",\"team\":\"CHAOS\",\"summonerName\":\"E1\",\"items\":[],\"level\":3}],"
                + "\"gameData\":{\"gameTime\":1.0}}";

        GameState state = mapper.toGameState(json);

        assertEquals("Jinx", state.activePlayer().championName());
        assertEquals(1, state.enemies().size());
    }

    @Test
    void readsFlatPenetrationAndLethalityFromActivePlayerStats() {
        String json = "{\"activePlayer\":{\"championStats\":{\"armorPenetrationFlat\":10,\"physicalLethality\":15,"
                + "\"magicPenetrationFlat\":12,\"magicLethality\":3},\"summonerName\":\"Me\"},"
                + "\"allPlayers\":[],\"gameData\":{\"gameTime\":1.0}}";

        Player active = mapper.toGameState(json).activePlayer();

        assertEquals(25, active.totalFlatArmorPen());
        assertEquals(15, active.totalFlatMagicPen());
    }

    private static String realFixture() {
        return Fixtures.read(Fixtures.ALL_GAME_DATA_REAL);
    }

    @Test
    void findsTheActivePlayerInARealPayloadWhereOnlyTheActiveNameCarriesATag() {
        GameState state = mapper.toGameState(realFixture());

        assertEquals("Wukong", state.activePlayer().championName());
        assertEquals(4, state.allies().size());
        assertEquals(5, state.enemies().size());
        assertTrue(state.enemies().stream().noneMatch(e -> e.championName().equals("Wukong")));
    }

    @Test
    void readsExactStatsAndGoldForTheActivePlayerFromARealPayload() {
        Player active = mapper.toGameState(realFixture()).activePlayer();

        assertEquals(139, active.stats().armor());
        assertEquals(3623, active.stats().maxHealth());
        assertEquals(18, active.stats().flatArmorPen());
        assertEquals(4183, active.totalGold());
        assertFalse(active.isEstimated());
    }

    @Test
    void estimatesEnemyGoldFromCatalogueTotalsNotFromTheMisleadingPriceField() {
        GameStateMapper priced = new GameStateMapper(
                new StatEstimator(name -> Optional.empty()),
                List.of(new Item(3075, "Thornmail", 2450, THORNMAIL.stats(), java.util.Set.of()),
                        new Item(6631, "Stridebreaker", 3300, THORNMAIL.stats(), java.util.Set.of()),
                        new Item(3009, "Boots of Swiftness", 1000, THORNMAIL.stats(), java.util.Set.of())),
                name -> ChampionClass.BRUISER);

        Player warwick = priced.toGameState(realFixture()).enemies().stream()
                .filter(p -> p.championName().equals("Warwick")).findFirst().orElseThrow();

        assertEquals(2450 + 3300 + 1000, warwick.totalGold());
    }

    @Test
    void marksTheDeadEnemiesFromARealPayload() {
        List<String> dead = mapper.toGameState(realFixture()).enemies().stream()
                .filter(Player::isDead).map(Player::championName).sorted().toList();

        assertEquals(List.of("Cassiopeia", "Zilean"), dead);
    }

    @Test
    void stripsTheTagWhenMatchingNamesButNotWhenDifferentPlayersShareAName() {
        String json = "{\"activePlayer\":{\"championStats\":{},\"summonerName\":\"Sam#EUW\"},"
                + "\"allPlayers\":[{\"championName\":\"Jinx\",\"team\":\"ORDER\",\"summonerName\":\"Sam\",\"items\":[],\"level\":3},"
                + "{\"championName\":\"Ahri\",\"team\":\"CHAOS\",\"summonerName\":\"Samuel\",\"items\":[],\"level\":3}],"
                + "\"gameData\":{\"gameTime\":1.0}}";

        GameState state = mapper.toGameState(json);

        assertEquals("Jinx", state.activePlayer().championName());
        assertEquals(1, state.enemies().size());
    }
}
