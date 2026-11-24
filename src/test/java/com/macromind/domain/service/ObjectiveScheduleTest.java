package com.macromind.domain.service;

import com.macromind.domain.model.ObjectiveEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObjectiveScheduleTest {

    private static ObjectiveEvent kill(String name, long at) {
        return new ObjectiveEvent(name, at);
    }

    @Test
    void schedulesFirstSpawnsWhenNothingWasKilled() {
        assertEquals(List.of(new ObjectiveEvent("Dragon", 300), new ObjectiveEvent("Baron", 1200),
                new ObjectiveEvent("Rift Herald", 900)), ObjectiveSchedule.nextSpawns(List.of()));
    }

    @Test
    void respawnsFromTheMostRecentKillOfEachType() {
        List<ObjectiveEvent> kills = List.of(kill("Dragon", 400), kill("Dragon", 800), kill("Baron", 1500),
                kill("Rift Herald", 950));

        assertEquals(List.of(new ObjectiveEvent("Dragon", 1100), new ObjectiveEvent("Baron", 1860)),
                ObjectiveSchedule.nextSpawns(kills));
    }

    @Test
    void ignoresUnknownObjectivesAndNeverRespawnsTheHerald() {
        assertEquals(List.of(new ObjectiveEvent("Dragon", 300), new ObjectiveEvent("Baron", 1200)),
                ObjectiveSchedule.nextSpawns(List.of(kill("Rift Herald", 900), kill("Voidgrub", 500))));
    }

    @Test
    void elderDragonRespawnsAfterSixMinutesUnderTheDragonName() {
        assertEquals(new ObjectiveEvent("Dragon", 2000 + 360),
                ObjectiveSchedule.nextSpawns(List.of(kill("Elder Dragon", 2000))).get(0));
    }

    @Test
    void theLatestKillWinsEvenWhenItIsAnEarlierDragonType() {
        assertEquals(new ObjectiveEvent("Dragon", 2300 + 300),
                ObjectiveSchedule.nextSpawns(List.of(kill("Elder Dragon", 2000), kill("Dragon", 2300))).get(0));
    }
}
