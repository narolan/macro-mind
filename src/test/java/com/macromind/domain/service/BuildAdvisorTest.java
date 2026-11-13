package com.macromind.domain.service;

import com.macromind.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class BuildAdvisorTest {

    private static final Item MAGIC_PEN_ITEM = item("Void Staff", NO_STATS, ItemTag.MAGIC_PENETRATION_PERCENT);

    @Test
    void usesMagicPenetrationNotArmorPenetrationForMagicResistRule() {
        Item armorPenItem = item("Last Whisper", withFlatArmorPen(200));
        Player active = player("Ahri", ChampionClass.MAGE, false, List.of(armorPenItem), AVERAGE_STATS);
        Player enemy = player("Tank", ChampionClass.TANK, true, List.of(), stats(80, 200, 100, 100));

        List<BuildAdvice> advices = new BuildAdvisor(game(active, List.of(enemy)), List.of(MAGIC_PEN_ITEM))
                .generateBuildAdvices();

        assertTrue(advices.stream().anyMatch(a -> a.recommendedItem().equals(MAGIC_PEN_ITEM)));
    }

    @Test
    void apThreatAdviceForTankUsesApMessageAndApTarget() {
        assertApThreatAdvice(ChampionClass.TANK, tankItem());
    }

    @Test
    void apThreatAdviceForBruiserUsesApMessageAndApTarget() {
        assertApThreatAdvice(ChampionClass.BRUISER, item("Maw", new ItemStats(50, 0, 0, 50, 300, 0, 0, 0, 0, 0, 0, 0)));
    }

    private static Item tankItem() {
        return item("Force of Nature", new ItemStats(0, 0, 0, 80, 400, 0, 0, 0, 0, 0, 0, 0));
    }

    private void assertApThreatAdvice(ChampionClass championClass, Item defensiveItem) {
        Player active = player("Me", championClass, false, List.of(), stats(30, 30, 100, 0));
        Player apEnemy = player("Syndra", ChampionClass.MAGE, true, List.of(), stats(50, 50, 0, 400));
        GameState state = game(active, List.of(apEnemy));

        List<BuildAdvice> advices = new BuildAdvisor(state, List.of(defensiveItem)).generateBuildAdvices();

        assertFalse(advices.isEmpty());
        for (BuildAdvice advice : advices) {
            assertTrue(advice.reasoning().contains("AP"), advice.reasoning());
            assertEquals(state.highestApThreat(), advice.targetEnemy());
        }
    }

    @Test
    void returnsNoAdviceForEmptyEnemyList() {
        Player active = player("Me", ChampionClass.MAGE, false, List.of(), AVERAGE_STATS);

        List<BuildAdvice> advices = new BuildAdvisor(game(active, List.of()), List.of(MAGIC_PEN_ITEM))
                .generateBuildAdvices();

        assertTrue(advices.isEmpty());
    }

    @Test
    void doesNotAdviseItemsTheActivePlayerAlreadyOwns() {
        Player active = player("Ahri", ChampionClass.MAGE, false, List.of(MAGIC_PEN_ITEM), AVERAGE_STATS);
        Player enemy = player("Tank", ChampionClass.TANK, true, List.of(), stats(80, 200, 100, 100));

        List<BuildAdvice> advices = new BuildAdvisor(game(active, List.of(enemy)), List.of(MAGIC_PEN_ITEM))
                .generateBuildAdvices();

        assertTrue(advices.isEmpty());
    }
}
