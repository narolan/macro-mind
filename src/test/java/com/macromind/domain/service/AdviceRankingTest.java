package com.macromind.domain.service;

import com.macromind.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class AdviceRankingTest {

    private static final Item VOID = itemWith("Void Staff", 3000, itemStats(0, 95, 0, 0, 0, 0, 0, 0, 0), ItemTag.MAGIC_PENETRATION_PERCENT);

    private static Player enemy(String name, ChampionClass championClass) {
        return player(name, championClass, true, List.of(), stats(80, 80, 100, 100));
    }

    private static final List<Player> TWO_TANKS = List.of(enemy("Ornn", ChampionClass.TANK), enemy("Sett", ChampionClass.BRUISER));
    private static final Player MAGE = player("Ahri", ChampionClass.MAGE, false, List.of(), stats(80, 50, 0, 100));

    // boots

    @Test
    void adviceForTheSameItemFromSeveralRulesAppearsOnce() {
        Player tankyMr = player("Ornn", ChampionClass.TANK, true, List.of(), stats(80, 200, 100, 0));
        Player sett = player("Sett", ChampionClass.BRUISER, true, List.of(), stats(80, 200, 100, 0));

        List<BuildAdvice> advice = advise(900, MAGE, List.of(tankyMr, sett), VOID);

        assertEquals(1, advice.size());
    }

    @Test
    void ranksByPriorityThenAffordabilityThenPrice() {
        Item expensive = itemWith("Expensive Pen", 3000, itemStats(0, 95, 0, 0, 0, 0, 0, 0, 0), ItemTag.MAGIC_PENETRATION_PERCENT);
        Item cheap = itemWith("Cheap Pen", 800, itemStats(0, 40, 0, 0, 0, 0, 0, 0, 0), ItemTag.MAGIC_PENETRATION_PERCENT);
        Item healer = itemWith("Morello", 2200, itemStats(0, 90, 0, 0, 0, 0, 0, 0, 0), ItemTag.GRIEVOUS_WOUNDS);
        Player poorMage = withGold(MAGE, 1000);
        List<Player> enemies = List.of(enemy("Ornn", ChampionClass.TANK), enemy("Sett", ChampionClass.BRUISER), enemy("Soraka", ChampionClass.SUPPORT));

        List<String> names = names(advise(900, poorMage, enemies, expensive, healer, cheap));

        assertEquals(List.of("Morello", "Cheap Pen", "Expensive Pen"), names);
    }

    @Test
    void ranksItemsWantedByMoreRulesFirstWithinTheSamePriority() {
        Item both = itemWith("Both Rules", 3000, itemStats(0, 90, 0, 0, 0, 0, 0, 0, 0),
                ItemTag.MAGIC_PENETRATION_PERCENT, ItemTag.PERCENT_HEALTH_DAMAGE);
        Item one = itemWith("One Rule", 500, itemStats(0, 40, 0, 0, 0, 0, 0, 0, 0), ItemTag.MAGIC_PENETRATION_PERCENT);

        Player bigTank = player("Ornn", ChampionClass.TANK, true, List.of(), new PlayerStats(80, 80, 4000, 4000, 100, 100, 0, 0, 10));
        Player bigBruiser = player("Sett", ChampionClass.BRUISER, true, List.of(), new PlayerStats(80, 80, 4000, 4000, 100, 100, 0, 0, 10));

        assertEquals(List.of("Both Rules", "One Rule"), names(advise(900, MAGE, List.of(bigTank, bigBruiser), one, both)));
    }

    @Test
    void prefersTheMostExpensiveAffordableItem() {
        Item expensive = itemWith("Expensive Pen", 3000, itemStats(0, 95, 0, 0, 0, 0, 0, 0, 0), ItemTag.MAGIC_PENETRATION_PERCENT);
        Item cheap = itemWith("Cheap Pen", 800, itemStats(0, 40, 0, 0, 0, 0, 0, 0, 0), ItemTag.MAGIC_PENETRATION_PERCENT);

        assertEquals(List.of("Expensive Pen", "Cheap Pen"), names(advise(900, withGold(MAGE, 5000), TWO_TANKS, cheap, expensive)));
    }

}
