package com.macromind.domain.service;

import com.macromind.domain.model.ChampionBaseStats;
import com.macromind.domain.model.Item;
import com.macromind.domain.model.PlayerStats;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Estimates enemy stats: Data Dragon base stats scaled by level, plus visible items. No runes or buffs. */
public class StatEstimator {

    private static final ChampionBaseStats UNKNOWN = new ChampionBaseStats(0, 0, 0, 0, 0, 0, 0, 0);

    private final Function<String, Optional<ChampionBaseStats>> baseStats;

    public StatEstimator(Function<String, Optional<ChampionBaseStats>> baseStats) {
        this.baseStats = baseStats;
    }

    public PlayerStats estimate(String championName, int level, List<Item> items) {
        ChampionBaseStats base = baseStats.apply(championName).orElse(UNKNOWN);
        double growth = growthFactor(level);
        int itemArmor = items.stream().mapToInt(i -> i.stats().armor()).sum();
        int itemMr = items.stream().mapToInt(i -> i.stats().magicResist()).sum();
        int itemHealth = items.stream().mapToInt(i -> i.stats().health()).sum();
        int itemAd = items.stream().mapToInt(i -> i.stats().attackDamage()).sum();
        int itemAp = items.stream().mapToInt(i -> i.stats().abilityPower()).sum();
        int lifesteal = (int) Math.round(items.stream().mapToDouble(i -> i.stats().lifesteal()).sum());
        int maxHealth = (int) Math.round(base.hp() + base.hpPerLevel() * growth) + itemHealth;
        return new PlayerStats(
                (int) Math.round(base.armor() + base.armorPerLevel() * growth) + itemArmor,
                (int) Math.round(base.magicResist() + base.magicResistPerLevel() * growth) + itemMr,
                maxHealth,
                maxHealth,
                (int) Math.round(base.attackDamage() + base.attackDamagePerLevel() * growth) + itemAd,
                itemAp,
                lifesteal,
                0,
                0);
    }

    private static double growthFactor(int level) {
        int n = Math.max(level, 1) - 1;
        return n * (0.7025 + 0.0175 * n);
    }
}
