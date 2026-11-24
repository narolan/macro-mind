package com.macromind.domain.model;

/** A short heads-up for the player. The key identifies it across polls, so it is shown only while it is new. */
public record Alert(AlertKind kind, String key, String message, AdvicePriority priority) {}
