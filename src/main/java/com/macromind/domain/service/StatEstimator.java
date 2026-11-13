package com.macromind.domain.service;

import com.macromind.domain.model.ChampionBaseStats;
import com.macromind.domain.model.Item;
import com.macromind.domain.model.ItemStats;
import com.macromind.domain.model.PlayerStats;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/** Estimates enemy stats: Data Dragon base stats scaled by level, plus visible items. No runes or buffs. */
public final class StatEstimator {

    // Riot's per-level stat growth: base + perLevel * (n) * (0.7025 + 0.0175 * n), where n = level - 1.
    private static final double GROWTH_BASE = 0.7025;
    private static final double GROWTH_STEP = 0.0175;
    private static final ChampionBaseStats UNKNOWN = new ChampionBaseStats(0, 0, 0, 0, 0, 0, 0, 0);

    private final Function<String, Optional<ChampionBaseStats>> baseStats;

    public StatEstimator(Function<String, Optional<ChampionBaseStats>> baseStats) {
        this.baseStats = baseStats;
    }

    public PlayerStats estimate(String championName, int level, List<Item> items) {
        ChampionBaseStats base = baseStats.apply(championName).orElse(UNKNOWN);
        double growth = growthFactor(level);
        int maxHealth = scaled(base.hp(), base.hpPerLevel(), growth) + sum(items, ItemStats::health);
        return new PlayerStats(
                scaled(base.armor(), base.armorPerLevel(), growth) + sum(items, ItemStats::armor),
                scaled(base.magicResist(), base.magicResistPerLevel(), growth) + sum(items, ItemStats::magicResist),
                maxHealth,
                maxHealth,
                scaled(base.attackDamage(), base.attackDamagePerLevel(), growth) + sum(items, ItemStats::attackDamage),
                sum(items, ItemStats::abilityPower),
                (int) Math.round(items.stream().mapToDouble(i -> i.stats().lifesteal()).sum()),
                sum(items, ItemStats::omnivamp),
                0);
    }

    private static int scaled(double base, double perLevel, double growth) {
        return (int) Math.round(base + perLevel * growth);
    }

    private static int sum(List<Item> items, ToIntFunction<ItemStats> stat) {
        return items.stream().mapToInt(item -> stat.applyAsInt(item.stats())).sum();
    }

    private static double growthFactor(int level) {
        int n = Math.max(level, 1) - 1;
        return n * (GROWTH_BASE + GROWTH_STEP * n);
    }
}
