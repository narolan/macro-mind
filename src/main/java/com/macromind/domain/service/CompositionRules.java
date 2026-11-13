package com.macromind.domain.service;

import com.macromind.domain.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Rules driven by what the enemy team is made of (frontline, squishies, assassins, shields, crowd control). */
final class CompositionRules {

    static final int FED_KILL_LEAD = 4;

    private final GameState gameState;
    private final AdviceFactory factory;
    private final TeamProfile team;
    private final Player me;
    private final EnemyThreat threat;

    CompositionRules(GameState gameState, AdviceFactory factory) {
        this.gameState = gameState;
        this.factory = factory;
        this.team = TeamProfile.of(gameState);
        this.me = gameState.activePlayer();
        this.threat = EnemyThreat.of(gameState);
    }

    List<BuildAdvice> advise() {
        List<BuildAdvice> advice = new ArrayList<>();
        advice.addAll(boots());
        advice.addAll(percentPenetrationVsFrontline());
        advice.addAll(flatPenetrationVsSquishy());
        advice.addAll(shieldBreak());
        advice.addAll(spellShieldVsAssassins());
        advice.addAll(tenacityVsCrowdControl());
        advice.addAll(cleanseVsCrowdControl());
        return advice;
    }

    private List<BuildAdvice> boots() {
        if (me.hasBoots() || gameState.gameTimeInSeconds() < AdviceThresholds.BOOTS_MIN_SECONDS) {
            return List.of();
        }
        double adRatio = threat.adRatio();
        if (adRatio > AdviceThresholds.HEAVY_AD_RATIO) {
            return factory.create(i -> i.hasTag(ItemTag.BOOTS) && i.stats().armor() > 0,
                    "Enemy team is mostly AD and you have no boots. Armor boots cut their damage.",
                    AdvicePriority.RECOMMENDED, AdviceCategory.DEFENSIVE, threat.highestAdThreat());
        }
        if (adRatio < AdviceThresholds.HEAVY_AP_RATIO) {
            return factory.create(i -> i.hasTag(ItemTag.BOOTS) && i.stats().magicResist() > 0,
                    "Enemy team is mostly AP and you have no boots. Magic resist boots cut their damage.",
                    AdvicePriority.RECOMMENDED, AdviceCategory.DEFENSIVE, threat.highestApThreat());
        }
        if (me.dealsMagicDamage()) {
            return factory.create(i -> i.hasTag(ItemTag.BOOTS) && i.hasTag(ItemTag.MAGIC_PENETRATION_FLAT),
                    "You have no boots and the enemy damage is mixed. Penetration boots add damage.",
                    AdvicePriority.RECOMMENDED, AdviceCategory.OFFENSIVE, null);
        }
        if (me.dealsPhysicalDamage()) {
            return factory.create(i -> i.hasTag(ItemTag.BOOTS) && i.stats().attackSpeed() > 0,
                    "You have no boots and the enemy damage is mixed. Attack speed boots add damage.",
                    AdvicePriority.RECOMMENDED, AdviceCategory.OFFENSIVE, null);
        }
        return List.of();
    }

    private List<BuildAdvice> percentPenetrationVsFrontline() {
        if (!team.hasFrontline() || gameState.gameTimeInSeconds() < AdviceThresholds.MID_GAME_SECONDS) {
            return List.of();
        }
        Player frontliner = firstOf(ChampionClass.TANK, ChampionClass.BRUISER);
        if (me.dealsPhysicalDamage()) {
            return factory.create(i -> i.hasTag(ItemTag.ARMOR_PENETRATION_PERCENT),
                    "Enemy team has several frontliners. Percent armor penetration keeps your damage relevant.",
                    AdvicePriority.RECOMMENDED, AdviceCategory.OFFENSIVE, frontliner);
        }
        if (me.dealsMagicDamage()) {
            return factory.create(i -> i.hasTag(ItemTag.MAGIC_PENETRATION_PERCENT),
                    "Enemy team has several frontliners. Percent magic penetration keeps your damage relevant.",
                    AdvicePriority.RECOMMENDED, AdviceCategory.OFFENSIVE, frontliner);
        }
        return List.of();
    }

    private List<BuildAdvice> flatPenetrationVsSquishy() {
        if (!team.isSquishy() || gameState.gameTimeInSeconds() < AdviceThresholds.MID_GAME_SECONDS) {
            return List.of();
        }
        if (me.dealsPhysicalDamage()) {
            return factory.create(i -> i.hasTag(ItemTag.ARMOR_PENETRATION_FLAT),
                    "Enemy team has no frontline. Lethality is the most efficient damage against squishy targets.",
                    AdvicePriority.RECOMMENDED, AdviceCategory.OFFENSIVE, null);
        }
        if (me.dealsMagicDamage()) {
            return factory.create(i -> i.hasTag(ItemTag.MAGIC_PENETRATION_FLAT),
                    "Enemy team has no frontline. Flat magic penetration is the most efficient against squishy targets.",
                    AdvicePriority.RECOMMENDED, AdviceCategory.OFFENSIVE, null);
        }
        return List.of();
    }

    private List<BuildAdvice> shieldBreak() {
        if (!team.hasShieldChampions() || !me.dealsPhysicalDamage()) {
            return List.of();
        }
        return factory.create(i -> i.hasTag(ItemTag.SHIELD_BREAK),
                "Enemy team has several shield champions. Lethality items reduce the shields they gain.",
                AdvicePriority.RECOMMENDED, AdviceCategory.UTILITY, null);
    }

    private List<BuildAdvice> spellShieldVsAssassins() {
        if (!Squishiness.of(me)) {
            return List.of();
        }
        List<Player> assassins = team.assassins();
        Optional<Player> fed = assassins.stream()
                .filter(a -> a.kda().kills() - a.kda().deaths() >= FED_KILL_LEAD)
                .findFirst();
        if (fed.isPresent()) {
            return factory.create(i -> i.hasTag(ItemTag.SPELL_SHIELD),
                    "A fed assassin is on the enemy team. A spell shield blocks their opening ability.",
                    AdvicePriority.CRITICAL, AdviceCategory.DEFENSIVE, fed.get());
        }
        if (assassins.size() < TeamProfile.ASSASSINS_MIN) {
            return List.of();
        }
        return factory.create(i -> i.hasTag(ItemTag.SPELL_SHIELD),
                "Enemy team has several assassins. A spell shield blocks their opening ability.",
                AdvicePriority.RECOMMENDED, AdviceCategory.DEFENSIVE, assassins.get(0));
    }

    private List<BuildAdvice> tenacityVsCrowdControl() {
        if (!team.hasHeavyCrowdControl()) {
            return List.of();
        }
        return factory.create(i -> i.hasTag(ItemTag.TENACITY),
                "Enemy team has heavy crowd control. Tenacity shortens how long you are locked down.",
                AdvicePriority.RECOMMENDED, AdviceCategory.UTILITY, null);
    }

    private List<BuildAdvice> cleanseVsCrowdControl() {
        if (!team.hasHeavyCrowdControl()) {
            return List.of();
        }
        return factory.create(i -> i.hasTag(ItemTag.CLEANSE),
                "Enemy team has heavy crowd control. A cleanse lets you break out of it.",
                AdvicePriority.RECOMMENDED, AdviceCategory.UTILITY, null);
    }

    private Player firstOf(ChampionClass... classes) {
        List<ChampionClass> wanted = List.of(classes);
        return gameState.enemies().stream().filter(p -> wanted.contains(p.championClass())).findFirst().orElse(null);
    }
}
