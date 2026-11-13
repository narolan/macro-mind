package com.macromind.domain.service;

import com.macromind.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class StatEstimatorTest {

    private static final ChampionBaseStats MALPHITE =
            new ChampionBaseStats(600, 100, 30, 4, 32, 2, 60, 3);

    private final StatEstimator estimator =
            new StatEstimator(name -> name.equals("Malphite") ? Optional.of(MALPHITE) : Optional.empty());

    @Test
    void estimatesLevelOneArmorAsBasePlusItems() {
        Item plate = item("Plate", withArmor(50));

        PlayerStats stats = estimator.estimate("Malphite", 1, List.of(plate));

        assertEquals(80, stats.armor());
        assertEquals(600, stats.maxHealth());
    }

    @Test
    void appliesGrowthFormulaAtLevelEighteen() {
        // growth factor at level 18 = 17 * (0.7025 + 0.0175 * 17) = 17.0
        PlayerStats stats = estimator.estimate("Malphite", 18, List.of());

        assertEquals(30 + 4 * 17, stats.armor());
        assertEquals(32 + 2 * 17, stats.magicResist());
        assertEquals(600 + 100 * 17, stats.maxHealth());
    }

    @Test
    void fallsBackToZeroBaseStatsForUnknownChampion() {
        Item plate = item("Plate", withArmor(50));

        PlayerStats stats = estimator.estimate("BrandNewChampion", 10, List.of(plate));

        assertEquals(50, stats.armor());
    }
}
