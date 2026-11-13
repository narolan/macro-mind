package com.macromind.domain.service;

import com.macromind.domain.model.AdviceCategory;
import com.macromind.domain.model.AdvicePriority;
import com.macromind.domain.model.Item;

import java.util.function.Predicate;

record AdviceRule(
        Predicate<Item> itemMatcher,
        String reasoning,
        AdvicePriority priority,
        AdviceCategory category
) {
}
