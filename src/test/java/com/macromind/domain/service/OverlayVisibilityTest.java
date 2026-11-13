package com.macromind.domain.service;

import com.macromind.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class OverlayVisibilityTest {

    private static final Item ITEM = item("Thing", NO_STATS);

    private static BuildAdvice advice(AdvicePriority priority) {
        return new BuildAdvice(ITEM, "why", priority, AdviceCategory.DEFENSIVE, null, Confidence.ESTIMATED, 0);
    }

    private static GameState state(boolean dead, long time) {
        Player me = player("Me", ChampionClass.MAGE, false, List.of(), AVERAGE_STATS);
        Player active = new Player(me.name(), me.championName(), me.championClass(), 1, false, List.of(),
                me.stats(), me.role(), me.kda(), 0, false, dead);
        return new GameState(time, active, List.of(), List.of(), GameMode.SUMMONERS_RIFT);
    }

    @Test
    void showsWhenDeadAndAdviceExists() {
        assertTrue(OverlayVisibility.shouldShow(state(true, 100), List.of(advice(AdvicePriority.RECOMMENDED)), Map.of()));
    }

    @Test
    void hidesWhenNoAdviceEvenIfDead() {
        assertFalse(OverlayVisibility.shouldShow(state(true, 100), List.of(), Map.of()));
    }

    @Test
    void hidesWhenAliveAndOnlyRecommendedAdvice() {
        assertFalse(OverlayVisibility.shouldShow(state(false, 100), List.of(advice(AdvicePriority.RECOMMENDED)), Map.of()));
    }

    @Test
    void showsCriticalAdviceOnlyWithinWindowAfterFirstSeen() {
        List<BuildAdvice> advice = List.of(advice(AdvicePriority.CRITICAL));
        Map<Integer, Long> seen = OverlayVisibility.track(Map.of(), advice, 100);

        assertTrue(OverlayVisibility.shouldShow(state(false, 110), advice, seen));
        assertFalse(OverlayVisibility.shouldShow(state(false, 130), advice, seen));
    }

    @Test
    void trackKeepsOriginalFirstSeenTime() {
        List<BuildAdvice> advice = List.of(advice(AdvicePriority.CRITICAL));
        Map<Integer, Long> first = OverlayVisibility.track(Map.of(), advice, 100);

        assertEquals(100L, OverlayVisibility.track(first, advice, 150).get(ITEM.id()));
    }

    @Test
    void trackStartsFreshWhenGameClockGoesBackwards() {
        List<BuildAdvice> advice = List.of(advice(AdvicePriority.CRITICAL));
        Map<Integer, Long> oldGame = OverlayVisibility.track(Map.of(), advice, 900);

        assertEquals(5L, OverlayVisibility.track(oldGame, advice, 5).get(ITEM.id()));
    }
}
