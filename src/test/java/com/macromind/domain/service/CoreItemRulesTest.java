package com.macromind.domain.service;

import com.macromind.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class CoreItemRulesTest {

    private static final Item INFINITY_EDGE = itemWith("Infinity Edge", 3400, new ItemStats(75, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.25));
    private static final Item RABADONS = itemWith("Rabadon's Deathcap", 3600, itemStats(0, 130, 0, 0, 0, 0, 0, 0, 0));
    private static final Item WARMOGS = itemWith("Warmog's Armor", 3100, itemStats(0, 0, 0, 0, 1000, 0, 0, 0, 0));
    private static final Item SUNFIRE = itemWith("Sunfire Aegis", 3200, itemStats(0, 0, 50, 0, 400, 0, 0, 0, 0));
    private static final Item TRIFORCE = itemWith("Trinity Force", 3333, itemStats(36, 0, 0, 0, 333, 0, 0, 0.3, 0));
    private static final Item CENSER = itemWith("Ardent Censer", 2200, itemStats(0, 60, 0, 0, 0, 0, 0, 0, 0), ItemTag.ENCHANTER);
    private static final Item DORANS = itemWith("Doran's Blade", 450, itemStats(10, 0, 0, 0, 80, 0, 0, 0, 0), ItemTag.STARTER);
    private static final Item PHAGE = itemWith("Phage", 1100, itemStats(20, 0, 0, 0, 200, 0, 0, 0, 0), ItemTag.COMPONENT);
    private static final Item IONIAN = itemWith("Ionian Boots of Lucidity", 900, itemStats(0, 0, 0, 0, 0, 0, 0, 0, 10), ItemTag.BOOTS);
    private static final Item[] CATALOGUE = {INFINITY_EDGE, RABADONS, WARMOGS, SUNFIRE, TRIFORCE, CENSER, DORANS, PHAGE};

    private static final List<Player> QUIET_ENEMIES = List.of(
            player("Ornn", ChampionClass.TANK, true, List.of(), stats(80, 80, 100, 100)),
            player("Zed", ChampionClass.ASSASSIN, true, List.of(), stats(80, 80, 100, 100)));

    private static Player active(ChampionClass championClass, int ad, int ap, Item... owned) {
        return player("Me", championClass, false, List.of(owned), new PlayerStats(60, 60, 2200, 2200, ad, ap, 0, 0, 10));
    }

    private static List<BuildAdvice> advise(Player me, Item... items) {
        return new BuildAdvisor(gameAt(900, me, QUIET_ENEMIES), List.of(items)).generateBuildAdvices();
    }

    private static List<String> names(List<BuildAdvice> advice) {
        return advice.stream().map(a -> a.recommendedItem().name()).sorted().toList();
    }

    @Test
    void marksmanGetsAttackItemsOnly() {
        assertEquals(List.of("Infinity Edge", "Trinity Force"), names(advise(active(ChampionClass.MARKSMAN, 150, 0), CATALOGUE)));
    }

    @Test
    void mageGetsAbilityPowerItemsOnly() {
        assertEquals(List.of("Ardent Censer", "Rabadon's Deathcap"), names(advise(active(ChampionClass.MAGE, 0, 200), CATALOGUE)));
    }

    @Test
    void tankGetsHealthAndResistItemsOnly() {
        assertEquals(List.of("Sunfire Aegis", "Warmog's Armor"), names(advise(active(ChampionClass.TANK, 60, 0), CATALOGUE)));
    }

    @Test
    void bruiserGetsDamageItemsWithHealthAndTanks() {
        List<String> names = names(advise(active(ChampionClass.BRUISER, 150, 0), CATALOGUE));

        assertTrue(names.contains("Trinity Force"));
        assertFalse(names.contains("Rabadon's Deathcap"));
    }

    @Test
    void assassinFollowsTheirDamageType() {
        assertTrue(names(advise(active(ChampionClass.ASSASSIN, 250, 0), CATALOGUE)).contains("Infinity Edge"));
        assertTrue(names(advise(active(ChampionClass.ASSASSIN, 0, 250), CATALOGUE)).contains("Rabadon's Deathcap"));
    }

    @Test
    void supportGetsEnchanterItemsOnly() {
        assertEquals(List.of("Ardent Censer"), names(advise(active(ChampionClass.SUPPORT, 0, 60), CATALOGUE)));
    }

    @Test
    void coreAdviceIsSituationalAndBuildsOnYourClass() {
        BuildAdvice advice = advise(active(ChampionClass.MARKSMAN, 150, 0), INFINITY_EDGE).get(0);

        assertEquals(AdvicePriority.SITUATIONAL, advice.priority());
        assertEquals(AdviceCategory.OFFENSIVE, advice.category());
    }

    @Test
    void neverAdvisesStartersOrComponentsAsCoreItems() {
        List<String> names = names(advise(active(ChampionClass.BRUISER, 150, 0), DORANS, PHAGE));

        assertTrue(names.isEmpty(), names.toString());
    }

    @Test
    void stopsAdvisingCoreItemsOnceTheBuildIsComplete() {
        Item[] full = new Item[6];
        for (int i = 0; i < full.length; i++) {
            full[i] = itemWith("Owned" + i, 3000, NO_STATS);
        }

        assertTrue(advise(active(ChampionClass.MARKSMAN, 150, 0, full), INFINITY_EDGE).isEmpty());
    }

    @Test
    void doesNotAdviseACoreItemTheyAlreadyOwn() {
        assertTrue(advise(active(ChampionClass.MARKSMAN, 150, 0, INFINITY_EDGE), INFINITY_EDGE).isEmpty());
    }

    @Test
    void threatAdviceOutranksCoreAdvice() {
        Item grievous = itemWith("Mortal Reminder", 3000, itemStats(35, 0, 0, 0, 0, 0, 0, 0, 0), ItemTag.GRIEVOUS_WOUNDS);
        Player soraka = player("Soraka", ChampionClass.SUPPORT, true, List.of(), stats(50, 50, 0, 100));

        List<BuildAdvice> advice = new BuildAdvisor(gameAt(900, active(ChampionClass.MARKSMAN, 150, 0), List.of(soraka)),
                List.of(INFINITY_EDGE, grievous)).generateBuildAdvices();

        assertEquals("Mortal Reminder", advice.get(0).recommendedItem().name());
        assertEquals("Infinity Edge", advice.get(1).recommendedItem().name());
    }

    @Test
    void coreAdviceIsAbsentWithoutEnemiesOrBeforeTheMidGame() {
        assertTrue(new BuildAdvisor(gameAt(900, active(ChampionClass.MARKSMAN, 150, 0), List.of()), List.of(INFINITY_EDGE)).generateBuildAdvices().isEmpty());
        assertTrue(new BuildAdvisor(gameAt(100, active(ChampionClass.MARKSMAN, 150, 0), QUIET_ENEMIES), List.of(INFINITY_EDGE)).generateBuildAdvices().isEmpty());
    }

    // default boots

    @Test
    void anyClassWithoutBootsGetsHasteBootsAgainstAMixedTeam() {
        List<Player> mixed = List.of(player("Zed", ChampionClass.ASSASSIN, true, List.of(), stats(50, 50, 200, 0)),
                player("Ahri", ChampionClass.MAGE, true, List.of(), stats(50, 50, 0, 200)));

        List<BuildAdvice> advice = new BuildAdvisor(gameAt(600, active(ChampionClass.SUPPORT, 0, 60), mixed), List.of(IONIAN)).generateBuildAdvices();

        assertEquals(List.of("Ionian Boots of Lucidity"), names(advice));
    }

    @Test
    void marksmanGetsAttackSpeedBootsAndMageGetsPenetrationBootsByDefault() {
        Item asBoots = itemWith("Berserker's Greaves", 1100, new ItemStats(0, 0, 0, 0, 0, 0, 0, 0, 0.3, 0, 0, 0), ItemTag.BOOTS);
        Item penBoots = itemWith("Sorcerer's Shoes", 1100, itemStats(0, 0, 0, 0, 0, 0, 12, 0, 0), ItemTag.BOOTS, ItemTag.MAGIC_PENETRATION_FLAT);
        List<Player> mixed = List.of(player("Zed", ChampionClass.ASSASSIN, true, List.of(), stats(50, 50, 200, 0)),
                player("Ahri", ChampionClass.MAGE, true, List.of(), stats(50, 50, 0, 200)));

        assertEquals(List.of("Berserker's Greaves"), names(new BuildAdvisor(gameAt(600, active(ChampionClass.MARKSMAN, 150, 0), mixed),
                List.of(asBoots, penBoots, IONIAN)).generateBuildAdvices()));
        assertEquals(List.of("Sorcerer's Shoes"), names(new BuildAdvisor(gameAt(600, active(ChampionClass.MAGE, 0, 200), mixed),
                List.of(asBoots, penBoots, IONIAN)).generateBuildAdvices()));
    }

    @Test
    void doesNotAdviseBootsToThePlayerWhoOwnsThem() {
        Player booted = active(ChampionClass.SUPPORT, 0, 60, IONIAN);

        assertTrue(new BuildAdvisor(gameAt(600, booted, QUIET_ENEMIES), List.of(IONIAN)).generateBuildAdvices().isEmpty());
    }

    @Test
    void everyCatalogueItemIsEitherAdviseableOrDeliberatelyExcluded() {
        List<Item> all = new ArrayList<>(List.of(CATALOGUE));
        all.add(IONIAN);

        long excluded = all.stream().filter(i -> !ItemFit.isAdviseable(i)).count();

        assertEquals(2, excluded, "only the starter and the plain component are excluded");
    }
}
