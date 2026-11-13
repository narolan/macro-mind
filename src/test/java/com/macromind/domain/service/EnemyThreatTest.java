package com.macromind.domain.service;

import com.macromind.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class EnemyThreatTest {

    private static final Player ME = player("Me", ChampionClass.MARKSMAN, false, List.of(), AVERAGE_STATS);

    private static EnemyThreat threat(Player... enemies) {
        return EnemyThreat.of(game(ME, List.of(enemies)));
    }

    // pinned numbers: these must not move when the maths is relocated

    @Test
    void scoresAdThreatFromAttackDamageKdaAndGold() {
        Player zed = player("Zed", ChampionClass.ASSASSIN, true, List.of(), stats(50, 50, 300, 0))
                .withKda(new KDA(5, 1, 2)).withGold(4000);

        // 300 * (1 + 5*0.12 + 2*0.04 - 1*0.06) * (1 + 4000/8000) = 300 * 1.62 * 1.5
        assertEquals(729.0, threat(zed).totalAdThreat(), 1e-9);
        assertEquals(0.0, threat(zed).totalApThreat(), 1e-9);
    }

    @Test
    void scoresApThreatFromAbilityPowerKdaAndGold() {
        Player ahri = player("Ahri", ChampionClass.MAGE, true, List.of(), stats(50, 50, 80, 400))
                .withKda(new KDA(0, 4, 0)).withGold(8000);

        // 400 * max(0.5, 1 - 4*0.06 = 0.76) * (1 + 1) = 608
        assertEquals(608.0, threat(ahri).totalApThreat(), 1e-9);
    }

    @Test
    void neverLetsKdaShrinkThreatBelowHalf() {
        Player feeder = player("Zed", ChampionClass.ASSASSIN, true, List.of(), stats(50, 50, 100, 0))
                .withKda(new KDA(0, 20, 0));

        assertEquals(100 * 0.5 * 1.0, threat(feeder).totalAdThreat(), 1e-9);
    }

    @Test
    void adRatioIsNeutralWithoutAnyThreat() {
        assertEquals(0.5, threat().adRatio(), 1e-9);
        assertEquals(0.5, threat(player("Ornn", ChampionClass.TANK, true, List.of(), stats(80, 80, 0, 0))).adRatio(), 1e-9);
    }

    @Test
    void highestThreatEnemiesAreFoundPerDamageType() {
        Player zed = player("Zed", ChampionClass.ASSASSIN, true, List.of(), stats(50, 50, 400, 0));
        Player ahri = player("Ahri", ChampionClass.MAGE, true, List.of(), stats(50, 50, 80, 400));

        assertEquals(zed, threat(zed, ahri).highestAdThreat());
        assertEquals(ahri, threat(zed, ahri).highestApThreat());
    }

    @Test
    void highestThreatIsNullWithoutEnemies() {
        assertNull(threat().highestAdThreat());
        assertNull(threat().highestApThreat());
    }

    // the estimated-mage rule that already existed

    @Test
    void estimatedMagesCountAsApThreatEvenWithoutApItems() {
        Player[] mages = new Player[5];
        for (int i = 0; i < mages.length; i++) {
            mages[i] = player("Mage" + i, ChampionClass.MAGE, true, List.of(), stats(30, 30, 100, 0)).asEstimated();
        }

        assertTrue(threat(mages).adRatio() < 0.3);
    }

    @Test
    void estimatedMarksmenStillCountAsAdThreat() {
        Player[] adcs = new Player[5];
        for (int i = 0; i < adcs.length; i++) {
            adcs[i] = player("Adc" + i, ChampionClass.MARKSMAN, true, List.of(), stats(30, 30, 100, 0)).asEstimated();
        }

        assertTrue(threat(adcs).adRatio() > 0.7);
    }

    @Test
    void exactMageStatsAreLeftUntouched() {
        assertTrue(threat(player("Mage", ChampionClass.MAGE, true, List.of(), stats(30, 30, 100, 0))).adRatio() > 0.7);
    }
}
