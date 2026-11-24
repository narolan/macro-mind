package com.macromind.domain.service;

import com.macromind.domain.model.Alert;
import com.macromind.domain.model.BuildAdvice;
import com.macromind.domain.model.GameState;

import java.util.List;

/** One source of alerts. Register new rules in {@link AlertAdvisor#RULES}. */
@FunctionalInterface
public interface AlertRule {
    List<Alert> evaluate(GameState state, List<BuildAdvice> advice);
}
