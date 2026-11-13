package com.macromind.domain.service;

import com.macromind.domain.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public record BuildAdvisor(
        GameState gameState,
        List<Item> possibleItems
) {

    public List<BuildAdvice> generateBuildAdvices() {
        List<BuildAdvice> advices = new ArrayList<>();
        Player activePlayer = gameState.activePlayer();
        if(gameState.hasEnemyWithHeavyHealing() && !activePlayer.hasGrievousWoundsItem()) {
            advices.addAll(generateAntiHealAdvice());
        }
        if((gameState.getHighestArmorEnemy() - activePlayer.getFlatArmorPen()) > 150) {
            advices.addAll(generateAntiArmorAdvice());
        }
        if((gameState.getHighestMagicResistEnemy() - activePlayer.getFlatMagicPen()) > 150) {
            advices.addAll(generateAntiMagicResistAdvice());
        }
        if(gameState.getHighestHpEnemy() > 3500) {
            if(gameState.getHighestHpEnemy() > 5000) {
                advices.addAll(generatePercentHealthDamageAdvice(AdvicePriority.CRITICAL));
            } else {
                advices.addAll(generatePercentHealthDamageAdvice(AdvicePriority.RECOMMENDED));
            }
        }
        if(gameState.getEnemyAdRatio() > 0.7) {
            advices.addAll(generateHeavyAdEnemyThreatAdvice(activePlayer));
        }
        if(gameState.getEnemyAdRatio() < 0.3) {
            advices.addAll(generateHeavyApEnemyThreatAdvice(activePlayer));
        }
        AdviceFactory factory = new AdviceFactory(gameState, possibleItems);
        advices.addAll(new CompositionRules(gameState, factory).advise());
        advices.addAll(new CoreItemRules(gameState, factory).advise());
        return AdviceRanker.rank(advices, activePlayer.totalGold());
    }

    private static final String AD_REASON = "Enemy team has heavy AD threat. Consider buying this item to survive their damage.";
    private static final String AP_REASON = "Enemy team has heavy AP threat. Consider buying this item to survive their damage.";
    private static final int ASSASSIN_BUILD_THRESHOLD = 150;

    private List<BuildAdvice> adviceFor(AdviceRule rule, Player target) {
        return new AdviceFactory(gameState, possibleItems).create(rule, target);
    }

    private static boolean offensive(Item item) {
        return item.stats().attackDamage() > 0 || item.stats().attackSpeed() > 0 || item.stats().criticalStrikeChance() > 0;
    }

    private static boolean magical(Item item) {
        return item.stats().abilityPower() > 0 || item.stats().cooldownReduction() > 0;
    }

    private List<BuildAdvice> generateHeavyAdEnemyThreatAdvice(Player activePlayer) {
        return heavyThreatAdvice(activePlayer, AD_REASON, gameState.highestAdThreat(),
                item -> item.stats().armor() > 0,
                "Guardian Angel", "Zhonya's Hourglass");
    }

    private List<BuildAdvice> generateHeavyApEnemyThreatAdvice(Player activePlayer) {
        return heavyThreatAdvice(activePlayer, AP_REASON, gameState.highestApThreat(),
                item -> item.stats().magicResist() > 0,
                "Guardian Angel", "Banshee's Veil", "Zhonya's Hourglass");
    }

    private List<BuildAdvice> heavyThreatAdvice(Player activePlayer, String reasoning, Player target,
                                                Predicate<Item> resist, String... assassinItems) {
        boolean squishy = gameState.isActivePlayerSquishy();
        return switch (activePlayer.championClass()) {
            case MARKSMAN -> squishy
                    ? adviceFor(new AdviceRule(
                            item -> offensive(item) && (resist.test(item) || item.hasAnyTag(ItemTag.SPELL_SHIELD, ItemTag.BURST_RESISTANCE)),
                            reasoning, AdvicePriority.CRITICAL, AdviceCategory.DEFENSIVE), target)
                    : List.<BuildAdvice>of();
            case MAGE -> squishy
                    ? adviceFor(new AdviceRule(
                            item -> magical(item) && (resist.test(item) || item.hasAnyTag(ItemTag.SPELL_SHIELD, ItemTag.BURST_RESISTANCE)),
                            reasoning, AdvicePriority.CRITICAL, AdviceCategory.DEFENSIVE), target)
                    : List.<BuildAdvice>of();
            case TANK -> adviceFor(new AdviceRule(
                    item -> item.stats().health() > 0 || resist.test(item),
                    reasoning, AdvicePriority.RECOMMENDED, AdviceCategory.DEFENSIVE), target);
            case BRUISER -> squishy
                    ? adviceFor(new AdviceRule(
                            item -> resist.test(item) && (item.stats().health() > 0 || item.stats().attackDamage() > 0 || item.hasTag(ItemTag.BURST_RESISTANCE)),
                            reasoning, AdvicePriority.RECOMMENDED, AdviceCategory.DEFENSIVE), target)
                    : List.<BuildAdvice>of();
            case ASSASSIN -> assassinAdvice(activePlayer, reasoning, target, assassinItems);
            case SUPPORT -> List.of();
        };
    }

    private List<BuildAdvice> assassinAdvice(Player activePlayer, String reasoning, Player target, String[] assassinItems) {
        List<String> names = List.of(assassinItems);
        if (activePlayer.stats().attackDamage() > ASSASSIN_BUILD_THRESHOLD) {
            return adviceFor(new AdviceRule(item -> item.name().equals("Guardian Angel"),
                    reasoning, AdvicePriority.RECOMMENDED, AdviceCategory.DEFENSIVE), target);
        }
        if (activePlayer.stats().abilityPower() > ASSASSIN_BUILD_THRESHOLD) {
            return adviceFor(new AdviceRule(item -> names.contains(item.name()) && !item.name().equals("Guardian Angel"),
                    reasoning, AdvicePriority.RECOMMENDED, AdviceCategory.DEFENSIVE), target);
        }
        return List.of();
    }

    private List<BuildAdvice> generateAntiHealAdvice() {
        Player target = gameState.enemies().stream()
                .filter(enemy -> enemy.stats().healthRegen() > 100 || enemy.stats().lifesteal() + enemy.stats().omnivamp() >= 15)
                .findFirst().orElse(null);
        return adviceFor(new AdviceRule(item -> item.hasAnyTag(ItemTag.GRIEVOUS_WOUNDS),
                "Enemy team has heavy healing. Consider buying this item to apply Grievous Wounds.",
                AdvicePriority.CRITICAL, AdviceCategory.UTILITY), target);
    }

    private List<BuildAdvice> generateAntiArmorAdvice() {
        Player target = gameState.enemies().stream().filter(enemy -> enemy.stats().armor() > 150).findFirst().orElse(null);
        return adviceFor(new AdviceRule(item -> item.hasAnyTag(ItemTag.ARMOR_PENETRATION_PERCENT),
                "Enemy team has high armor. Consider buying this item to penetrate their defenses.",
                AdvicePriority.RECOMMENDED, AdviceCategory.OFFENSIVE), target);
    }

    private List<BuildAdvice> generateAntiMagicResistAdvice() {
        Player target = gameState.enemies().stream().filter(enemy -> enemy.stats().magicResist() > 150).findFirst().orElse(null);
        return adviceFor(new AdviceRule(item -> item.hasAnyTag(ItemTag.MAGIC_PENETRATION_PERCENT),
                "Enemy team has high magic resistance. Consider buying this item to penetrate their defenses.",
                AdvicePriority.RECOMMENDED, AdviceCategory.OFFENSIVE), target);
    }

    private List<BuildAdvice> generatePercentHealthDamageAdvice(AdvicePriority advicePriority) {
        Player target = gameState.enemies().stream().filter(enemy -> enemy.stats().maxHealth() > 3500).findFirst().orElse(null);
        return adviceFor(new AdviceRule(item -> item.hasAnyTag(ItemTag.PERCENT_HEALTH_DAMAGE),
                "Enemy team has high health. Consider buying this item to deal percentage health damage.",
                advicePriority, AdviceCategory.OFFENSIVE), target);
    }

}
