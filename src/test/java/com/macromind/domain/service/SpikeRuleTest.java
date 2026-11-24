package com.macromind.domain.service;

import com.macromind.domain.model.Alert;
import com.macromind.domain.model.AlertKind;
import com.macromind.domain.model.ChampionClass;
import com.macromind.domain.model.Item;
import com.macromind.domain.model.ItemTag;
import com.macromind.domain.model.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpikeRuleTest {

    private static final Item BIG = itemWith("Big", 3000, NO_STATS);
    private static final Item CHEAP = itemWith("Cheap", 1000, NO_STATS);
    private static final Item BIG_COMPONENT = itemWith("BigComp", 3000, NO_STATS, ItemTag.COMPONENT);
    private static final Item BIG_BOOTS = itemWith("Greaves", 3000, NO_STATS, ItemTag.BOOTS);

    private final SpikeRule rule = new SpikeRule();

    private static Player enemy(String name, int level, Item... items) {
        Player p = player(name, ChampionClass.ASSASSIN, true, List.of(items), AVERAGE_STATS);
        return new Player(name, name, p.championClass(), level, true, p.items(), p.stats(), p.role(), p.kda(), 0, false, false);
    }

    private List<Alert> run(Player... enemies) {
        return rule.evaluate(game(player("Me", ChampionClass.MAGE, false, List.of(), AVERAGE_STATS), List.of(enemies)), List.of());
    }

    private static List<String> keys(List<Alert> alerts) {
        return alerts.stream().map(Alert::key).toList();
    }

    @Test
    void warnsWhenEnemyReachesEachLevelThreshold() {
        assertEquals(List.of("spike:Zed:lvl6"), keys(run(enemy("Zed", 6))));
        assertEquals(List.of("spike:Zed:lvl11"), keys(run(enemy("Zed", 11))));
        assertEquals(List.of("spike:Zed:lvl16"), keys(run(enemy("Zed", 17))));
    }

    @Test
    void staysQuietBelowFirstThreshold() {
        assertEquals(List.of(), run(enemy("Zed", 5)));
    }

    @Test
    void messageNamesChampionAndLevelAsSpike() {
        Alert alert = run(enemy("Zed", 6)).getFirst();
        assertEquals(AlertKind.SPIKE, alert.kind());
        assertEquals("Zed hit level 6", alert.message());
    }

    @Test
    void countsOnlyFinishedNonBootItems() {
        List<Alert> alerts = run(enemy("Zed", 1, BIG, BIG, CHEAP, BIG_COMPONENT, BIG_BOOTS));
        assertEquals(List.of("spike:Zed:items:2"), keys(alerts));
        assertEquals("Zed now has 2 big items", alerts.getFirst().message());
        assertEquals(List.of(), run(enemy("Zed", 1, CHEAP, BIG_COMPONENT, BIG_BOOTS)));
    }

    @Test
    void warnsOnlyForMostDangerousEnemy() {
        Player weak = enemy("Weak", 6);
        Player strong = withKda(enemy("Strong", 6), 10, 0, 0);
        assertEquals(List.of("spike:Strong:lvl6"), keys(run(weak, strong)));
    }

    @Test
    void skipsDeadEnemies() {
        Player deadStrong = withKda(enemy("Strong", 6), 10, 0, 0).withDead(true);
        assertEquals(List.of("spike:Weak:lvl6"), keys(run(deadStrong, enemy("Weak", 6))));
    }

    @Test
    void capsAtTwoAlertsPerPoll() {
        List<Alert> alerts = run(enemy("Zed", 16, BIG));
        assertEquals(2, alerts.size());
        assertTrue(alerts.stream().allMatch(a -> a.kind() == AlertKind.SPIKE));
    }

    @Test
    void returnsNothingWithoutEnemies() {
        assertEquals(List.of(), run());
    }
}
