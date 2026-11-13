package com.macromind.domain.model;

public record ItemStats(
    int attackDamage,
    int abilityPower,
    int armor,
    int magicResist,
    int health,
    int mana,
    int flatArmorPen,
    int flatMagicPen,
    double attackSpeed,
    double cooldownReduction,
    double lifesteal,
    double criticalStrikeChance,
    int omnivamp
) {
    public ItemStats(int attackDamage, int abilityPower, int armor, int magicResist, int health, int mana,
                     int flatArmorPen, int flatMagicPen, double attackSpeed, double cooldownReduction,
                     double lifesteal, double criticalStrikeChance) {
        this(attackDamage, abilityPower, armor, magicResist, health, mana, flatArmorPen, flatMagicPen,
                attackSpeed, cooldownReduction, lifesteal, criticalStrikeChance, 0);
    }
}
