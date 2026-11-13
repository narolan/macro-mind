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
        int healthRegen,
        int flatArmorPen,
        int flatMagicPen
) {
    public PlayerStats(int armor, int magicResist, int maxHealth, int currentHealth, int attackDamage,
                       int abilityPower, int lifesteal, int omnivamp, int healthRegen) {
        this(armor, magicResist, maxHealth, currentHealth, attackDamage, abilityPower, lifesteal, omnivamp,
                healthRegen, 0, 0);
    }
}
