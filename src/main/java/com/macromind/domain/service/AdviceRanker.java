package com.macromind.domain.service;

import com.macromind.domain.model.BuildAdvice;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Merges advice for the same item and orders it by what the player should look at first. */
final class AdviceRanker {

    private AdviceRanker() {}

    private record Ranked(BuildAdvice advice, int rulesWanting) {}

    /**
     * One entry per item (its most urgent advice). Order: priority, then how many rules want the item, then items the
     * player can already afford (most expensive first), then the rest (cheapest, i.e. closest, first).
     */
    static List<BuildAdvice> rank(List<BuildAdvice> advices, int gold) {
        Map<String, List<BuildAdvice>> byItem = advices.stream().collect(
                Collectors.groupingBy(a -> a.recommendedItem().name(), LinkedHashMap::new, Collectors.toList()));
        Comparator<Ranked> order = Comparator
                .comparing((Ranked r) -> r.advice().priority())
                .thenComparing(Comparator.comparingInt(Ranked::rulesWanting).reversed())
                .thenComparing(r -> r.advice().recommendedItem().goldValue() > gold)
                .thenComparingInt(r -> price(r, gold));
        return byItem.values().stream()
                .map(group -> new Ranked(group.stream().min(Comparator.comparing(BuildAdvice::priority)).orElseThrow(), group.size()))
                .sorted(order)
                .map(Ranked::advice)
                .toList();
    }

    private static int price(Ranked ranked, int gold) {
        int price = ranked.advice().recommendedItem().goldValue();
        return price <= gold ? -price : price;
    }
}
