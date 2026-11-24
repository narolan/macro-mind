package com.macromind.domain.model;

/** An objective (Dragon, Elder Dragon, Baron, Rift Herald) and a game time: its kill on GameState, its next spawn in the schedule. */
public record ObjectiveEvent(String name, long atSeconds) {}
