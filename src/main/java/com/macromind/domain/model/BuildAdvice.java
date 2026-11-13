package com.macromind.domain.model;

public record BuildAdvice(
        Item recommendedItem,
        String reasoning,
        AdvicePriority priority,
        AdviceCategory category,
        Player targetEnemy,
        Confidence confidence,
        long timestamp
) {
}

