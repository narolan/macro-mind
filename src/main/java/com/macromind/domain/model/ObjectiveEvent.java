package com.macromind.domain.model;

/** An objective that becomes available at the given game time. */
public record ObjectiveEvent(String name, long atSeconds) {}
