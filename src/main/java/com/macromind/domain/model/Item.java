package com.macromind.domain.model;

import java.util.Set;

public record Item(
        int id,
        String name,
        int goldValue,
        ItemStats stats,
        Set<ItemTag> tags
        ) {
}

