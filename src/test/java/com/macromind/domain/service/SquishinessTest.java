package com.macromind.domain.service;

import com.macromind.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class SquishinessTest {

    private static Player of(ChampionClass championClass, PlayerStats stats, Item... items) {
        return player("Me", championClass, false, List.of(items), stats);
    }

    private static PlayerStats stats(int armor, int mr, int health) {
        return new PlayerStats(armor, mr, health, health, 100, 0, 0, 0, 10);
    }

    @Test
    void aFragileMageWithoutProtectionIsSquishy() {
        assertTrue(Squishiness.of(of(ChampionClass.MAGE, stats(30, 30, 1800))));
    }

    @Test
    void aSturdyTankIsNotSquishy() {
        assertFalse(Squishiness.of(of(ChampionClass.TANK, stats(200, 200, 4500))));
    }

    @Test
    void needsThreeOfTheFiveSignals() {
        // class (1) + low armor (2) only: not enough
        assertFalse(Squishiness.of(of(ChampionClass.MARKSMAN, stats(50, 200, 4000), item("Shield", NO_STATS, ItemTag.SPELL_SHIELD))));
        // class (1) + low armor (2) + low MR (3): enough
        assertTrue(Squishiness.of(of(ChampionClass.MARKSMAN, stats(50, 50, 4000), item("Shield", NO_STATS, ItemTag.SPELL_SHIELD))));
    }

    @Test
    void aShieldOrBurstResistanceItemRemovesOneSignal() {
        PlayerStats frail = stats(30, 30, 1800);
        Item shield = item("Banshee's Veil", NO_STATS, ItemTag.SPELL_SHIELD);

        assertTrue(Squishiness.of(of(ChampionClass.TANK, frail)));          // low hp + armor + mr + no protection = 4
        assertTrue(Squishiness.of(of(ChampionClass.TANK, frail, shield)));  // 3 left, still squishy
        assertFalse(Squishiness.of(of(ChampionClass.TANK, stats(30, 200, 1800), shield))); // 2 left
    }

    @Test
    void thresholdsAreExclusive() {
        // exactly 2500 hp and exactly 100 resists are NOT low
        assertFalse(Squishiness.of(of(ChampionClass.TANK, stats(100, 100, 2500))));
    }
}
