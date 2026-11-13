package com.macromind.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class GameStateThreatTest {

    private static final Player ME = player("Me", ChampionClass.MARKSMAN, false, List.of(), AVERAGE_STATS);

    @Test
    void estimatedMagesCountAsApThreatEvenWithoutApItems() {
        List<Player> mages = java.util.stream.IntStream.range(0, 5)
                .mapToObj(i -> estimated(player("Mage" + i, ChampionClass.MAGE, true, List.of(), stats(30, 30, 100, 0))))
                .toList();

        assertTrue(game(ME, mages).getEnemyAdRatio() < 0.3);
    }

    @Test
    void estimatedMarksmenStillCountAsAdThreat() {
        List<Player> adcs = java.util.stream.IntStream.range(0, 5)
                .mapToObj(i -> estimated(player("Adc" + i, ChampionClass.MARKSMAN, true, List.of(), stats(30, 30, 100, 0))))
                .toList();

        assertTrue(game(ME, adcs).getEnemyAdRatio() > 0.7);
    }

    @Test
    void exactMageStatsAreLeftUntouched() {
        Player exactMage = player("Mage", ChampionClass.MAGE, true, List.of(), stats(30, 30, 100, 0));

        assertTrue(game(ME, List.of(exactMage)).getEnemyAdRatio() > 0.7);
    }
}
