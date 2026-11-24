package com.macromind.domain.model;

import java.util.Set;

public record Item(
        int id,
        String name,
        int goldValue,
        ItemStats stats,
        Set<ItemTag> tags,
        Set<Integer> maps
        ) {

    public Item {
        tags = Set.copyOf(tags);
        maps = Set.copyOf(maps);
    }

    /** An item with no map restriction (available in every mode). */
    public Item(int id, String name, int goldValue, ItemStats stats, Set<ItemTag> tags) {
        this(id, name, goldValue, stats, tags, Set.of());
    }

    /** True when the shop of this mode sells the item; an item with no map data is assumed available. */
    public boolean isAvailableIn(GameMode mode) {
        return maps.isEmpty() || mode.mapIds().stream().anyMatch(maps::contains);
    }

    public boolean hasTag(ItemTag tag) {
        return tags.contains(tag);
    }

    public boolean hasAnyTag(ItemTag... candidates) {
        for (ItemTag candidate : candidates) {
            if (tags.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}
