package com.macromind.domain.model;

public record PlayerStats(
        int armor,
        int magicResist,
        int maxHealth,
        int currentHealth,
        int attackDamage,
        int abilityPower,
        int lifesteal,
        int omnivamp,
        int healthRegen
) {
}
