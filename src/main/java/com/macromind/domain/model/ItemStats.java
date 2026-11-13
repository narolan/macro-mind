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

    public static final ItemStats NONE = new ItemStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    public ItemStats(int attackDamage, int abilityPower, int armor, int magicResist, int health, int mana,
                     int flatArmorPen, int flatMagicPen, double attackSpeed, double cooldownReduction,
                     double lifesteal, double criticalStrikeChance) {
        this(attackDamage, abilityPower, armor, magicResist, health, mana, flatArmorPen, flatMagicPen,
                attackSpeed, cooldownReduction, lifesteal, criticalStrikeChance, 0);
    }

    public ItemStats withArmor(int newArmor) {
        return new ItemStats(attackDamage, abilityPower, newArmor, magicResist, health, mana, flatArmorPen, flatMagicPen,
                attackSpeed, cooldownReduction, lifesteal, criticalStrikeChance, omnivamp);
    }

    public ItemStats withMagicResist(int newMagicResist) {
        return new ItemStats(attackDamage, abilityPower, armor, newMagicResist, health, mana, flatArmorPen, flatMagicPen,
                attackSpeed, cooldownReduction, lifesteal, criticalStrikeChance, omnivamp);
    }

    public ItemStats withFlatArmorPen(int newFlatArmorPen) {
        return new ItemStats(attackDamage, abilityPower, armor, magicResist, health, mana, newFlatArmorPen, flatMagicPen,
                attackSpeed, cooldownReduction, lifesteal, criticalStrikeChance, omnivamp);
    }
}
