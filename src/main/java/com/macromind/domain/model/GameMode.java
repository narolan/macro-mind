package com.macromind.domain.model;

import java.util.Set;

public enum GameMode {
    SUMMONERS_RIFT(Set.of(11, 453)),
    ARAM(Set.of(12));

    private final Set<Integer> mapIds;

    GameMode(Set<Integer> mapIds) {
        this.mapIds = mapIds;
    }

    /** Data Dragon map ids whose shop this mode uses. */
    public Set<Integer> mapIds() {
        return mapIds;
    }

    /** The Live Client's mapNumber; any map without its own item pool here (including Arena) falls back to Summoner's Rift. */
    public static GameMode fromMapNumber(int mapNumber) {
        for (GameMode mode : values()) {
            if (mode.mapIds.contains(mapNumber)) {
                return mode;
            }
        }
        return SUMMONERS_RIFT;
    }
}
