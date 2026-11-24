package com.macromind.domain.service;

import com.macromind.domain.model.AdvicePriority;
import com.macromind.domain.model.Alert;
import com.macromind.domain.model.BuildAdvice;
import com.macromind.domain.model.GameState;
import com.macromind.domain.model.ObjectiveTimer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Show advice when the active player is dead (the Live Client API exposes no "in base" flag, so death is the
 * proxy for a buy moment), or for a short window after a CRITICAL advice first appears.
 */
public final class OverlayVisibility {

    static final long CRITICAL_WINDOW_SECONDS = 20;
    static final long ALERT_WINDOW_SECONDS = 20;

    private OverlayVisibility() {}

    public static boolean shouldShow(GameState state, List<BuildAdvice> advice, Map<Integer, Long> firstSeenCritical) {
        return shouldShow(state, advice, firstSeenCritical, List.of(), List.of());
    }

    /** As above, but fresh alerts or running objective timers are also a reason to show, even with no advice. */
    public static boolean shouldShow(GameState state, List<BuildAdvice> advice, Map<Integer, Long> firstSeenCritical,
                                     List<Alert> freshAlerts, List<ObjectiveTimer> timers) {
        if (!freshAlerts.isEmpty() || !timers.isEmpty()) {
            return true;
        }
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

    /** Like {@link #track}, for alerts: when each alert key was first seen, with the same new-game reset. */
    public static Map<String, Long> trackAlerts(Map<String, Long> firstSeen, List<Alert> alerts, long gameTime) {
        boolean newGame = firstSeen.values().stream().anyMatch(first -> first > gameTime);
        Map<String, Long> updated = new HashMap<>(newGame ? Map.of() : firstSeen);
        alerts.forEach(a -> updated.putIfAbsent(a.key(), gameTime));
        return Map.copyOf(updated);
    }

    /** The alerts to show now: all of them while the player is dead, otherwise only those first seen within the window. */
    public static List<Alert> freshAlerts(GameState state, List<Alert> alerts, Map<String, Long> firstSeen) {
        if (state.activePlayer().isDead()) {
            return alerts;
        }
        return alerts.stream()
                .filter(a -> state.gameTimeInSeconds() - firstSeen.getOrDefault(a.key(), Long.MIN_VALUE / 2) <= ALERT_WINDOW_SECONDS)
                .toList();
    }
}
