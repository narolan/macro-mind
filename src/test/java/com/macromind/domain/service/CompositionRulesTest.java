package com.macromind.domain.service;

import com.macromind.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class CompositionRulesTest {

    private static final PlayerStats FRAIL_AP = new PlayerStats(30, 30, 1800, 1800, 0, 200, 0, 0, 10);

    private static final Item ARMOR_BOOTS = itemWith("Plated Steelcaps", 1200, itemStats(0, 0, 25, 0, 0, 0, 0, 0, 0), ItemTag.BOOTS);
    private static final Item MR_BOOTS = itemWith("Mercury's Treads", 1250, itemStats(0, 0, 0, 20, 0, 0, 0, 0, 0), ItemTag.BOOTS, ItemTag.TENACITY);
    private static final Item AS_BOOTS = itemWith("Berserker's Greaves", 1100, itemStats(0, 0, 0, 0, 0, 0, 0, 0.3, 0), ItemTag.BOOTS);
    private static final Item PEN_BOOTS = itemWith("Sorcerer's Shoes", 1100, itemStats(0, 0, 0, 0, 0, 0, 12, 0, 0), ItemTag.BOOTS, ItemTag.MAGIC_PENETRATION_FLAT);
    private static final Item LDR = itemWith("Lord Dominik's Regards", 3100, itemStats(35, 0, 0, 0, 0, 0, 0, 0, 0), ItemTag.ARMOR_PENETRATION_PERCENT);
    private static final Item VOID = itemWith("Void Staff", 3000, itemStats(0, 95, 0, 0, 0, 0, 0, 0, 0), ItemTag.MAGIC_PENETRATION_PERCENT);
    private static final Item LETHALITY = itemWith("Serpent's Fang", 2500, itemStats(55, 0, 0, 0, 0, 15, 0, 0, 0), ItemTag.ARMOR_PENETRATION_FLAT, ItemTag.SHIELD_BREAK);
    private static final Item FLAT_MPEN = itemWith("Shadowflame", 3000, itemStats(0, 100, 0, 0, 0, 0, 15, 0, 0), ItemTag.MAGIC_PENETRATION_FLAT);
    private static final Item BANSHEES = itemWith("Banshee's Veil", 2600, itemStats(0, 80, 0, 40, 0, 0, 0, 0, 0), ItemTag.SPELL_SHIELD);

    private static Player enemy(String name, ChampionClass championClass) {
        return player(name, championClass, true, List.of(), stats(80, 80, 100, 100));
    }

    private static final List<Player> TWO_TANKS = List.of(enemy("Ornn", ChampionClass.TANK), enemy("Sett", ChampionClass.BRUISER));
    private static final Player ADC = player("Jinx", ChampionClass.MARKSMAN, false, List.of(), AVERAGE_STATS);
    private static final Player MAGE = player("Ahri", ChampionClass.MAGE, false, List.of(), stats(80, 50, 0, 100));

    // boots

    @Test
    void advisesArmorBootsWhenNoBootsAndEnemyTeamIsHeavyAd() {
        Player zed = player("Zed", ChampionClass.ASSASSIN, true, List.of(), stats(50, 50, 400, 0));

        List<String> names = names(advise(600, ADC, List.of(zed), ARMOR_BOOTS, MR_BOOTS, AS_BOOTS));

        assertEquals(List.of("Plated Steelcaps"), names);
    }

    @Test
    void advisesMagicResistBootsWhenEnemyTeamIsHeavyAp() {
        Player syndra = player("Syndra", ChampionClass.MAGE, true, List.of(), stats(50, 50, 0, 400));

        assertEquals(List.of("Mercury's Treads"), names(advise(600, ADC, List.of(syndra), ARMOR_BOOTS, MR_BOOTS, AS_BOOTS)));
    }

    @Test
    void advisesClassSpecificBootsAgainstAMixedTeam() {
        List<Player> mixed = List.of(player("Zed", ChampionClass.ASSASSIN, true, List.of(), stats(50, 50, 200, 0)),
                player("Syndra", ChampionClass.MAGE, true, List.of(), stats(50, 50, 0, 200)));

        assertEquals(List.of("Sorcerer's Shoes"), names(advise(600, MAGE, mixed, PEN_BOOTS, AS_BOOTS, ARMOR_BOOTS)));
        assertEquals(List.of("Berserker's Greaves"), names(advise(600, ADC, mixed, PEN_BOOTS, AS_BOOTS, ARMOR_BOOTS)));
    }

    @Test
    void doesNotAdviseBootsToAPlayerWhoOwnsBootsOrBeforeThreeMinutes() {
        Player zed = player("Zed", ChampionClass.ASSASSIN, true, List.of(), stats(50, 50, 400, 0));
        Player booted = player("Jinx", ChampionClass.MARKSMAN, false, List.of(AS_BOOTS), AVERAGE_STATS);

        assertTrue(advise(600, booted, List.of(zed), ARMOR_BOOTS).isEmpty());
        assertTrue(advise(100, ADC, List.of(zed), ARMOR_BOOTS).isEmpty());
    }

    // penetration vs team composition

    @Test
    void advisesPercentArmorPenetrationAgainstTwoFrontlinersForPhysicalPlayers() {
        List<BuildAdvice> advice = advise(900, ADC, TWO_TANKS, LDR, VOID);

        assertEquals(List.of("Lord Dominik's Regards"), names(advice));
        assertEquals(AdvicePriority.RECOMMENDED, advice.get(0).priority());
        assertEquals(AdviceCategory.OFFENSIVE, advice.get(0).category());
    }

    @Test
    void advisesPercentMagicPenetrationAgainstTwoFrontlinersForMagicPlayers() {
        assertEquals(List.of("Void Staff"), names(advise(900, MAGE, TWO_TANKS, LDR, VOID)));
    }

    @Test
    void doesNotAdvisePercentPenetrationAgainstASingleFrontliner() {
        assertTrue(advise(900, ADC, List.of(enemy("Ornn", ChampionClass.TANK), enemy("Zed", ChampionClass.ASSASSIN)), LDR).isEmpty());
    }

    @Test
    void advisesLethalityAgainstASquishyTeamAndFlatMagicPenForMages() {
        List<Player> squishy = List.of(enemy("Zed", ChampionClass.ASSASSIN), enemy("Ahri", ChampionClass.MAGE), enemy("Jinx", ChampionClass.MARKSMAN));

        assertEquals(List.of("Serpent's Fang"), names(advise(900, ADC, squishy, LETHALITY, FLAT_MPEN)));
        assertEquals(List.of("Shadowflame"), names(advise(900, MAGE, squishy, LETHALITY, FLAT_MPEN)));
    }

    @Test
    void doesNotAdviseFlatPenetrationWhenTheEnemyHasAFrontlineOrTooFewKnownEnemies() {
        List<Player> withTank = List.of(enemy("Zed", ChampionClass.ASSASSIN), enemy("Ahri", ChampionClass.MAGE), enemy("Ornn", ChampionClass.TANK));
        List<Player> tooFew = List.of(enemy("Zed", ChampionClass.ASSASSIN), enemy("Ahri", ChampionClass.MAGE));

        assertTrue(advise(900, ADC, withTank, LETHALITY).isEmpty());
        assertTrue(advise(900, ADC, tooFew, LETHALITY).isEmpty());
    }

    // shields and assassins

    @Test
    void advisesShieldBreakAgainstTwoShieldChampions() {
        List<Player> shielders = List.of(enemy("Janna", ChampionClass.SUPPORT), enemy("Lulu", ChampionClass.SUPPORT));

        List<BuildAdvice> advice = advise(900, ADC, shielders, LETHALITY);

        assertEquals(List.of("Serpent's Fang"), names(advice));
        assertEquals(AdviceCategory.UTILITY, advice.get(0).category());
    }

    @Test
    void doesNotAdviseShieldBreakAgainstASingleShieldChampion() {
        assertTrue(advise(900, ADC, List.of(enemy("Janna", ChampionClass.SUPPORT), enemy("Ornn", ChampionClass.TANK)), LETHALITY).isEmpty());
    }

    @Test
    void advisesSpellShieldAgainstTwoAssassinsAsRecommended() {
        Player frailMage = player("Ahri", ChampionClass.MAGE, false, List.of(), FRAIL_AP);
        List<Player> assassins = List.of(enemy("Zed", ChampionClass.ASSASSIN), enemy("Talon", ChampionClass.ASSASSIN));

        List<BuildAdvice> advice = advise(900, frailMage, assassins, BANSHEES);

        assertEquals(List.of("Banshee's Veil"), names(advice));
        assertEquals(AdvicePriority.RECOMMENDED, advice.get(0).priority());
    }

    @Test
    void advisesSpellShieldAsCriticalAgainstAFedAssassin() {
        Player frailMage = player("Ahri", ChampionClass.MAGE, false, List.of(), FRAIL_AP);
        Player fedZed = withKda(enemy("Zed", ChampionClass.ASSASSIN), 6, 0, 2);

        List<BuildAdvice> advice = advise(900, frailMage, List.of(fedZed), BANSHEES);

        assertEquals(AdvicePriority.CRITICAL, advice.get(0).priority());
        assertEquals("Zed", advice.get(0).targetEnemy().championName());
    }

    @Test
    void doesNotAdviseAntiAssassinItemsToSturdyPlayers() {
        Player tank = player("Ornn", ChampionClass.TANK, false, List.of(), new PlayerStats(300, 300, 4500, 4500, 100, 0, 0, 0, 10));
        List<Player> assassins = List.of(enemy("Zed", ChampionClass.ASSASSIN), enemy("Talon", ChampionClass.ASSASSIN));

        assertTrue(advise(900, tank, assassins, BANSHEES).isEmpty());
    }

    // item selection and ranking

    @Test
    void advisesTenacityAgainstThreeHardCrowdControlChampions() {
        List<Player> ccTeam = List.of(enemy("Leona", ChampionClass.TANK), enemy("Nautilus", ChampionClass.TANK), enemy("Amumu", ChampionClass.TANK));

        List<BuildAdvice> advice = advise(900, ADC, ccTeam, SCIMITAR);

        assertEquals(List.of("Mercurial Scimitar"), names(advice));
        assertEquals(AdviceCategory.UTILITY, advice.get(0).category());
    }

    @Test
    void doesNotAdviseTenacityAgainstTwoCrowdControlChampions() {
        List<Player> ccTeam = List.of(enemy("Leona", ChampionClass.TANK), enemy("Nautilus", ChampionClass.TANK), enemy("Zed", ChampionClass.ASSASSIN));

        assertTrue(advise(900, ADC, ccTeam, SCIMITAR).isEmpty());
    }

    @Test
    void tenacityBootsAreNotAdvisedToAPlayerWhoAlreadyHasBoots() {
        Player booted = player("Jinx", ChampionClass.MARKSMAN, false, List.of(AS_BOOTS), AVERAGE_STATS);
        List<Player> ccTeam = List.of(enemy("Leona", ChampionClass.TANK), enemy("Nautilus", ChampionClass.TANK), enemy("Amumu", ChampionClass.TANK));

        assertTrue(advise(900, booted, ccTeam, TENACITY_BOOTS).isEmpty());
    }

    // cleanse

    private static final Item TENACITY_BOOTS = itemWith("Mercury's Treads", 1250, itemStats(0, 0, 0, 20, 0, 0, 0, 0, 0), ItemTag.BOOTS, ItemTag.TENACITY);
    private static final Item SCIMITAR = itemWith("Mercurial Scimitar", 3000, itemStats(40, 0, 0, 30, 0, 0, 0, 0, 0), ItemTag.TENACITY);
    private static final Item QSS = itemWith("Quicksilver Sash", 1300, itemStats(0, 0, 0, 30, 0, 0, 0, 0, 0), ItemTag.CLEANSE, ItemTag.COMPONENT);

    @Test
    void advisesCleanseAgainstThreeHardCrowdControlChampionsEvenAsAComponent() {
        List<Player> ccTeam = List.of(enemy("Leona", ChampionClass.TANK), enemy("Nautilus", ChampionClass.TANK), enemy("Amumu", ChampionClass.TANK));

        assertEquals(List.of("Quicksilver Sash"), names(advise(900, ADC, ccTeam, QSS)));
    }

    @Test
    void doesNotAdviseCleanseWithoutHeavyCrowdControl() {
        assertTrue(advise(900, ADC, List.of(enemy("Leona", ChampionClass.TANK), enemy("Zed", ChampionClass.ASSASSIN)), QSS).isEmpty());
    }
}
