package com.macromind.domain.service;

import com.macromind.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class AdviceFitTest {

    private static final Item AS_BOOTS = itemWith("Berserker's Greaves", 1100, itemStats(0, 0, 0, 0, 0, 0, 0, 0.3, 0), ItemTag.BOOTS);
    private static final Item PEN_BOOTS = itemWith("Sorcerer's Shoes", 1100, itemStats(0, 0, 0, 0, 0, 0, 12, 0, 0), ItemTag.BOOTS, ItemTag.MAGIC_PENETRATION_FLAT);

    private static Player enemy(String name, ChampionClass championClass) {
        return player(name, championClass, true, List.of(), stats(80, 80, 100, 100));
    }

    private static final Player ADC = player("Jinx", ChampionClass.MARKSMAN, false, List.of(), AVERAGE_STATS);
    private static final Player MAGE = player("Ahri", ChampionClass.MAGE, false, List.of(), stats(80, 50, 0, 100));

    // boots

    @Test
    void neverAdvisesItemsThatDoNotFitTheDamageType() {
        Item adItem = itemWith("Duskblade", 3100, itemStats(60, 0, 0, 0, 0, 0, 0, 0, 0), ItemTag.ARMOR_PENETRATION_PERCENT);

        Player armoredTank = player("Ornn", ChampionClass.TANK, true, List.of(), stats(200, 80, 100, 100));
        Player armoredBruiser = player("Sett", ChampionClass.BRUISER, true, List.of(), stats(200, 80, 100, 100));

        // the armor rule alone would recommend this AD item; a mage has no use for it
        assertTrue(advise(900, MAGE, List.of(armoredTank, armoredBruiser), adItem).isEmpty());
    }

    private static final Player TANKY_SORAKA = player("Soraka", ChampionClass.SUPPORT, true, List.of(),
            new PlayerStats(80, 80, 4000, 4000, 100, 100, 0, 0, 10));

    @Test
    void excludesStartersAndPlainComponentsButKeepsAntiHealComponents() {
        Item starter = itemWith("Doran's Blade", 450, NO_STATS, ItemTag.STARTER, ItemTag.GRIEVOUS_WOUNDS);
        Item antiHealComponent = itemWith("Bramble Vest", 800, NO_STATS, ItemTag.COMPONENT, ItemTag.GRIEVOUS_WOUNDS);
        Item plainComponent = itemWith("Amplifying Tome", 435, NO_STATS, ItemTag.COMPONENT, ItemTag.PERCENT_HEALTH_DAMAGE);
        Item finished = itemWith("Liandry's Torment", 3000, NO_STATS, ItemTag.PERCENT_HEALTH_DAMAGE);

        List<String> names = names(advise(900, MAGE, List.of(TANKY_SORAKA), starter, antiHealComponent, plainComponent, finished));

        assertTrue(names.contains("Bramble Vest"));
        assertTrue(names.contains("Liandry's Torment"));
        assertFalse(names.contains("Doran's Blade"), "starters are never advised");
        assertFalse(names.contains("Amplifying Tome"), "plain components are never advised");
    }

    @Test
    void advisesBootsComponentsEvenThoughTheyBuildIntoAdvancedBoots() {
        Item bootsWithUpgrade = itemWith("Plated Steelcaps", 1200, itemStats(0, 0, 25, 0, 0, 0, 0, 0, 0), ItemTag.BOOTS, ItemTag.COMPONENT);
        Player zed = player("Zed", ChampionClass.ASSASSIN, true, List.of(), stats(50, 50, 400, 0));

        assertEquals(List.of("Plated Steelcaps"), names(advise(600, ADC, List.of(zed), bootsWithUpgrade)));
    }

    @Test
    void neverAdvisesAnotherPairOfBootsToAPlayerWhoOwnsBoots() {
        Item penBoots = PEN_BOOTS;
        Player booted = player("Ahri", ChampionClass.MAGE, false, List.of(AS_BOOTS), stats(80, 50, 0, 100));
        List<Player> squishy = List.of(enemy("Zed", ChampionClass.ASSASSIN), enemy("Ahri", ChampionClass.MAGE), enemy("Jinx", ChampionClass.MARKSMAN));

        assertTrue(advise(900, booted, squishy, penBoots).isEmpty());
    }
}
