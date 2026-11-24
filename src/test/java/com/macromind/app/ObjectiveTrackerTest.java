package com.macromind.app;

import com.macromind.domain.model.ChampionClass;
import com.macromind.domain.model.GameMode;
import com.macromind.domain.model.GameState;
import com.macromind.domain.model.ObjectiveEvent;
import com.macromind.domain.model.ObjectiveTimer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ObjectiveTrackerTest {

    private final ObjectiveTracker tracker = new ObjectiveTracker();

    private static GameState at(long seconds, ObjectiveEvent... kills) {
        return new GameState(seconds, player("Me", ChampionClass.MARKSMAN, false, List.of(), AVERAGE_STATS),
                List.of(), List.of(), GameMode.SUMMONERS_RIFT, List.of(kills));
    }

    @Test
    void showsTheFirstDragonOnlyWithinThirtySeconds() {
        assertEquals(List.of(), tracker.timers(at(269)));
        assertEquals(List.of(new ObjectiveTimer("Dragon", 30)), tracker.timers(at(270)));
        assertEquals(List.of(new ObjectiveTimer("Dragon", 1)), tracker.timers(at(299)));
        assertEquals(List.of(), tracker.timers(at(300)));
    }

    @Test
    void countsHeraldAndBaronFromTheirFirstSpawn() {
        ObjectiveEvent dragon = new ObjectiveEvent("Dragon", 100);
        assertEquals(List.of(new ObjectiveTimer("Rift Herald", 30)), tracker.timers(at(870, dragon)));
        assertEquals(List.of(new ObjectiveTimer("Baron", 30)), tracker.timers(at(1170, dragon)));
    }

    @Test
    void anObjectiveKilledForTheFirstTimeRespawnsFromTheKill() {
        assertEquals(List.of(new ObjectiveTimer("Baron", 30)), tracker.timers(at(1830, new ObjectiveEvent("Baron", 1500))));
        assertEquals(List.of(), tracker.timers(at(1860, new ObjectiveEvent("Baron", 1500))));
    }

    @Test
    void usesTheMostRecentKillForTheRespawn() {
        GameState state = at(1070, new ObjectiveEvent("Dragon", 400), new ObjectiveEvent("Dragon", 800));
        assertEquals(List.of(new ObjectiveTimer("Dragon", 30)), tracker.timers(state));
    }

    @Test
    void forgetsEverythingWhenANewGameStartsAndTheClockGoesBackwards() {
        tracker.timers(at(1070, new ObjectiveEvent("Dragon", 800)));
        assertEquals(List.of(new ObjectiveTimer("Dragon", 30)), tracker.timers(at(270)));
    }
}
