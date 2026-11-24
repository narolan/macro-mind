package com.macromind.domain.service;

import com.macromind.domain.model.Alert;
import com.macromind.domain.model.AlertKind;
import com.macromind.domain.model.ChampionClass;
import com.macromind.domain.model.Item;
import com.macromind.domain.model.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class DamageMixRuleTest {

    private static final Player AD_ENEMY = player("Zed", ChampionClass.ASSASSIN, true, List.of(), stats(50, 50, 400, 0));
    private static final Player AP_ENEMY = player("Syndra", ChampionClass.MAGE, true, List.of(), stats(50, 50, 0, 400));
    private static final Item ARMOR = item("Plate", withArmor(40));
    private static final Item MR = item("Mantle", withMagicResist(40));
    private static final long LATE = 600;

    private static List<Alert> run(long seconds, ChampionClass championClass, List<Item> owned, Player... enemies) {
        Player me = player("Me", championClass, false, owned, AVERAGE_STATS);
        return new DamageMixRule().evaluate(gameAt(seconds, me, List.of(enemies)), List.of());
    }

    @Test
    void warnsAboutHeavyAdWhenPlayerHasNoArmor() {
        List<Alert> alerts = run(LATE, ChampionClass.MARKSMAN, List.of(MR), AD_ENEMY);

        assertEquals(1, alerts.size());
        assertEquals(AlertKind.DAMAGE_MIX, alerts.get(0).kind());
        assertEquals("damage-mix-ad", alerts.get(0).key());
        assertEquals("Enemy damage is mostly AD and you have no armor yet", alerts.get(0).message());
    }

    @Test
    void warnsAboutHeavyApWhenPlayerHasNoMagicResist() {
        List<Alert> alerts = run(LATE, ChampionClass.TANK, List.of(ARMOR), AP_ENEMY);

        assertEquals(1, alerts.size());
        assertEquals("damage-mix-ap", alerts.get(0).key());
        assertEquals("Enemy damage is mostly AP and you have no magic resist yet", alerts.get(0).message());
    }

    @Test
    void staysQuietOnceTheMatchingResistIsOwned() {
        assertEquals(List.of(), run(LATE, ChampionClass.MARKSMAN, List.of(ARMOR), AD_ENEMY));
        assertEquals(List.of(), run(LATE, ChampionClass.MARKSMAN, List.of(MR), AP_ENEMY));
    }

    @Test
    void staysQuietForSupportAndUnknownClasses() {
        assertEquals(List.of(), run(LATE, ChampionClass.SUPPORT, List.of(), AD_ENEMY));
        assertEquals(List.of(), run(LATE, ChampionClass.UNKNOWN, List.of(), AD_ENEMY));
    }

    @Test
    void staysQuietBeforeTheLaningPhaseIsUnderWay() {
        assertEquals(List.of(), run(AdviceThresholds.BOOTS_MIN_SECONDS - 1, ChampionClass.MARKSMAN, List.of(), AD_ENEMY));
    }

    @Test
    void staysQuietForABalancedMix() {
        assertEquals(List.of(), run(LATE, ChampionClass.MARKSMAN, List.of(), AD_ENEMY, AP_ENEMY));
    }
}
