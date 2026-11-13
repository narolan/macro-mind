package com.macromind.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ItemTagsTest {

    private static final Item ITEM = new Item(1, "Thing", 1000, ItemStats.NONE, Set.of(ItemTag.BOOTS, ItemTag.TENACITY));

    @Test
    void hasTagIsTrueOnlyForTagsTheItemCarries() {
        assertTrue(ITEM.hasTag(ItemTag.BOOTS));
        assertFalse(ITEM.hasTag(ItemTag.CLEANSE));
    }

    @Test
    void hasAnyTagIsTrueWhenAtLeastOneTagMatches() {
        assertTrue(ITEM.hasAnyTag(ItemTag.CLEANSE, ItemTag.TENACITY));
        assertFalse(ITEM.hasAnyTag(ItemTag.CLEANSE, ItemTag.STARTER));
    }

    @Test
    void hasAnyTagWithNoTagsIsFalse() {
        assertFalse(ITEM.hasAnyTag());
    }
}
