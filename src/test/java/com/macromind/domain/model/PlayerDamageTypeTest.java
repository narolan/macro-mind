package com.macromind.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class PlayerDamageTypeTest {

    private static Player of(ChampionClass championClass, int ad, int ap) {
        return player("P", championClass, false, List.of(), stats(50, 50, ad, ap));
    }

    @Test
    void mageDealsMagicDamageOnly() {
        assertTrue(of(ChampionClass.MAGE, 100, 0).dealsMagicDamage());
        assertFalse(of(ChampionClass.MAGE, 100, 0).dealsPhysicalDamage());
    }

    @Test
    void marksmanDealsPhysicalDamageOnly() {
        assertTrue(of(ChampionClass.MARKSMAN, 100, 0).dealsPhysicalDamage());
        assertFalse(of(ChampionClass.MARKSMAN, 100, 0).dealsMagicDamage());
    }

    @Test
    void assassinAndBruiserFollowTheirHigherStat() {
        assertTrue(of(ChampionClass.ASSASSIN, 200, 0).dealsPhysicalDamage());
        assertTrue(of(ChampionClass.ASSASSIN, 0, 200).dealsMagicDamage());
        assertTrue(of(ChampionClass.BRUISER, 100, 100).dealsPhysicalDamage());
    }

    @Test
    void tanksAndSupportsHaveNoDamageProfile() {
        assertFalse(of(ChampionClass.TANK, 100, 0).dealsPhysicalDamage());
        assertFalse(of(ChampionClass.SUPPORT, 0, 100).dealsMagicDamage());
    }

    @Test
    void hasBootsWhenAnyOwnedItemIsBoots() {
        Item boots = itemWith("Boots", 1100, NO_STATS, ItemTag.BOOTS);

        assertTrue(player("P", ChampionClass.MAGE, false, List.of(boots), AVERAGE_STATS).hasBoots());
        assertFalse(of(ChampionClass.MAGE, 0, 100).hasBoots());
    }
}
