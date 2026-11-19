package com.macromind.api;

/** What the app tells the user about itself, so "nothing on screen" is never a guessing game. */
public enum AppStatus {
    /** Working, and League is not in a match. */
    NO_GAME,
    /** Reading a live match and advising. */
    IN_GAME,
    /** Riot's item data never loaded, so no advice is possible. */
    NO_DATA,
    /** The game client answered but its response could not be used. */
    ERROR
}
