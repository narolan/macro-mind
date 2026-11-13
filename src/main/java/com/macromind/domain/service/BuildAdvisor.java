package com.macromind.domain.service;

import com.macromind.domain.model.BuildAdvice;
import com.macromind.domain.model.GameState;
import com.macromind.domain.model.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a game snapshot into a ranked list of item advice.
 *
 * <p>Three groups of rules contribute, in this order: threats (heavy healing, armor, AD/AP), team composition
 * (boots, penetration, shields, crowd control) and a low-priority class fit. The ranker then merges and orders them.
 */
public final class BuildAdvisor {

    private final GameState gameState;
    private final List<Item> possibleItems;

    public BuildAdvisor(GameState gameState, List<Item> possibleItems) {
        this.gameState = gameState;
        this.possibleItems = List.copyOf(possibleItems);
    }

    public List<BuildAdvice> generateBuildAdvices() {
        AdviceFactory factory = new AdviceFactory(gameState, possibleItems);
        List<BuildAdvice> advice = new ArrayList<>();
        advice.addAll(new ThreatRules(gameState, factory).advise());
        advice.addAll(new CompositionRules(gameState, factory).advise());
        advice.addAll(new CoreItemRules(gameState, factory).advise());
        return AdviceRanker.rank(advice, gameState.activePlayer().totalGold());
    }
}
