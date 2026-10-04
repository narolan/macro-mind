package com.macromind.domain.model;

public record BuildAdvice(
        Item recommendedItem,
        String reasoning,
        AdvicePriority priority,
        AdviceCategory category,
        Player targetEnemy,
        long timestamp
) {
}

