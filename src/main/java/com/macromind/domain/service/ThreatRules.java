package com.macromind.domain.service;

import com.macromind.domain.model.AdviceCategory;
import com.macromind.domain.model.AdvicePriority;
import com.macromind.domain.model.BuildAdvice;
import com.macromind.domain.model.GameState;
import com.macromind.domain.model.Item;
import com.macromind.domain.model.ItemTag;
import com.macromind.domain.model.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Rules that answer a measurable threat: healing, armor, magic resist, big health pools, and heavy AD or AP. */
final class ThreatRules {

    private static final int HIGH_RESIST_GAP = 150;
    private static final int HIGH_HP = 3500;
    private static final int VERY_HIGH_HP = 5000;
    private static final int ASSASSIN_BUILD_THRESHOLD = 150;

    private static final String GUARDIAN_ANGEL = "Guardian Angel";
    private static final String ZHONYAS_HOURGLASS = "Zhonya's Hourglass";
    private static final String BANSHEES_VEIL = "Banshee's Veil";

    private static final String AD_REASON = "Enemy team has heavy AD threat. Consider buying this item to survive their damage.";
    private static final String AP_REASON = "Enemy team has heavy AP threat. Consider buying this item to survive their damage.";

    private final GameState gameState;
    private final AdviceFactory factory;
    private final Player me;
    private final EnemyThreat threat;

    ThreatRules(GameState gameState, AdviceFactory factory) {
        this.gameState = gameState;
        this.factory = factory;
        this.me = gameState.activePlayer();
        this.threat = EnemyThreat.of(gameState);
    }

    /** The order matters: the ranker keeps it as the tie-break between equally urgent advice. */
    List<BuildAdvice> advise() {
        List<BuildAdvice> advice = new ArrayList<>();
        advice.addAll(antiHeal());
        advice.addAll(antiArmor());
        advice.addAll(antiMagicResist());
        advice.addAll(percentHealthDamage());
        advice.addAll(heavyAdThreat());
        advice.addAll(heavyApThreat());
        return advice;
    }

    private List<BuildAdvice> antiHeal() {
        if (!TeamProfile.of(gameState).hasHeavyHealing() || me.hasGrievousWoundsItem()) {
            return List.of();
        }
        Player target = firstEnemy(Player::hasHighSustainStats);
        return factory.create(item -> item.hasTag(ItemTag.GRIEVOUS_WOUNDS),
                "Enemy team has heavy healing. Consider buying this item to apply Grievous Wounds.",
                AdvicePriority.CRITICAL, AdviceCategory.UTILITY, target);
    }

    private List<BuildAdvice> antiArmor() {
        if (gameState.highestEnemyArmor() - me.totalFlatArmorPen() <= HIGH_RESIST_GAP) {
            return List.of();
        }
        Player target = firstEnemy(enemy -> enemy.stats().armor() > HIGH_RESIST_GAP);
        return factory.create(item -> item.hasTag(ItemTag.ARMOR_PENETRATION_PERCENT),
                "Enemy team has high armor. Consider buying this item to penetrate their defenses.",
                AdvicePriority.RECOMMENDED, AdviceCategory.OFFENSIVE, target);
    }

    private List<BuildAdvice> antiMagicResist() {
        if (gameState.highestEnemyMagicResist() - me.totalFlatMagicPen() <= HIGH_RESIST_GAP) {
            return List.of();
        }
        Player target = firstEnemy(enemy -> enemy.stats().magicResist() > HIGH_RESIST_GAP);
        return factory.create(item -> item.hasTag(ItemTag.MAGIC_PENETRATION_PERCENT),
                "Enemy team has high magic resistance. Consider buying this item to penetrate their defenses.",
                AdvicePriority.RECOMMENDED, AdviceCategory.OFFENSIVE, target);
    }

    private List<BuildAdvice> percentHealthDamage() {
        int highestHealth = gameState.highestEnemyMaxHealth();
        if (highestHealth <= HIGH_HP) {
            return List.of();
        }
        AdvicePriority priority = highestHealth > VERY_HIGH_HP ? AdvicePriority.CRITICAL : AdvicePriority.RECOMMENDED;
        Player target = firstEnemy(enemy -> enemy.stats().maxHealth() > HIGH_HP);
        return factory.create(item -> item.hasTag(ItemTag.PERCENT_HEALTH_DAMAGE),
                "Enemy team has high health. Consider buying this item to deal percentage health damage.",
                priority, AdviceCategory.OFFENSIVE, target);
    }

    private List<BuildAdvice> heavyAdThreat() {
        if (threat.adRatio() <= AdviceThresholds.HEAVY_AD_RATIO) {
            return List.of();
        }
        return survive(AD_REASON, threat.highestAdThreat(), item -> item.stats().armor() > 0,
                List.of(ZHONYAS_HOURGLASS));
    }

    private List<BuildAdvice> heavyApThreat() {
        if (threat.adRatio() >= AdviceThresholds.HEAVY_AP_RATIO) {
            return List.of();
        }
        return survive(AP_REASON, threat.highestApThreat(), item -> item.stats().magicResist() > 0,
                List.of(BANSHEES_VEIL, ZHONYAS_HOURGLASS));
    }

    /** Defensive items fitted to the player's class, against whichever damage type is heavy. */
    private List<BuildAdvice> survive(String reasoning, Player target, Predicate<Item> resist, List<String> assassinApItems) {
        Predicate<Item> blocksBurst = resist.or(item -> item.hasAnyTag(ItemTag.SPELL_SHIELD, ItemTag.BURST_RESISTANCE));
        boolean squishy = Squishiness.of(me);
        return switch (me.championClass()) {
            case SUPPORT -> List.of();
            case ASSASSIN -> assassinSurvive(reasoning, target, assassinApItems);
            case MARKSMAN -> squishy
                    ? factory.create(item -> ItemFit.givesPhysicalDamage(item) && blocksBurst.test(item),
                            reasoning, AdvicePriority.CRITICAL, AdviceCategory.DEFENSIVE, target)
                    : List.of();
            case MAGE -> squishy
                    ? factory.create(item -> givesMageStats(item) && blocksBurst.test(item),
                            reasoning, AdvicePriority.CRITICAL, AdviceCategory.DEFENSIVE, target)
                    : List.of();
            case TANK -> factory.create(item -> ItemFit.isTankItem(item) && (resist.test(item) || ItemFit.hasNoResist(item)),
                    reasoning, AdvicePriority.RECOMMENDED, AdviceCategory.DEFENSIVE, target);
            case BRUISER -> squishy
                    ? factory.create(item -> resist.test(item) && (item.stats().health() > 0
                                    || item.stats().attackDamage() > 0 || item.hasTag(ItemTag.BURST_RESISTANCE)),
                            reasoning, AdvicePriority.RECOMMENDED, AdviceCategory.DEFENSIVE, target)
                    : List.of();
        };
    }

    private List<BuildAdvice> assassinSurvive(String reasoning, Player target, List<String> apItems) {
        if (me.stats().attackDamage() > ASSASSIN_BUILD_THRESHOLD) {
            return factory.create(item -> item.name().equals(GUARDIAN_ANGEL),
                    reasoning, AdvicePriority.RECOMMENDED, AdviceCategory.DEFENSIVE, target);
        }
        if (me.stats().abilityPower() > ASSASSIN_BUILD_THRESHOLD) {
            return factory.create(item -> apItems.contains(item.name()),
                    reasoning, AdvicePriority.RECOMMENDED, AdviceCategory.DEFENSIVE, target);
        }
        return List.of();
    }

    /** Ability power or cooldown reduction: what a mage looks for in a defensive item. */
    private static boolean givesMageStats(Item item) {
        return ItemFit.givesAbilityPower(item) || item.stats().cooldownReduction() > 0;
    }

    private Player firstEnemy(Predicate<Player> filter) {
        return gameState.enemies().stream().filter(filter).findFirst().orElse(null);
    }
}
