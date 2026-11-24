package com.macromind.domain.model;

import java.util.List;

public record Player(
        String name,
        String championName,
        ChampionClass championClass,
        int level,
        boolean isEnemy,
        List<Item> items,
        PlayerStats stats,
        Role role,
        KDA kda,
        int totalGold,
        boolean isEstimated,
        boolean isDead
) {

    private static final int HEAVY_REGEN_PER_SECOND = 100;
    private static final int HEAVY_SUSTAIN_PERCENT = 15;

    public Player {
        items = List.copyOf(items);
    }

    public Player withGold(int gold) {
        return new Player(name, championName, championClass, level, isEnemy, items, stats, role, kda, gold, isEstimated, isDead);
    }

    public Player withKda(KDA newKda) {
        return new Player(name, championName, championClass, level, isEnemy, items, stats, role, newKda, totalGold, isEstimated, isDead);
    }

    public Player asEstimated() {
        return new Player(name, championName, championClass, level, isEnemy, items, stats, role, kda, totalGold, true, isDead);
    }

    public Player withDead(boolean dead) {
        return new Player(name, championName, championClass, level, isEnemy, items, stats, role, kda, totalGold, isEstimated, dead);
    }

    public boolean hasGrievousWoundsItem() {
        return items.stream().anyMatch(item -> item.hasTag(ItemTag.GRIEVOUS_WOUNDS));
    }

    public int totalFlatArmorPen() {
        return stats.flatArmorPen() + items.stream()
                .mapToInt(item -> item.stats().flatArmorPen())
                .sum();
    }

    public int totalFlatMagicPen() {
        return stats.flatMagicPen() + items.stream()
                .mapToInt(item -> item.stats().flatMagicPen())
                .sum();
    }

    /** Mages deal magic damage; marksmen physical; hybrids follow their stronger offensive stat. */
    public boolean dealsMagicDamage() {
        return switch (championClass) {
            case MAGE -> true;
            case ASSASSIN, BRUISER -> stats.abilityPower() > stats.attackDamage();
            case MARKSMAN, TANK, SUPPORT, UNKNOWN -> false;
        };
    }

    public boolean dealsPhysicalDamage() {
        return switch (championClass) {
            case MARKSMAN -> true;
            case ASSASSIN, BRUISER -> stats.attackDamage() >= stats.abilityPower();
            case MAGE, TANK, SUPPORT, UNKNOWN -> false;
        };
    }

    /** Heavy sustain: strong health regen, or lifesteal plus omnivamp of at least 15%. */
    public boolean hasHighSustainStats() {
        return stats.healthRegen() > HEAVY_REGEN_PER_SECOND
                || stats.lifesteal() + stats.omnivamp() >= HEAVY_SUSTAIN_PERCENT;
    }

    public boolean hasBoots() {
        return items.stream().anyMatch(item -> item.hasTag(ItemTag.BOOTS));
    }

}
