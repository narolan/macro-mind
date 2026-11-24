package com.macromind.app;

import com.macromind.domain.model.GameState;
import com.macromind.domain.model.ObjectiveTimer;

import java.util.List;

/** Turns the game's objective events into countdowns; called only from the single polling thread. */
final class ObjectiveTracker {

    static final long TIMER_WINDOW_SECONDS = 30;

    /** Countdowns for objectives due within the window; empty while nothing fills GameState.objectiveEvents. */
    List<ObjectiveTimer> timers(GameState state) {
        return state.objectiveEvents().stream()
                .map(event -> new ObjectiveTimer(event.name(), event.atSeconds() - state.gameTimeInSeconds()))
                .filter(timer -> timer.secondsLeft() > 0 && timer.secondsLeft() <= TIMER_WINDOW_SECONDS)
                .toList();
    }
}
