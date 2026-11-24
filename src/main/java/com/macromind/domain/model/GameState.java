package com.macromind.domain.model;

import java.util.List;

/** A snapshot of the game as the active player sees it. */
public record GameState(
        long gameTimeInSeconds,
        Player activePlayer,
        List<Player> allies,
        List<Player> enemies,
        GameMode gameMode,
        List<ObjectiveEvent> objectiveEvents
) {

    public GameState {
        allies = List.copyOf(allies);
        enemies = List.copyOf(enemies);
        objectiveEvents = List.copyOf(objectiveEvents);
    }

    /** A state without objective events. */
    public GameState(long gameTimeInSeconds, Player activePlayer, List<Player> allies, List<Player> enemies, GameMode gameMode) {
        this(gameTimeInSeconds, activePlayer, allies, enemies, gameMode, List.of());
    }

    public int highestEnemyArmor() {
        return enemies.stream().mapToInt(player -> player.stats().armor()).max().orElse(0);
    }

    public int highestEnemyMagicResist() {
        return enemies.stream().mapToInt(player -> player.stats().magicResist()).max().orElse(0);
    }

    public int highestEnemyMaxHealth() {
        return enemies.stream().mapToInt(player -> player.stats().maxHealth()).max().orElse(0);
    }
}
