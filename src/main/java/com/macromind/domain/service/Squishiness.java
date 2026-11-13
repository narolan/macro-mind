package com.macromind.domain.service;

import com.macromind.domain.model.ChampionClass;
import com.macromind.domain.model.ItemTag;
import com.macromind.domain.model.Player;

import java.util.Set;

/** Whether a player is fragile enough that burst-protection items matter: three of five signals must hold. */
final class Squishiness {

    private static final Set<ChampionClass> SQUISHY_CLASSES =
            Set.of(ChampionClass.MARKSMAN, ChampionClass.MAGE, ChampionClass.ASSASSIN, ChampionClass.SUPPORT);
    private static final int SQUISHY_MAX_HEALTH = 2500;
    private static final int SQUISHY_MAX_RESIST = 100;
    private static final int SIGNALS_REQUIRED = 3;

    private Squishiness() {}

    static boolean of(Player player) {
        int signals = 0;
        if (SQUISHY_CLASSES.contains(player.championClass())) {
            signals++;
        }
        if (player.stats().maxHealth() < SQUISHY_MAX_HEALTH) {
            signals++;
        }
        if (player.stats().armor() < SQUISHY_MAX_RESIST) {
            signals++;
        }
        if (player.stats().magicResist() < SQUISHY_MAX_RESIST) {
            signals++;
        }
        if (player.items().stream().noneMatch(item -> item.hasAnyTag(ItemTag.BURST_RESISTANCE, ItemTag.SPELL_SHIELD))) {
            signals++;
        }
        return signals >= SIGNALS_REQUIRED;
    }
}
