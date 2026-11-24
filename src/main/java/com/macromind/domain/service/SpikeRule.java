package com.macromind.domain.service;

import com.macromind.domain.model.AdvicePriority;
import com.macromind.domain.model.Alert;
import com.macromind.domain.model.AlertKind;
import com.macromind.domain.model.BuildAdvice;
import com.macromind.domain.model.GameState;
import com.macromind.domain.model.Player;

import java.util.List;

/**
 * Warns when the most dangerous living enemy (by {@link EnemyThreat}) has just hit an ultimate level spike: level
 * 6, 11 or 16, or the one level after ({@value #SPIKE_BAND_LEVELS} levels per band). Rules keep no memory, so
 * "just" is judged from the level alone; the poll loop's alert freshness window stops repeats.
 * Big-item spikes are deliberately not covered: from one snapshot a rule cannot tell a new item from an old one.
 * At most one alert per poll.
 */
final class SpikeRule implements AlertRule {

    static final int SPIKE_BAND_LEVELS = 2;
    private static final int[] SPIKE_LEVELS = {6, 11, 16};

    @Override
    public List<Alert> evaluate(GameState state, List<BuildAdvice> advice) {
        List<Player> alive = state.enemies().stream().filter(e -> !e.isDead()).toList();
        Player target = new EnemyThreat(alive).mostDangerous();
        if (target == null) {
            return List.of();
        }
        for (int spikeLevel : SPIKE_LEVELS) {
            if (target.level() >= spikeLevel && target.level() < spikeLevel + SPIKE_BAND_LEVELS) {
                return List.of(new Alert(AlertKind.SPIKE, "spike:" + target.championName() + ":lvl" + spikeLevel,
                        target.championName() + " hit level " + spikeLevel, AdvicePriority.RECOMMENDED));
            }
        }
        return List.of();
    }
}
