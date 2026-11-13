package com.macromind.domain.service;

import com.macromind.domain.model.ChampionClass;
import com.macromind.domain.model.GameState;
import com.macromind.domain.model.Player;

import java.util.List;
import java.util.Set;

/** Read-only questions about the enemy team's makeup that several rules ask. */
record TeamProfile(List<Player> enemies) {

    // ponytail: hand-kept; Riot exposes no crowd-control or shield data. Extend as champions are added.
    private static final Set<String> HARD_CC = Set.of("Leona", "Nautilus", "Amumu", "Malphite", "Sejuani", "Ornn", "Alistar",
            "Blitzcrank", "Thresh", "Morgana", "Lissandra", "Maokai", "Rell", "Skarner", "Zac", "Vi", "Jarvan IV", "Lux", "Syndra",
            "Ahri", "Neeko", "Veigar", "Cassiopeia", "Elise", "Warwick", "Renekton", "Braum", "Taric", "Bard", "Zyra");
    private static final Set<String> SHIELDERS = Set.of("Janna", "Lulu", "Karma", "Orianna", "Sona", "Shen", "Yuumi",
            "Seraphine", "Rakan", "Ivern", "Malphite", "Sivir", "Galio", "Diana", "Lux");

    private static final Set<String> HIGH_HEALING_CHAMPIONS =
            Set.of("Aatrox", "Soraka", "Dr. Mundo", "Sylas", "Vladimir", "Swain", "Briar", "Warwick");

    static final int FRONTLINE_MIN = 2;
    static final int SQUISHY_MIN = 3;
    static final int SHIELDERS_MIN = 2;
    static final int ASSASSINS_MIN = 2;
    static final int HARD_CC_MIN = 3;

    static TeamProfile of(GameState state) {
        return new TeamProfile(state.enemies());
    }

    long count(ChampionClass... classes) {
        Set<ChampionClass> wanted = Set.of(classes);
        return enemies.stream().filter(p -> wanted.contains(p.championClass())).count();
    }

    boolean hasHeavyHealing() {
        return enemies.stream().anyMatch(p -> HIGH_HEALING_CHAMPIONS.contains(p.championName()) || p.hasHighSustainStats());
    }

    boolean hasFrontline() {
        return count(ChampionClass.TANK, ChampionClass.BRUISER) >= FRONTLINE_MIN;
    }

    boolean isSquishy() {
        return count(ChampionClass.TANK, ChampionClass.BRUISER) == 0 && enemies.size() >= SQUISHY_MIN;
    }

    boolean hasShieldChampions() {
        return enemies.stream().filter(p -> SHIELDERS.contains(p.championName())).count() >= SHIELDERS_MIN;
    }

    boolean hasHeavyCrowdControl() {
        return enemies.stream().filter(p -> HARD_CC.contains(p.championName())).count() >= HARD_CC_MIN;
    }

    List<Player> assassins() {
        return enemies.stream().filter(p -> p.championClass() == ChampionClass.ASSASSIN).toList();
    }
}
