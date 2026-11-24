package com.macromind.domain.service;

import com.macromind.domain.model.AdviceCategory;
import com.macromind.domain.model.AdvicePriority;
import com.macromind.domain.model.Alert;
import com.macromind.domain.model.AlertKind;
import com.macromind.domain.model.BuildAdvice;
import com.macromind.domain.model.ChampionClass;
import com.macromind.domain.model.Confidence;
import com.macromind.domain.model.GameState;
import com.macromind.domain.model.Item;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuyNowRuleTest {

    private static final Item CALLING = itemWith("Executioner's Calling", 800, NO_STATS);
    private final BuyNowRule rule = new BuyNowRule();

    private static BuildAdvice advice(Item item, AdvicePriority priority) {
        return new BuildAdvice(item, "why", priority, AdviceCategory.OFFENSIVE, null, Confidence.EXACT, 0L);
    }

    private static GameState stateWith(int gold, List<Item> owned) {
        return game(withGold(player("Me", ChampionClass.MAGE, false, owned, AVERAGE_STATS), gold), List.of());
    }

    @Test
    void alertsWhenGoldCoversTopRecommendedItem() {
        List<Alert> alerts = rule.evaluate(stateWith(800, List.of()), List.of(advice(CALLING, AdvicePriority.RECOMMENDED)));

        assertEquals(1, alerts.size());
        assertEquals(AlertKind.BUY_NOW, alerts.get(0).kind());
        assertEquals("You can afford Executioner's Calling now", alerts.get(0).message());
        assertEquals(AdvicePriority.RECOMMENDED, alerts.get(0).priority());
    }

    @Test
    void staysSilentWhenGoldFallsShort() {
        assertTrue(rule.evaluate(stateWith(799, List.of()), List.of(advice(CALLING, AdvicePriority.CRITICAL))).isEmpty());
    }

    @Test
    void staysSilentForSituationalAdvice() {
        assertTrue(rule.evaluate(stateWith(5000, List.of()), List.of(advice(CALLING, AdvicePriority.SITUATIONAL))).isEmpty());
    }

    @Test
    void staysSilentWithoutAdvice() {
        assertTrue(rule.evaluate(stateWith(5000, List.of()), List.of()).isEmpty());
    }

    @Test
    void staysSilentForAnItemAlreadyOwned() {
        assertTrue(rule.evaluate(stateWith(5000, List.of(CALLING)), List.of(advice(CALLING, AdvicePriority.CRITICAL))).isEmpty());
    }

    @Test
    void keyIsStableAcrossGoldChanges() {
        List<BuildAdvice> ranked = List.of(advice(CALLING, AdvicePriority.RECOMMENDED));

        String first = rule.evaluate(stateWith(800, List.of()), ranked).get(0).key();
        String later = rule.evaluate(stateWith(2500, List.of()), ranked).get(0).key();

        assertEquals("buy-now:" + CALLING.id(), first);
        assertEquals(first, later);
    }
}
