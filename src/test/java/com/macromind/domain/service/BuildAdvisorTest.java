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

    private static final Item JAKSHO = item("Jak'Sho, The Protean", itemStats(0, 0, 45, 45, 350, 0, 0, 0, 0));

    private static boolean advises(ChampionClass championClass, Item candidate) {
        Player me = player("Vayne", championClass, false, List.of(), AVERAGE_STATS);
        Player enemy = player("Zed", ChampionClass.ASSASSIN, true, List.of(), stats(80, 200, 100, 100));
        return new BuildAdvisor(game(me, List.of(enemy)), List.of(candidate)).generateBuildAdvices().stream()
                .anyMatch(a -> a.recommendedItem().equals(candidate));
    }

    @Test
    void doesNotAdviseAPureTankItemToAMarksmanOrMage() {
        assertFalse(advises(ChampionClass.MARKSMAN, JAKSHO));
        assertFalse(advises(ChampionClass.MAGE, JAKSHO));
    }

    @Test
    void stillAdvisesAPureTankItemToABruiser() {
        assertTrue(advises(ChampionClass.BRUISER, JAKSHO));
    }

    @Test
    void givesNoClassBasedAdviceForAChampionItCouldNotLookUp() {
        assertFalse(advises(ChampionClass.UNKNOWN, JAKSHO));
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
            assertEquals(EnemyThreat.of(state).highestApThreat(), advice.targetEnemy());
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

    @Test
    void marksAdviceEstimatedWhenEnemyStatsAreEstimated() {
        Player active = player("Ahri", ChampionClass.MAGE, false, List.of(), AVERAGE_STATS);
        Player estimatedEnemy = estimated(player("Tank", ChampionClass.TANK, true, List.of(), stats(80, 200, 100, 100)));

        List<BuildAdvice> advices = new BuildAdvisor(game(active, List.of(estimatedEnemy)), List.of(MAGIC_PEN_ITEM))
                .generateBuildAdvices();

        assertFalse(advices.isEmpty());
        assertTrue(advices.stream().allMatch(a -> a.confidence() == Confidence.ESTIMATED));
    }

    @Test
    void marksAdviceExactWhenEnemyStatsAreExact() {
        Player active = player("Ahri", ChampionClass.MAGE, false, List.of(), AVERAGE_STATS);
        Player enemy = player("Tank", ChampionClass.TANK, true, List.of(), stats(80, 200, 100, 100));

        List<BuildAdvice> advices = new BuildAdvisor(game(active, List.of(enemy)), List.of(MAGIC_PEN_ITEM))
                .generateBuildAdvices();

        assertTrue(advices.stream().allMatch(a -> a.confidence() == Confidence.EXACT));
    }

    @Test
    void doesNotAdviseAnItemOwnedUnderADifferentId() {
        Item owned = new Item(1, "Void Staff", 3000, NO_STATS, java.util.Set.of(ItemTag.MAGIC_PENETRATION_PERCENT));
        Item otherCopy = new Item(2, "Void Staff", 3000, NO_STATS, java.util.Set.of(ItemTag.MAGIC_PENETRATION_PERCENT));
        Player active = player("Ahri", ChampionClass.MAGE, false, List.of(owned), AVERAGE_STATS);
        Player enemy = player("Tank", ChampionClass.TANK, true, List.of(), stats(80, 200, 100, 100));

        List<BuildAdvice> advices = new BuildAdvisor(game(active, List.of(enemy)), List.of(otherCopy)).generateBuildAdvices();

        assertTrue(advices.isEmpty());
    }

    @Test
    void adviseGrievousWoundsAgainstExactlyFifteenPercentLifesteal() {
        Item grievous = item("Thornmail", NO_STATS, ItemTag.GRIEVOUS_WOUNDS);
        Player active = player("Ahri", ChampionClass.MAGE, false, List.of(), AVERAGE_STATS);
        PlayerStats leech = new PlayerStats(80, 50, 2000, 2000, 100, 0, 15, 0, 10);
        Player enemy = player("Vayne", ChampionClass.MARKSMAN, true, List.of(), leech);

        List<BuildAdvice> advices = new BuildAdvisor(game(active, List.of(enemy)), List.of(grievous)).generateBuildAdvices();

        assertFalse(advices.isEmpty());
    }
}
