package com.macromind.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WithersTest {

    private static final Player BASE = new Player("p", "Jinx", ChampionClass.MARKSMAN, 9, true, List.of(),
            new PlayerStats(1, 2, 3, 4, 5, 6, 7, 8, 9), Role.ADC, new KDA(1, 2, 3), 500, false, false);

    @Test
    void withGoldChangesOnlyGoldAndLeavesTheOriginalUntouched() {
        Player richer = BASE.withGold(900);

        assertEquals(900, richer.totalGold());
        assertEquals(500, BASE.totalGold());
        assertEquals(BASE.withGold(500), BASE);
    }

    @Test
    void withKdaChangesOnlyTheScore() {
        Player scored = BASE.withKda(new KDA(7, 0, 1));

        assertEquals(new KDA(7, 0, 1), scored.kda());
        assertEquals(BASE.stats(), scored.stats());
        assertEquals(BASE.totalGold(), scored.totalGold());
    }

    @Test
    void asEstimatedFlagsThePlayerAsEstimated() {
        assertFalse(BASE.isEstimated());
        assertTrue(BASE.asEstimated().isEstimated());
    }

    @Test
    void withDeadTogglesTheDeadFlag() {
        assertTrue(BASE.withDead(true).isDead());
        assertFalse(BASE.withDead(true).withDead(false).isDead());
    }

    @Test
    void noneHasNoStatsAtAll() {
        assertEquals(0, ItemStats.NONE.attackDamage() + ItemStats.NONE.abilityPower() + ItemStats.NONE.armor()
                + ItemStats.NONE.magicResist() + ItemStats.NONE.health() + ItemStats.NONE.flatArmorPen()
                + ItemStats.NONE.flatMagicPen() + ItemStats.NONE.omnivamp());
        assertEquals(0.0, ItemStats.NONE.attackSpeed() + ItemStats.NONE.cooldownReduction()
                + ItemStats.NONE.lifesteal() + ItemStats.NONE.criticalStrikeChance());
    }

    @Test
    void itemStatsWithersChangeOneFieldAndKeepTheRest() {
        ItemStats base = new ItemStats(10, 20, 30, 40, 50, 60, 70, 80, 0.5, 0.6, 0.7, 0.8, 9);

        assertEquals(new ItemStats(10, 20, 99, 40, 50, 60, 70, 80, 0.5, 0.6, 0.7, 0.8, 9), base.withArmor(99));
        assertEquals(new ItemStats(10, 20, 30, 99, 50, 60, 70, 80, 0.5, 0.6, 0.7, 0.8, 9), base.withMagicResist(99));
        assertEquals(new ItemStats(10, 20, 30, 40, 50, 60, 99, 80, 0.5, 0.6, 0.7, 0.8, 9), base.withFlatArmorPen(99));
    }
}
