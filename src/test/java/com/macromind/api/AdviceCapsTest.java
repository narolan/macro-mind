package com.macromind.api;

import com.macromind.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AdviceCapsTest {

    private static BuildAdvice advice(String item, String reason, AdvicePriority priority) {
        return new BuildAdvice(new Item(item.hashCode(), item, 1, ItemStats.NONE, Set.of()), reason, priority,
                AdviceCategory.DEFENSIVE, null, Confidence.EXACT, 0);
    }

    private static List<String> names(List<BuildAdvice> advice) {
        return advice.stream().map(a -> a.recommendedItem().name()).toList();
    }

    @Test
    void emptyStaysEmpty() {
        assertEquals(List.of(), AdviceCaps.forOverlay(List.of()));
    }

    @Test
    void sortsMostUrgentFirstAndKeepsTheOriginalOrderWithinAPriority() {
        List<BuildAdvice> in = List.of(
                advice("s1", "a", AdvicePriority.SITUATIONAL), advice("c1", "b", AdvicePriority.CRITICAL),
                advice("r1", "c", AdvicePriority.RECOMMENDED), advice("c2", "d", AdvicePriority.CRITICAL));

        assertEquals(List.of("c1", "c2", "r1", "s1"), names(AdviceCaps.forOverlay(in)));
    }

    @Test
    void keepsAtMostFourEntriesPerPriorityAndReason() {
        List<BuildAdvice> in = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            in.add(advice("heal" + i, "heavy healing", AdvicePriority.CRITICAL));
        }

        assertEquals(List.of("heal0", "heal1", "heal2", "heal3"), names(AdviceCaps.forOverlay(in)));
    }

    @Test
    void theSameReasonAtADifferentPriorityIsCountedSeparately() {
        List<BuildAdvice> in = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            in.add(advice("c" + i, "same", AdvicePriority.CRITICAL));
            in.add(advice("r" + i, "same", AdvicePriority.RECOMMENDED));
        }

        assertEquals(8, AdviceCaps.forOverlay(in).size());
    }

    @Test
    void neverReturnsMoreThanTwelveEntries() {
        List<BuildAdvice> in = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            in.add(advice("item" + i, "reason " + i, AdvicePriority.RECOMMENDED));
        }

        assertEquals(12, AdviceCaps.forOverlay(in).size());
    }

    @Test
    void aChattyRuleCannotCrowdOutALaterReason() {
        List<BuildAdvice> in = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            in.add(advice("heal" + i, "heavy healing", AdvicePriority.CRITICAL));
        }
        in.add(advice("boots", "needs boots", AdvicePriority.CRITICAL));

        assertTrue(names(AdviceCaps.forOverlay(in)).contains("boots"));
    }

    @Test
    void doesNotModifyItsInput() {
        List<BuildAdvice> in = new ArrayList<>(List.of(advice("b", "x", AdvicePriority.SITUATIONAL), advice("a", "y", AdvicePriority.CRITICAL)));
        List<BuildAdvice> copy = List.copyOf(in);

        AdviceCaps.forOverlay(in);

        assertEquals(copy, in);
    }
}
