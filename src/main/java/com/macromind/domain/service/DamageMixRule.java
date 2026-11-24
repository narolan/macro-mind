package com.macromind.domain.service;

import com.macromind.domain.model.AdvicePriority;
import com.macromind.domain.model.Alert;
import com.macromind.domain.model.AlertKind;
import com.macromind.domain.model.BuildAdvice;
import com.macromind.domain.model.ChampionClass;
import com.macromind.domain.model.GameState;
import com.macromind.domain.model.Item;
import com.macromind.domain.model.Player;

import java.util.List;
import java.util.function.ToIntFunction;

/** Warns when the enemy team is heavily one damage type and the player has no matching resist item yet. */
final class DamageMixRule implements AlertRule {

    private static final Alert NO_ARMOR = new Alert(AlertKind.DAMAGE_MIX, "damage-mix-ad",
            "Enemy damage is mostly AD and you have no armor yet", AdvicePriority.RECOMMENDED);
    private static final Alert NO_MAGIC_RESIST = new Alert(AlertKind.DAMAGE_MIX, "damage-mix-ap",
            "Enemy damage is mostly AP and you have no magic resist yet", AdvicePriority.RECOMMENDED);

    @Override
    public List<Alert> evaluate(GameState state, List<BuildAdvice> advice) {
        Player me = state.activePlayer();
        boolean isExcluded = me.championClass() == ChampionClass.SUPPORT || me.championClass() == ChampionClass.UNKNOWN;
        if (isExcluded || state.gameTimeInSeconds() < AdviceThresholds.BOOTS_MIN_SECONDS) {
            return List.of();
        }
        double adRatio = EnemyThreat.of(state).adRatio();
        if (adRatio > AdviceThresholds.HEAVY_AD_RATIO && !owns(me, item -> item.stats().armor())) {
            return List.of(NO_ARMOR);
        }
        if (adRatio < AdviceThresholds.HEAVY_AP_RATIO && !owns(me, item -> item.stats().magicResist())) {
            return List.of(NO_MAGIC_RESIST);
        }
        return List.of();
    }

    private static boolean owns(Player me, ToIntFunction<Item> resist) {
        return me.items().stream().anyMatch(item -> resist.applyAsInt(item) > 0);
    }
}
