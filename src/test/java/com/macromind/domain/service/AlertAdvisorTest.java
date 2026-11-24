package com.macromind.domain.service;

import com.macromind.domain.model.AdvicePriority;
import com.macromind.domain.model.Alert;
import com.macromind.domain.model.AlertKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AlertAdvisorTest {

    @Test
    void registersNoRulesYetSoNothingIsAlerted() {
        assertEquals(List.of(), AlertAdvisor.alerts(game(player("Me", com.macromind.domain.model.ChampionClass.MAGE, false, List.of(), AVERAGE_STATS), List.of()), List.of()));
    }

    @Test
    void concatenatesWhatEveryRuleFinds() {
        Alert a = new Alert(AlertKind.BUY_NOW, "a", "A", AdvicePriority.CRITICAL);
        Alert b = new Alert(AlertKind.SPIKE, "b", "B", AdvicePriority.RECOMMENDED);

        List<Alert> alerts = AlertAdvisor.alerts(List.of((s, adv) -> List.of(a), (s, adv) -> List.of(b)), null, List.of());

        assertEquals(List.of(a, b), alerts);
    }
}
