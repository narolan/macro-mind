package com.macromind.domain.service;

import com.macromind.domain.model.Alert;
import com.macromind.domain.model.BuildAdvice;
import com.macromind.domain.model.GameState;

import java.util.List;

/** Runs every registered {@link AlertRule} and concatenates what they find. */
public final class AlertAdvisor {

    /** One line per rule. */
    public static final List<AlertRule> RULES = List.of(
            new BuyNowRule(),
            new SpikeRule());

    private AlertAdvisor() {}

    public static List<Alert> alerts(GameState state, List<BuildAdvice> advice) {
        return alerts(RULES, state, advice);
    }

    public static List<Alert> alerts(List<AlertRule> rules, GameState state, List<BuildAdvice> advice) {
        return rules.stream().flatMap(rule -> rule.evaluate(state, advice).stream()).toList();
    }
}
