package com.macromind.domain.model;

import java.util.Set;

public record Item(
        int id,
        String name,
        int goldValue,
        ItemStats stats,
        Set<ItemTag> tags
        ) {

    public Item {
        tags = Set.copyOf(tags);
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
