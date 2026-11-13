package com.macromind.domain.model;

public record ChampionBaseStats(
        double hp, double hpPerLevel,
        double armor, double armorPerLevel,
        double magicResist, double magicResistPerLevel,
        double attackDamage, double attackDamagePerLevel
) {
}
