package com.macromind.domain.model;

public enum ChampionClass {
    MARKSMAN,
    MAGE,
    TANK,
    BRUISER,
    ASSASSIN,
    SUPPORT,
    /** The champion could not be looked up: no class-based advice is given rather than guessing one. */
    UNKNOWN
}
