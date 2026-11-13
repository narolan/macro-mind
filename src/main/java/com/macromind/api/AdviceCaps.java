package com.macromind.api;

import com.macromind.domain.model.BuildAdvice;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Limits what is sent to the overlay. Rules can match most of the catalogue, so each priority-and-reason group is
 * capped (one chatty rule cannot crowd out the others), and the overlay merges entries that share a reason into a card.
 */
final class AdviceCaps {

    static final int MAX_ADVICE = 12;
    static final int MAX_PER_REASON = 4;

    private AdviceCaps() {}

    /** Most urgent first (a stable sort, so ranker order is kept within a priority), then capped. */
    static List<BuildAdvice> forOverlay(List<BuildAdvice> advice) {
        List<BuildAdvice> byUrgency = advice.stream().sorted(Comparator.comparing(BuildAdvice::priority)).toList();
        Map<String, Integer> keptPerReason = new HashMap<>();
        List<BuildAdvice> kept = new ArrayList<>();
        for (BuildAdvice candidate : byUrgency) {
            if (kept.size() == MAX_ADVICE) {
                break;
            }
            String group = candidate.priority() + "|" + candidate.reasoning();
            int count = keptPerReason.merge(group, 1, Integer::sum);
            if (count <= MAX_PER_REASON) {
                kept.add(candidate);
            }
        }
        return List.copyOf(kept);
    }
}
