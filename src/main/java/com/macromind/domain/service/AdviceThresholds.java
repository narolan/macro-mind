package com.macromind.domain.service;

/** The numbers that decide when a rule fires. Kept in one place so two rules cannot drift apart. */
final class AdviceThresholds {

    /** Enemy damage is "mostly AD" above this share of physical threat, and "mostly AP" below its mirror. */
    static final double HEAVY_AD_RATIO = 0.7;
    static final double HEAVY_AP_RATIO = 0.3;

    /** Boots are only worth advising once the laning phase is under way. */
    static final long BOOTS_MIN_SECONDS = 180;

    /** Mid-game rules (penetration, class-fit core items) wait until the first items are done. */
    static final long MID_GAME_SECONDS = 600;

    private AdviceThresholds() {}
}
