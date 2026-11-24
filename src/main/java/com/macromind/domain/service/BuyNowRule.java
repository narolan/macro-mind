package com.macromind.domain.service;

import com.macromind.domain.model.AdvicePriority;
import com.macromind.domain.model.Alert;
import com.macromind.domain.model.AlertKind;
import com.macromind.domain.model.BuildAdvice;
import com.macromind.domain.model.GameState;

import java.util.List;

/**
 * Nudges the player when current gold (the active player's totalGold is the Live Client currentGold) covers the
 * top-ranked CRITICAL or RECOMMENDED item. Item.goldValue is the full recipe cost, so owned components are not discounted.
 * shortcut: the nudge fires late for a partly built item, upgrade if that proves annoying.
 */
public final class BuyNowRule implements AlertRule {

    @Override
    public List<Alert> evaluate(GameState state, List<BuildAdvice> advice) {
        if (advice.isEmpty() || advice.get(0).priority() == AdvicePriority.SITUATIONAL) {
            return List.of();
        }
        BuildAdvice top = advice.get(0);
        var item = top.recommendedItem();
        var me = state.activePlayer();
        boolean isOwned = me.items().stream().anyMatch(owned -> owned.id() == item.id());
        if (isOwned || me.totalGold() < item.goldValue()) {
            return List.of();
        }
        return List.of(new Alert(AlertKind.BUY_NOW, "buy-now:" + item.id(),
                "You can afford " + item.name() + " now", top.priority()));
    }
}
