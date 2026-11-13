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

    public boolean hasGrievousWoundsItem() {
        return items().stream().anyMatch(item -> item.tags().contains(ItemTag.GRIEVOUS_WOUNDS));
    }

    public int getFlatArmorPen() {
        return stats().flatArmorPen() + items().stream()
                .mapToInt(item -> item.stats().flatArmorPen())
                .sum();
    }

    public int getFlatMagicPen() {
        return stats().flatMagicPen() + items().stream()
                .mapToInt(item -> item.stats().flatMagicPen())
                .sum();
    }

    /** Mages deal magic damage; marksmen physical; hybrids follow their stronger offensive stat. */
    public boolean dealsMagicDamage() {
        return switch (championClass()) {
            case MAGE -> true;
            case ASSASSIN, BRUISER -> stats().abilityPower() > stats().attackDamage();
            case MARKSMAN, TANK, SUPPORT -> false;
        };
    }

    public boolean dealsPhysicalDamage() {
        return switch (championClass()) {
            case MARKSMAN -> true;
            case ASSASSIN, BRUISER -> stats().attackDamage() >= stats().abilityPower();
            case MAGE, TANK, SUPPORT -> false;
        };
    }

    public boolean hasBoots() {
        return items().stream().anyMatch(item -> item.tags().contains(ItemTag.BOOTS));
    }

    public boolean hasArmorPenPercentage() {
        return items().stream().anyMatch(item -> item.tags().contains(ItemTag.ARMOR_PENETRATION_PERCENT));
    }

}
