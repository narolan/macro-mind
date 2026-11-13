package com.macromind.domain.service;

import com.macromind.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class BuildAdvisorBranchesTest {

    private static final PlayerStats SQUISHY = new PlayerStats(30, 30, 1800, 1800, 100, 0, 0, 0, 10);
    private static final PlayerStats SQUISHY_AP = new PlayerStats(30, 30, 1800, 1800, 0, 200, 0, 0, 10);
    private static final PlayerStats SQUISHY_AD = new PlayerStats(30, 30, 1800, 1800, 200, 0, 0, 0, 10);

    private static final Item AD_ARMOR = item("Death's Dance", new ItemStats(60, 0, 40, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    private static final Item AD_MR = item("Maw", new ItemStats(60, 0, 0, 40, 0, 0, 0, 0, 0, 0, 0, 0));
    private static final Item AP_ARMOR = item("Seraph", new ItemStats(0, 80, 40, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    private static final Item AP_MR = item("Banshee's Veil", new ItemStats(0, 80, 0, 40, 0, 0, 0, 0, 0, 0, 0, 0), ItemTag.SPELL_SHIELD);
    private static final Item GUARDIAN_ANGEL = item("Guardian Angel", NO_STATS);
    private static final Item ZHONYAS = item("Zhonya's Hourglass", NO_STATS);
    private static final Item BANSHEES = item("Banshee's Veil", NO_STATS);

    private static final Player AD_ENEMY = player("Zed", ChampionClass.ASSASSIN, true, List.of(), stats(50, 50, 400, 0));
    private static final Player AP_ENEMY = player("Syndra", ChampionClass.MAGE, true, List.of(), stats(50, 50, 0, 400));

    private static List<BuildAdvice> advise(ChampionClass championClass, PlayerStats stats, Player enemy, Item... items) {
        Player active = player("Me", championClass, false, List.of(), stats);
        return urgent(new BuildAdvisor(game(active, List.of(enemy)), List.of(items)).generateBuildAdvices());
    }

    /** Threat rules are under test here; the low-priority class-fit advice has its own tests. */
    private static List<BuildAdvice> urgent(List<BuildAdvice> advice) {
        return advice.stream().filter(a -> a.priority() != AdvicePriority.SITUATIONAL).toList();
    }

    private static List<String> names(List<BuildAdvice> advice) {
        return advice.stream().map(a -> a.recommendedItem().name()).toList();
    }

    @Test
    void squishyMarksmanVsAdAdvisesOffensiveArmorItemsAsCritical() {
        List<BuildAdvice> advice = advise(ChampionClass.MARKSMAN, SQUISHY, AD_ENEMY, AD_ARMOR, AD_MR);

        assertEquals(List.of("Death's Dance"), names(advice));
        assertEquals(AdvicePriority.CRITICAL, advice.get(0).priority());
        assertEquals(AdviceCategory.DEFENSIVE, advice.get(0).category());
    }

    @Test
    void squishyMarksmanVsApAdvisesOffensiveMagicResistItems() {
        assertEquals(List.of("Maw"), names(advise(ChampionClass.MARKSMAN, SQUISHY, AP_ENEMY, AD_ARMOR, AD_MR)));
    }

    @Test
    void squishyMageVsAdAdvisesApArmorItemsAndVsApMagicResistItems() {
        // a spell shield counts against AD burst too, as in the original rule
        assertEquals(List.of("Seraph", "Banshee's Veil"), names(advise(ChampionClass.MAGE, SQUISHY, AD_ENEMY, AP_ARMOR, AP_MR)));
        assertEquals(List.of("Banshee's Veil"), names(advise(ChampionClass.MAGE, SQUISHY, AP_ENEMY, AP_ARMOR, AP_MR)));
    }

    @Test
    void tankVsAdAdvisesArmorOrHealthAsRecommended() {
        List<BuildAdvice> advice = advise(ChampionClass.TANK, SQUISHY, AD_ENEMY, AD_ARMOR, AD_MR);

        assertEquals(List.of("Death's Dance"), names(advice));
        assertEquals(AdvicePriority.RECOMMENDED, advice.get(0).priority());
    }

    @Test
    void squishyBruiserVsAdAdvisesArmorWithHealthOrDamage() {
        assertEquals(List.of("Death's Dance"), names(advise(ChampionClass.BRUISER, SQUISHY, AD_ENEMY, AD_ARMOR, AD_MR)));
    }

    @Test
    void assassinWithHighAdBuysGuardianAngelVsAdAndVsAp() {
        assertEquals(List.of("Guardian Angel"), names(advise(ChampionClass.ASSASSIN, SQUISHY_AD, AD_ENEMY, GUARDIAN_ANGEL, ZHONYAS)));
        assertEquals(List.of("Guardian Angel"), names(advise(ChampionClass.ASSASSIN, SQUISHY_AD, AP_ENEMY, GUARDIAN_ANGEL, BANSHEES)));
    }

    @Test
    void assassinWithHighApBuysZhonyasVsAdAndBansheesOrZhonyasVsAp() {
        assertEquals(List.of("Zhonya's Hourglass"), names(advise(ChampionClass.ASSASSIN, SQUISHY_AP, AD_ENEMY, GUARDIAN_ANGEL, ZHONYAS, BANSHEES)));
        assertEquals(List.of("Zhonya's Hourglass", "Banshee's Veil"),
                names(advise(ChampionClass.ASSASSIN, SQUISHY_AP, AP_ENEMY, GUARDIAN_ANGEL, ZHONYAS, BANSHEES)));
    }

    @Test
    void supportNeverGetsThreatAdvice() {
        assertTrue(advise(ChampionClass.SUPPORT, SQUISHY, AD_ENEMY, AD_ARMOR, AP_MR).isEmpty());
    }

    @Test
    void sturdyMageGetsNoThreatAdvice() {
        PlayerStats sturdy = new PlayerStats(200, 200, 4000, 4000, 0, 200, 0, 0, 10);
        Player active = player("Me", ChampionClass.MAGE, false,
                List.of(item("Shield", NO_STATS, ItemTag.SPELL_SHIELD)), sturdy);

        assertTrue(urgent(new BuildAdvisor(game(active, List.of(AD_ENEMY)), List.of(AP_ARMOR)).generateBuildAdvices()).isEmpty());
    }

    @Test
    void highArmorEnemyTriggersPercentArmorPenetrationAdvice() {
        Item lordDominiks = item("Lord Dominik's", NO_STATS, ItemTag.ARMOR_PENETRATION_PERCENT);
        Player active = player("Me", ChampionClass.MARKSMAN, false, List.of(), AVERAGE_STATS);
        Player tank = player("Tank", ChampionClass.TANK, true, List.of(), stats(200, 50, 100, 0));

        List<BuildAdvice> advice = new BuildAdvisor(game(active, List.of(tank)), List.of(lordDominiks)).generateBuildAdvices();

        assertEquals(List.of("Lord Dominik's"), names(advice));
        assertEquals(AdviceCategory.OFFENSIVE, advice.get(0).category());
    }

    @Test
    void percentHealthDamageIsRecommendedAbove3500AndCriticalAbove5000() {
        Item botrk = item("Blade", NO_STATS, ItemTag.PERCENT_HEALTH_DAMAGE);
        Player active = player("Me", ChampionClass.MARKSMAN, false, List.of(), AVERAGE_STATS);
        Player big = player("Big", ChampionClass.TANK, true, List.of(), new PlayerStats(50, 50, 4000, 4000, 100, 0, 0, 0, 10));
        Player huge = player("Huge", ChampionClass.TANK, true, List.of(), new PlayerStats(50, 50, 5500, 5500, 100, 0, 0, 0, 10));

        assertEquals(AdvicePriority.RECOMMENDED,
                new BuildAdvisor(game(active, List.of(big)), List.of(botrk)).generateBuildAdvices().get(0).priority());
        assertEquals(AdvicePriority.CRITICAL,
                new BuildAdvisor(game(active, List.of(huge)), List.of(botrk)).generateBuildAdvices().get(0).priority());
    }

    @Test
    void healingEnemyChampionTriggersGrievousWoundsAdviceUnlessAlreadyOwned() {
        Item morello = item("Morello", NO_STATS, ItemTag.GRIEVOUS_WOUNDS);
        Player soraka = player("Soraka", ChampionClass.SUPPORT, true, List.of(), stats(50, 50, 0, 100));
        Player withoutGw = player("Me", ChampionClass.MAGE, false, List.of(), AVERAGE_STATS);
        Player withGw = player("Me", ChampionClass.MAGE, false, List.of(item("Thornmail", NO_STATS, ItemTag.GRIEVOUS_WOUNDS)), AVERAGE_STATS);

        assertEquals(AdvicePriority.CRITICAL,
                new BuildAdvisor(game(withoutGw, List.of(soraka)), List.of(morello)).generateBuildAdvices().get(0).priority());
        assertTrue(new BuildAdvisor(game(withGw, List.of(soraka)), List.of(morello)).generateBuildAdvices().isEmpty());
    }
}
