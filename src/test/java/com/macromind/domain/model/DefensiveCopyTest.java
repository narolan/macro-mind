package com.macromind.domain.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DefensiveCopyTest {

    private static final Item ITEM = new Item(1, "Thing", 1000, ItemStats.NONE, Set.of());

    private static Player playerWith(List<Item> items) {
        return new Player("p", "Jinx", ChampionClass.MARKSMAN, 9, false, items,
                new PlayerStats(1, 2, 3, 4, 5, 6, 7, 8, 9), Role.ADC, new KDA(0, 0, 0), 0, false, false);
    }

    @Test
    void playerKeepsItsOwnCopyOfItemsSoLaterCallerChangesDoNotLeakIn() {
        List<Item> source = new ArrayList<>(List.of(ITEM));
        Player player = playerWith(source);

        source.clear();

        assertEquals(1, player.items().size());
    }

    @Test
    void playerItemsCannotBeModifiedThroughTheAccessor() {
        Player player = playerWith(new ArrayList<>(List.of(ITEM)));

        assertThrows(UnsupportedOperationException.class, () -> player.items().add(ITEM));
    }

    @Test
    void gameStateKeepsItsOwnCopyOfAlliesAndEnemies() {
        Player p = playerWith(List.of());
        List<Player> allies = new ArrayList<>(List.of(p));
        List<Player> enemies = new ArrayList<>(List.of(p));
        GameState state = new GameState(10, p, allies, enemies, GameMode.SUMMONERS_RIFT);

        allies.clear();
        enemies.clear();

        assertEquals(1, state.allies().size());
        assertEquals(1, state.enemies().size());
        assertThrows(UnsupportedOperationException.class, () -> state.enemies().add(p));
    }

    @Test
    void itemKeepsItsOwnCopyOfItsTags() {
        Set<ItemTag> source = new HashSet<>(Set.of(ItemTag.BOOTS));
        Item item = new Item(1, "Boots", 300, ItemStats.NONE, source);

        source.add(ItemTag.CLEANSE);

        assertEquals(Set.of(ItemTag.BOOTS), item.tags());
        assertThrows(UnsupportedOperationException.class, () -> item.tags().add(ItemTag.CLEANSE));
    }

    @Test
    void gameStateKeepsItsOwnCopyOfObjectiveEvents() {
        Player p = playerWith(List.of());
        List<ObjectiveEvent> events = new ArrayList<>(List.of(new ObjectiveEvent("Baron", 1200)));
        GameState state = new GameState(10, p, List.of(), List.of(), GameMode.SUMMONERS_RIFT, events);

        events.clear();

        assertEquals(1, state.objectiveEvents().size());
    }
}
