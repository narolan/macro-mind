package com.macromind.app;

import com.macromind.domain.model.GameState;
import com.macromind.domain.model.ObjectiveTimer;
import com.macromind.domain.service.ObjectiveSchedule;

import java.util.List;

/** Turns objective kills into countdowns; derived from the state alone, so a new game (fewer kills, earlier clock) resets itself. */
final class ObjectiveTracker {

    static final long TIMER_WINDOW_SECONDS = 30;

    /** Countdowns for objectives due within the window; GameState.objectiveEvents holds the kills so far. */
    List<ObjectiveTimer> timers(GameState state) {
        return ObjectiveSchedule.nextSpawns(state.objectiveEvents()).stream()
                .map(event -> new ObjectiveTimer(event.name(), event.atSeconds() - state.gameTimeInSeconds()))
                .filter(timer -> timer.secondsLeft() > 0 && timer.secondsLeft() <= TIMER_WINDOW_SECONDS)
                .toList();
    }
}
