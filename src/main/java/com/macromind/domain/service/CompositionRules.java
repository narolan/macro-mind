package com.macromind.domain.service;

import com.macromind.domain.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Rules driven by what the enemy team is made of (frontline, squishies, assassins, shields, crowd control). */
final class CompositionRules {

    static final long BOOTS_MIN_SECONDS = 180;
    static final long MID_GAME_SECONDS = 600;
    static final int FED_KILL_LEAD = 4;
    private static final double HEAVY_AD_RATIO = 0.7;
    private static final double HEAVY_AP_RATIO = 0.3;

    private final GameState gameState;
    private final AdviceFactory factory;
    private final TeamProfile team;
    private final Player me;

    CompositionRules(GameState gameState, AdviceFactory factory) {
        this.gameState = gameState;
        this.factory = factory;
        this.team = TeamProfile.of(gameState);
        this.me = gameState.activePlayer();
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

    private static boolean isBoots(Item item) {
        return item.tags().contains(ItemTag.BOOTS);
    }

    private static boolean tagged(Item item, ItemTag tag) {
        return item.tags().contains(tag);
    }

    private List<BuildAdvice> rule(Predicate<Item> matcher, String reasoning, AdvicePriority priority,
                                   AdviceCategory category, Player target) {
        return factory.create(new AdviceRule(matcher, reasoning, priority, category), target);
    }

    private List<BuildAdvice> boots() {
        if (me.hasBoots() || gameState.gameTimeInSeconds() < BOOTS_MIN_SECONDS) {
            return List.of();
        }
        double adRatio = gameState.getEnemyAdRatio();
        if (adRatio > HEAVY_AD_RATIO) {
            return rule(i -> isBoots(i) && i.stats().armor() > 0,
                    "Enemy team is mostly AD and you have no boots. Armor boots cut their damage.",
                    AdvicePriority.RECOMMENDED, AdviceCategory.DEFENSIVE, gameState.highestAdThreat());
        }
        if (adRatio < HEAVY_AP_RATIO) {
            return rule(i -> isBoots(i) && i.stats().magicResist() > 0,
                    "Enemy team is mostly AP and you have no boots. Magic resist boots cut their damage.",
                    AdvicePriority.RECOMMENDED, AdviceCategory.DEFENSIVE, gameState.highestApThreat());
        }
        if (me.dealsMagicDamage()) {
            return rule(i -> isBoots(i) && tagged(i, ItemTag.MAGIC_PENETRATION_FLAT),
                    "You have no boots and the enemy damage is mixed. Penetration boots add damage.",
                    AdvicePriority.RECOMMENDED, AdviceCategory.OFFENSIVE, null);
        }
        if (me.dealsPhysicalDamage()) {
            return rule(i -> isBoots(i) && i.stats().attackSpeed() > 0,
                    "You have no boots and the enemy damage is mixed. Attack speed boots add damage.",
                    AdvicePriority.RECOMMENDED, AdviceCategory.OFFENSIVE, null);
        }
        return List.of();
    }

    private List<BuildAdvice> percentPenetrationVsFrontline() {
        if (!team.hasFrontline() || gameState.gameTimeInSeconds() < MID_GAME_SECONDS) {
            return List.of();
        }
        Player frontliner = firstOf(ChampionClass.TANK, ChampionClass.BRUISER);
        if (me.dealsPhysicalDamage()) {
            return rule(i -> tagged(i, ItemTag.ARMOR_PENETRATION_PERCENT),
                    "Enemy team has several frontliners. Percent armor penetration keeps your damage relevant.",
                    AdvicePriority.RECOMMENDED, AdviceCategory.OFFENSIVE, frontliner);
        }
        if (me.dealsMagicDamage()) {
            return rule(i -> tagged(i, ItemTag.MAGIC_PENETRATION_PERCENT),
                    "Enemy team has several frontliners. Percent magic penetration keeps your damage relevant.",
                    AdvicePriority.RECOMMENDED, AdviceCategory.OFFENSIVE, frontliner);
        }
        return List.of();
    }

    private List<BuildAdvice> flatPenetrationVsSquishy() {
        if (!team.isSquishy() || gameState.gameTimeInSeconds() < MID_GAME_SECONDS) {
            return List.of();
        }
        if (me.dealsPhysicalDamage()) {
            return rule(i -> tagged(i, ItemTag.ARMOR_PENETRATION_FLAT),
                    "Enemy team has no frontline. Lethality is the most efficient damage against squishy targets.",
                    AdvicePriority.RECOMMENDED, AdviceCategory.OFFENSIVE, null);
        }
        if (me.dealsMagicDamage()) {
            return rule(i -> tagged(i, ItemTag.MAGIC_PENETRATION_FLAT),
                    "Enemy team has no frontline. Flat magic penetration is the most efficient against squishy targets.",
                    AdvicePriority.RECOMMENDED, AdviceCategory.OFFENSIVE, null);
        }
        return List.of();
    }

    private List<BuildAdvice> shieldBreak() {
        if (!team.hasShieldChampions() || !me.dealsPhysicalDamage()) {
            return List.of();
        }
        return rule(i -> tagged(i, ItemTag.SHIELD_BREAK),
                "Enemy team has several shield champions. Lethality items reduce the shields they gain.",
                AdvicePriority.RECOMMENDED, AdviceCategory.UTILITY, null);
    }

    private List<BuildAdvice> spellShieldVsAssassins() {
        if (!gameState.isActivePlayerSquishy()) {
            return List.of();
        }
        List<Player> assassins = team.assassins();
        Player fed = assassins.stream()
                .filter(a -> a.kda().kills() - a.kda().deaths() >= FED_KILL_LEAD)
                .findFirst().orElse(null);
        if (fed == null && assassins.size() < TeamProfile.ASSASSINS_MIN) {
            return List.of();
        }
        return rule(i -> tagged(i, ItemTag.SPELL_SHIELD),
                fed != null ? "A fed assassin is on the enemy team. A spell shield blocks their opening ability."
                        : "Enemy team has several assassins. A spell shield blocks their opening ability.",
                fed != null ? AdvicePriority.CRITICAL : AdvicePriority.RECOMMENDED,
                AdviceCategory.DEFENSIVE, fed != null ? fed : assassins.get(0));
    }

    private List<BuildAdvice> tenacityVsCrowdControl() {
        if (!team.hasHeavyCrowdControl()) {
            return List.of();
        }
        return rule(i -> tagged(i, ItemTag.TENACITY),
                "Enemy team has heavy crowd control. Tenacity shortens how long you are locked down.",
                AdvicePriority.RECOMMENDED, AdviceCategory.UTILITY, null);
    }

    private List<BuildAdvice> cleanseVsCrowdControl() {
        if (!team.hasHeavyCrowdControl()) {
            return List.of();
        }
        return rule(i -> tagged(i, ItemTag.CLEANSE),
                "Enemy team has heavy crowd control. A cleanse lets you break out of it.",
                AdvicePriority.RECOMMENDED, AdviceCategory.UTILITY, null);
    }

    private Player firstOf(ChampionClass... classes) {
        List<ChampionClass> wanted = List.of(classes);
        return gameState.enemies().stream().filter(p -> wanted.contains(p.championClass())).findFirst().orElse(null);
    }
}
