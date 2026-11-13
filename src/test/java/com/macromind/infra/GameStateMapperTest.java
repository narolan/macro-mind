package com.macromind.infra;

import com.macromind.domain.model.*;
import com.macromind.domain.service.StatEstimator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static String fixture() throws IOException {
        return Files.readString(Path.of("src/test/resources/fixtures/allgamedata.json"));
    }

    @Test
    void mapsFixtureToGameStateWithExactActiveStatsAndEstimatedEnemies() throws IOException {
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
    void estimatesEnemyGoldFromItemPricesAndStatsFromItems() throws IOException {
        GameState state = mapper.toGameState(fixture());

        Player malphite = state.enemies().stream().filter(p -> p.championName().equals("Malphite")).findFirst().orElseThrow();
        assertEquals(2700, malphite.totalGold());
        assertTrue(malphite.isDead());
        assertTrue(malphite.stats().armor() >= 70);
    }

    @Test
    void mapsActivePlayerKda() throws IOException {
        GameState state = mapper.toGameState(fixture());

        assertEquals(new KDA(5, 1, 3), state.activePlayer().kda());
    }

    @Test
    void toleratesEmptyAllPlayers() {
        String json = "{\"activePlayer\":{\"championStats\":{},\"riotId\":\"Me#EUW\",\"summonerName\":\"Me\"},\"allPlayers\":[],\"gameData\":{\"gameTime\":1.0,\"gameMode\":\"CLASSIC\"}}";

        GameState state = mapper.toGameState(json);

        assertTrue(state.enemies().isEmpty());
    }
}
