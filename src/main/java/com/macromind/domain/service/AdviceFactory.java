package com.macromind.domain.service;

import com.macromind.domain.model.*;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Turns a rule into advice, applying the filters every rule shares so no rule can recommend a pointless item. */
final class AdviceFactory {

    private final GameState gameState;
    private final List<Item> possibleItems;
    private final Set<String> ownedNames;
    private final Confidence confidence;

    AdviceFactory(GameState gameState, List<Item> possibleItems) {
        this.gameState = gameState;
        this.possibleItems = possibleItems;
        this.ownedNames = gameState.activePlayer().items().stream().map(Item::name).collect(Collectors.toUnmodifiableSet());
        this.confidence = gameState.enemies().stream().anyMatch(Player::isEstimated)
                ? Confidence.ESTIMATED : Confidence.EXACT;
    }

    List<BuildAdvice> create(Predicate<Item> matcher, String reasoning, AdvicePriority priority,
                             AdviceCategory category, Player target) {
        return create(new AdviceRule(matcher, reasoning, priority, category), target);
    }

    List<BuildAdvice> create(AdviceRule rule, Player target) {
        Player active = gameState.activePlayer();
        return possibleItems.stream()
                .filter(item -> !ownedNames.contains(item.name()))
                .filter(ItemFit::isAdviseable)
                .filter(item -> ItemFit.fitsDamageType(item, active))
                .filter(item -> !(item.hasTag(ItemTag.BOOTS) && active.hasBoots()))
                .filter(rule.itemMatcher())
                .map(item -> new BuildAdvice(item, rule.reasoning(), rule.priority(), rule.category(),
                        target, confidence, gameState.gameTimeInSeconds()))
                .toList();
    }
}
