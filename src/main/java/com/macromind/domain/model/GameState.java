package com.macromind.domain.model;

import java.util.List;

import static com.macromind.domain.model.ChampionClass.*;

public record GameState(
        long gameTimeInSeconds,
        Player activePlayer,
        List<Player> allies,
        List<Player> enemies,
        GameMode gameMode
) {

    private static final int HEAVY_SUSTAIN_PERCENT = 15;
    private static final List<String> HIGH_HEALING_CHAMPIONS = List.of("Aatrox", "Soraka", "Dr. Mundo", "Sylas", "Vladimir", "Swain", "Briar", "Warwick");

    public Player highestAdThreat() {
        return enemies.stream()
                .max((p1, p2) -> Double.compare(calculateIndividualAdThreat(p1), calculateIndividualAdThreat(p2)))
                .orElse(null);
    }

    public Player highestApThreat() {
        return enemies.stream()
                .max((p1, p2) -> Double.compare(calculateIndividualApThreat(p1), calculateIndividualApThreat(p2)))
                .orElse(null);
    }

    public int getHighestArmorEnemy() {
        return enemies.stream()
                .mapToInt(player -> player.stats().armor())
                .max()
                .orElse(0);
    }

    public int getHighestMagicResistEnemy() {
        return enemies.stream()
                .mapToInt(player -> player.stats().magicResist())
                .max()
                .orElse(0);
    }

    public boolean hasEnemyWithHeavyHealing() {
        return enemies.stream()
                .anyMatch(enemy ->
                        HIGH_HEALING_CHAMPIONS.contains(enemy.championName()) ||
                        (enemy.stats().lifesteal() + enemy.stats().omnivamp() >= HEAVY_SUSTAIN_PERCENT) ||
                        enemy.stats().healthRegen() > 100);
    }

    public double getTotalEnemyAdThreat() {
        return enemies.stream()
                .mapToDouble(this::calculateIndividualAdThreat)
                .sum();
    }

    public double getTotalEnemyApThreat() {
        return enemies.stream()
                .mapToDouble(this::calculateIndividualApThreat)
                .sum();
    }

    public double getEnemyAdRatio() {
        double totalAd = getTotalEnemyAdThreat();
        double totalAp = getTotalEnemyApThreat();
        double totalThreat = totalAd + totalAp;

        if(totalThreat == 0) {
            return 0.5;
        }
        return totalAd / totalThreat;
    }

    private double calculateIndividualAdThreat(Player player) {
        double baseAd = isEstimatedMage(player) ? 0 : player.stats().attackDamage();
        double kdaMultiplier = Math.max(0.5, getKdaMultiplier(player));
        double goldMultiplier = 1.0 + (player.totalGold() / 8000.0);

        return baseAd * kdaMultiplier * goldMultiplier;
    }

    private double calculateIndividualApThreat(Player player) {
        // Data Dragon has no base AP, so an estimated mage's damage would otherwise read as pure AD.
        double baseAp = player.stats().abilityPower() + (isEstimatedMage(player) ? player.stats().attackDamage() : 0);
        double kdaMultiplier = Math.max(0.5, getKdaMultiplier(player));
        double goldMultiplier = 1.0 + (player.totalGold() / 8000.0);

        return baseAp * kdaMultiplier * goldMultiplier;
    }

    private static boolean isEstimatedMage(Player player) {
        return player.isEstimated() && player.championClass() == MAGE;
    }

    private double getKdaMultiplier(Player player) {
        return 1.0 + (player.kda().kills() * 0.12) + (player.kda().assists() * 0.04) - (player.kda().deaths() * 0.06);
    }

    public int getHighestHpEnemy() {
        return enemies.stream()
                .mapToInt(player -> player.stats().maxHealth())
                .max()
                .orElse(0);
    }

    public boolean isActivePlayerSquishy() {
        int squishyBools = 0;
        if(List.of(MARKSMAN, MAGE, ASSASSIN, SUPPORT).contains(activePlayer.championClass())) {
            squishyBools++;
        }

        if(activePlayer.stats().maxHealth() < 2500) {
            squishyBools++;
        }

        if(activePlayer.stats().armor() < 100) {
            squishyBools++;
        }
        if(activePlayer.stats().magicResist() < 100) {
            squishyBools++;
        }

        if(activePlayer.items().stream()
                .noneMatch(item -> item.tags().contains(ItemTag.BURST_RESISTANCE) || item.tags().contains(ItemTag.SPELL_SHIELD))) {
            squishyBools++;
        }

        return squishyBools >= 3;
    }
}
