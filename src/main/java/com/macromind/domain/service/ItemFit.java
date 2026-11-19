package com.macromind.domain.service;

import com.macromind.domain.model.Item;
import com.macromind.domain.model.ItemStats;
import com.macromind.domain.model.ItemTag;
import com.macromind.domain.model.Player;

/** Whether an item is worth advising to a given player at all, regardless of which rule wants it. */
final class ItemFit {

    private static final int TANK_ITEM_MIN_HEALTH = 300;

    private ItemFit() {}

    /** Starters and plain components are never advised; anti-heal components, cleanses and boots are. */
    static boolean isAdviseable(Item item) {
        if (item.hasTag(ItemTag.STARTER)) {
            return false;
        }
        if (!item.hasTag(ItemTag.COMPONENT)) {
            return true;
        }
        return item.hasAnyTag(ItemTag.GRIEVOUS_WOUNDS, ItemTag.BOOTS, ItemTag.CLEANSE);
    }

    /** Attack damage, attack speed or crit chance: stats a physical damage dealer wants. */
    static boolean givesPhysicalDamage(Item item) {
        return item.stats().attackDamage() > 0 || item.stats().attackSpeed() > 0 || item.stats().criticalStrikeChance() > 0;
    }

    static boolean givesAbilityPower(Item item) {
        return item.stats().abilityPower() > 0;
    }

    /** Durable items whose damage does not outweigh their resistances (so a damage item with some health is not one). */
    static boolean isTankItem(Item item) {
        ItemStats s = item.stats();
        boolean durable = s.health() >= TANK_ITEM_MIN_HEALTH || s.armor() > 0 || s.magicResist() > 0;
        return durable && s.attackDamage() + s.abilityPower() <= s.armor() + s.magicResist();
    }

    /** True for an item with no armor or magic resist at all, such as a pure health item: it helps against any damage. */
    static boolean hasNoResist(Item item) {
        return item.stats().armor() + item.stats().magicResist() == 0;
    }

    /** An item that only gives AD is useless to a mage, and vice versa. Neutral (defensive/utility) items fit anyone. */
    static boolean fitsDamageType(Item item, Player player) {
        boolean givesAd = givesPhysicalDamage(item);
        boolean givesAp = givesAbilityPower(item);
        if (givesAd && !givesAp) {
            return player.dealsPhysicalDamage() || !player.dealsMagicDamage();
        }
        if (givesAp && !givesAd) {
            return player.dealsMagicDamage() || !player.dealsPhysicalDamage();
        }
        return true;
    }
}
