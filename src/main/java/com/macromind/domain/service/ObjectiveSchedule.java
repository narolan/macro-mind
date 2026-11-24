package com.macromind.domain.service;

import com.macromind.domain.model.ObjectiveEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * When each objective is next up, from the kills so far.
 *
 * <p>Verified 2026-11-24 against the League wiki (patch V26.12):
 * https://wiki.leagueoflegends.com/en-us/Baron_Nashor (first spawn 20:00, respawn 6:00),
 * https://wiki.leagueoflegends.com/en-us/Dragon (first spawn 5:00, respawn 5:00, Elder Dragon respawn 6:00),
 * https://wiki.leagueoflegends.com/en-us/Rift_Herald (spawns once at 15:00, no respawn, despawns 19:45).
 * Not modelled: the herald's despawn (it is only shown before 15:00 anyway).
 */
public final class ObjectiveSchedule {

    public static final String DRAGON = "Dragon";
    public static final String ELDER_DRAGON = "Elder Dragon";
    public static final String BARON = "Baron";
    public static final String HERALD = "Rift Herald";

    static final long DRAGON_FIRST_SPAWN = 5 * 60;
    static final long DRAGON_RESPAWN = 5 * 60;
    static final long ELDER_RESPAWN = 6 * 60;
    static final long BARON_FIRST_SPAWN = 20 * 60;
    static final long BARON_RESPAWN = 6 * 60;
    static final long HERALD_FIRST_SPAWN = 15 * 60;

    private ObjectiveSchedule() {}

    /** Next spawn per objective, from kills (ObjectiveEvents carrying the kill time). The herald does not respawn. */
    public static List<ObjectiveEvent> nextSpawns(List<ObjectiveEvent> kills) {
        List<ObjectiveEvent> spawns = new ArrayList<>();
        spawns.add(new ObjectiveEvent(DRAGON, lastKill(kills, DRAGON, ELDER_DRAGON)
                .map(kill -> kill.atSeconds() + (kill.name().equals(ELDER_DRAGON) ? ELDER_RESPAWN : DRAGON_RESPAWN))
                .orElse(DRAGON_FIRST_SPAWN)));
        spawns.add(new ObjectiveEvent(BARON, lastKill(kills, BARON)
                .map(kill -> kill.atSeconds() + BARON_RESPAWN).orElse(BARON_FIRST_SPAWN)));
        if (lastKill(kills, HERALD).isEmpty()) {
            spawns.add(new ObjectiveEvent(HERALD, HERALD_FIRST_SPAWN));
        }
        return spawns;
    }

    private static Optional<ObjectiveEvent> lastKill(List<ObjectiveEvent> kills, String... names) {
        List<String> wanted = List.of(names);
        return kills.stream().filter(kill -> wanted.contains(kill.name()))
                .max(Comparator.comparingLong(ObjectiveEvent::atSeconds));
    }
}
