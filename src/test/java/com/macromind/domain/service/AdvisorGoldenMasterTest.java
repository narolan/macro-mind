package com.macromind.domain.service;

import com.macromind.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the exact advice (items, order, priority, reason, target) for a few fixed games, so a refactor of the
 * rules engine cannot change what players are told without a test failing. Update the expectations on purpose only.
 */
class AdvisorGoldenMasterTest {

    private static final List<Item> CATALOGUE = List.of(
            itemWith("Mortal Reminder", 3000, itemStats(35, 0, 0, 0, 0, 0, 0, 0, 0), ItemTag.GRIEVOUS_WOUNDS, ItemTag.ARMOR_PENETRATION_PERCENT),
            itemWith("Thornmail", 2450, itemStats(0, 0, 75, 0, 150, 0, 0, 0, 0), ItemTag.GRIEVOUS_WOUNDS),
            itemWith("Lord Dominik's Regards", 3100, itemStats(35, 0, 0, 0, 0, 0, 0, 0, 0), ItemTag.ARMOR_PENETRATION_PERCENT),
            itemWith("Void Staff", 3000, itemStats(0, 95, 0, 0, 0, 0, 0, 0, 0), ItemTag.MAGIC_PENETRATION_PERCENT),
            itemWith("Liandry's Torment", 3000, itemStats(0, 90, 0, 0, 300, 0, 0, 0, 0), ItemTag.PERCENT_HEALTH_DAMAGE),
            itemWith("Blade of The Ruined King", 3200, itemStats(40, 0, 0, 0, 0, 0, 0, 0.25, 0), ItemTag.PERCENT_HEALTH_DAMAGE, ItemTag.ON_HIT),
            itemWith("Banshee's Veil", 2600, itemStats(0, 80, 0, 40, 0, 0, 0, 0, 0), ItemTag.SPELL_SHIELD),
            itemWith("Guardian Angel", 3200, itemStats(55, 0, 40, 0, 0, 0, 0, 0, 0), ItemTag.BURST_RESISTANCE),
            itemWith("Zhonya's Hourglass", 3250, itemStats(0, 105, 50, 0, 0, 0, 0, 0, 0), ItemTag.BURST_RESISTANCE),
            itemWith("Plated Steelcaps", 1200, itemStats(0, 0, 25, 0, 0, 0, 0, 0, 0), ItemTag.BOOTS),
            itemWith("Mercury's Treads", 1250, itemStats(0, 0, 0, 20, 0, 0, 0, 0, 0), ItemTag.BOOTS, ItemTag.TENACITY),
            itemWith("Berserker's Greaves", 1100, new ItemStats(0, 0, 0, 0, 0, 0, 0, 0, 0.3, 0, 0, 0), ItemTag.BOOTS),
            itemWith("Warmog's Armor", 3100, itemStats(0, 0, 0, 0, 1000, 0, 0, 0, 0)),
            itemWith("Infinity Edge", 3400, new ItemStats(75, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.25)),
            itemWith("Rabadon's Deathcap", 3600, itemStats(0, 130, 0, 0, 0, 0, 0, 0, 0)),
            itemWith("Serpent's Fang", 2500, itemStats(55, 0, 0, 0, 0, 15, 0, 0, 0), ItemTag.ARMOR_PENETRATION_FLAT, ItemTag.SHIELD_BREAK),
            itemWith("Mercurial Scimitar", 3000, itemStats(40, 0, 0, 30, 0, 0, 0, 0, 0), ItemTag.TENACITY, ItemTag.CLEANSE));

    private static Player enemy(String name, ChampionClass cls, int armor, int mr, int hp, int ad, int ap, int gold, KDA kda) {
        return new Player(name, name, cls, 15, true, List.of(), new PlayerStats(armor, mr, hp, hp, ad, ap, 0, 0, 10),
                Role.MID, kda, gold, true, false);
    }

    private static Player me(ChampionClass cls, int ad, int ap, int armor, int mr, int hp, int gold, Item... owned) {
        return new Player("Me", "Me", cls, 15, false, List.of(owned), new PlayerStats(armor, mr, hp, hp, ad, ap, 0, 0, 10),
                Role.ADC, new KDA(3, 1, 4), gold, false, false);
    }

    /** One line per advice: enough to detect any change in item, order, urgency, reason or target. */
    private static List<String> digest(GameState state) {
        List<String> lines = new ArrayList<>();
        for (BuildAdvice a : new BuildAdvisor(state, CATALOGUE).generateBuildAdvices()) {
            lines.add(a.priority().name().charAt(0) + "|" + a.category().name().charAt(0) + "|" + a.recommendedItem().name()
                    + "|" + a.reasoning().substring(0, Math.min(34, a.reasoning().length()))
                    + "|" + (a.targetEnemy() == null ? "-" : a.targetEnemy().championName()) + "|" + a.confidence().name().charAt(0));
        }
        return lines;
    }

    private static final List<Player> HEALING_TANKY_TEAM = List.of(
            enemy("Soraka", ChampionClass.SUPPORT, 60, 60, 2400, 80, 300, 7000, new KDA(0, 2, 9)),
            enemy("Ornn", ChampionClass.TANK, 210, 90, 5200, 120, 0, 9000, new KDA(2, 3, 5)),
            enemy("Sett", ChampionClass.BRUISER, 190, 80, 4100, 260, 0, 11000, new KDA(6, 2, 3)),
            enemy("Zed", ChampionClass.ASSASSIN, 70, 50, 2200, 380, 0, 12000, new KDA(9, 1, 2)),
            enemy("Jinx", ChampionClass.MARKSMAN, 60, 50, 2100, 330, 0, 10000, new KDA(5, 3, 6)));

    private static final List<Player> ASSASSIN_TEAM = List.of(
            enemy("Zed", ChampionClass.ASSASSIN, 70, 50, 2200, 380, 0, 12000, new KDA(10, 1, 2)),
            enemy("Talon", ChampionClass.ASSASSIN, 70, 50, 2200, 340, 0, 9000, new KDA(4, 2, 2)),
            enemy("Ahri", ChampionClass.MAGE, 50, 50, 2000, 90, 420, 9500, new KDA(3, 2, 7)),
            enemy("Lux", ChampionClass.MAGE, 50, 50, 1900, 80, 460, 8800, new KDA(2, 4, 9)));

    private static final List<Player> AP_TEAM = List.of(
            enemy("Syndra", ChampionClass.MAGE, 50, 50, 2000, 80, 500, 11000, new KDA(7, 1, 3)),
            enemy("Ahri", ChampionClass.MAGE, 50, 50, 2000, 90, 450, 9500, new KDA(3, 2, 7)),
            enemy("Karma", ChampionClass.SUPPORT, 50, 50, 1900, 80, 380, 7000, new KDA(0, 3, 12)));

    @Test
    void marksmanAgainstAHealingTankyTeam() {
        GameState state = gameAt(1300, me(ChampionClass.MARKSMAN, 200, 0, 40, 40, 1800, 3500), HEALING_TANKY_TEAM);

        assertEquals(List.of(
            "C|U|Mortal Reminder|Enemy team has heavy healing. Cons|-|E",
            "C|O|Blade of The Ruined King|Enemy team has high health. Consid|Ornn|E",
            "C|D|Guardian Angel|Enemy team has heavy AD threat. Co|Zed|E",
            "C|U|Thornmail|Enemy team has heavy healing. Cons|-|E",
            "R|O|Lord Dominik's Regards|Enemy team has high armor. Conside|Ornn|E",
            "R|D|Plated Steelcaps|Enemy team is mostly AD and you ha|Zed|E",
            "S|O|Infinity Edge|Nothing urgent to counter. This it|-|E",
            "S|O|Mercurial Scimitar|Nothing urgent to counter. This it|-|E",
            "S|O|Serpent's Fang|Nothing urgent to counter. This it|-|E",
            "S|U|Berserker's Greaves|You have no boots yet. These suit |-|E"),
                digest(state));
    }

    @Test
    void squishyMageAgainstAnAssassinTeam() {
        GameState state = gameAt(1300, me(ChampionClass.MAGE, 0, 260, 40, 40, 1800, 3000), ASSASSIN_TEAM);

        assertEquals(List.of(
            "C|D|Banshee's Veil|A fed assassin is on the enemy tea|Zed|E",
            "S|O|Void Staff|Nothing urgent to counter. This it|-|E",
            "S|O|Liandry's Torment|Nothing urgent to counter. This it|-|E",
            "S|O|Zhonya's Hourglass|Nothing urgent to counter. This it|-|E",
            "S|O|Rabadon's Deathcap|Nothing urgent to counter. This it|-|E"),
                digest(state));
    }

    @Test
    void tankAgainstAnApTeamWithABootsGap() {
        GameState state = gameAt(900, me(ChampionClass.TANK, 80, 0, 160, 70, 3600, 2000), AP_TEAM);

        assertEquals(List.of(
            "R|D|Mercury's Treads|Enemy team has heavy AP threat. Co|Syndra|E",
            "R|D|Warmog's Armor|Enemy team has heavy AP threat. Co|Syndra|E",
            "S|D|Thornmail|Nothing urgent to counter. This it|-|E"),
                digest(state));
    }

    @Test
    void assassinWithApAgainstAHealingTankyTeam() {
        GameState state = gameAt(1500, me(ChampionClass.ASSASSIN, 90, 220, 60, 50, 2100, 4000, item("Boots", NO_STATS, ItemTag.BOOTS)), HEALING_TANKY_TEAM);

        assertEquals(List.of(
            "C|O|Liandry's Torment|Enemy team has high health. Consid|Ornn|E",
            "C|D|Banshee's Veil|A fed assassin is on the enemy tea|Zed|E",
            "C|U|Thornmail|Enemy team has heavy healing. Cons|-|E",
            "R|D|Zhonya's Hourglass|Enemy team has heavy AD threat. Co|Zed|E",
            "R|O|Void Staff|Enemy team has several frontliners|Ornn|E",
            "S|O|Rabadon's Deathcap|Nothing urgent to counter. This it|-|E"),
                digest(state));
    }

    /** With no enemies there is no enemy damage to react to: only the generic "no boots yet" hint remains. */
    @Test
    void emptyEnemyTeamOnlyGetsTheGenericBootsHint() {
        assertEquals(List.of("S|U|Berserker's Greaves|You have no boots yet. These suit |-|E"),
                digest(gameAt(1500, me(ChampionClass.MARKSMAN, 200, 0, 40, 40, 1800, 3500), List.of())));
    }
}
