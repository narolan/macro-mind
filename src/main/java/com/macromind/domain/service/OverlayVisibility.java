package com.macromind.domain.service;

import com.macromind.domain.model.AdvicePriority;
import com.macromind.domain.model.BuildAdvice;
import com.macromind.domain.model.GameState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Show advice when the active player is dead (the Live Client API exposes no "in base" flag, so death is the
 * proxy for a buy moment), or for a short window after a CRITICAL advice first appears.
 */
public final class OverlayVisibility {

    static final long CRITICAL_WINDOW_SECONDS = 20;

    private OverlayVisibility() {}

    public static boolean shouldShow(GameState state, List<BuildAdvice> advice, Map<Integer, Long> firstSeenCritical) {
        if (advice.isEmpty()) {
            return false;
        }
        if (state.activePlayer().isDead()) {
            return true;
        }
        return advice.stream()
                .filter(a -> a.priority() == AdvicePriority.CRITICAL)
                .map(a -> firstSeenCritical.get(a.recommendedItem().id()))
                .anyMatch(first -> first != null && state.gameTimeInSeconds() - first <= CRITICAL_WINDOW_SECONDS);
    }

    /** Returns a new map recording when each CRITICAL item was first seen; earlier timestamps are kept. */
    public static Map<Integer, Long> track(Map<Integer, Long> firstSeen, List<BuildAdvice> advice, long gameTime) {
        // A game clock earlier than a stored timestamp means a new game started: forget the old one.
        boolean newGame = firstSeen.values().stream().anyMatch(first -> first > gameTime);
        Map<Integer, Long> updated = new HashMap<>(newGame ? Map.of() : firstSeen);
        advice.stream()
                .filter(a -> a.priority() == AdvicePriority.CRITICAL)
                .forEach(a -> updated.putIfAbsent(a.recommendedItem().id(), gameTime));
        return Map.copyOf(updated);
    }
}
