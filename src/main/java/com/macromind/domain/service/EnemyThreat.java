package com.macromind.domain.service;

import com.macromind.domain.model.ChampionClass;
import com.macromind.domain.model.GameState;
import com.macromind.domain.model.Player;

import java.util.Comparator;
import java.util.List;

/** How threatening the enemy team is, split into physical (AD) and magical (AP) damage. */
record EnemyThreat(List<Player> enemies) {

    private static final double GOLD_PER_THREAT_STEP = 8000.0;
    private static final double KILL_WEIGHT = 0.12;
    private static final double ASSIST_WEIGHT = 0.04;
    private static final double DEATH_WEIGHT = 0.06;
    private static final double MIN_KDA_MULTIPLIER = 0.5;
    private static final double NEUTRAL_AD_RATIO = 0.5;

    static EnemyThreat of(GameState state) {
        return new EnemyThreat(state.enemies());
    }

    /** The enemy with the highest AD threat, or null when there are no enemies. */
    Player highestAdThreat() {
        return enemies.stream().max(Comparator.comparingDouble(EnemyThreat::adThreat)).orElse(null);
    }

    /** The enemy with the highest AP threat, or null when there are no enemies. */
    Player highestApThreat() {
        return enemies.stream().max(Comparator.comparingDouble(EnemyThreat::apThreat)).orElse(null);
    }

    /** The enemy with the highest combined AD and AP threat, or null when there are no enemies. */
    Player mostDangerous() {
        return enemies.stream().max(Comparator.comparingDouble(p -> adThreat(p) + apThreat(p))).orElse(null);
    }

    double totalAdThreat() {
        return enemies.stream().mapToDouble(EnemyThreat::adThreat).sum();
    }

    double totalApThreat() {
        return enemies.stream().mapToDouble(EnemyThreat::apThreat).sum();
    }

    /** Share of the enemy damage that is physical: 1.0 is all AD, 0.0 all AP, 0.5 when there is no threat at all. */
    double adRatio() {
        double totalAd = totalAdThreat();
        double totalThreat = totalAd + totalApThreat();
        return totalThreat == 0 ? NEUTRAL_AD_RATIO : totalAd / totalThreat;
    }

    private static double adThreat(Player player) {
        double baseAd = isEstimatedMage(player) ? 0 : player.stats().attackDamage();
        return baseAd * kdaMultiplier(player) * goldMultiplier(player);
    }

    private static double apThreat(Player player) {
        // Data Dragon has no base AP, so an estimated mage's damage would otherwise read as pure AD.
        double baseAp = player.stats().abilityPower() + (isEstimatedMage(player) ? player.stats().attackDamage() : 0);
        return baseAp * kdaMultiplier(player) * goldMultiplier(player);
    }

    private static boolean isEstimatedMage(Player player) {
        return player.isEstimated() && player.championClass() == ChampionClass.MAGE;
    }

    private static double kdaMultiplier(Player player) {
        double raw = 1.0 + player.kda().kills() * KILL_WEIGHT + player.kda().assists() * ASSIST_WEIGHT
                - player.kda().deaths() * DEATH_WEIGHT;
        return Math.max(MIN_KDA_MULTIPLIER, raw);
    }

    private static double goldMultiplier(Player player) {
        return 1.0 + player.totalGold() / GOLD_PER_THREAT_STEP;
    }
}
