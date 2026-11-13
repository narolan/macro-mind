package com.macromind;

import com.macromind.domain.model.*;

import java.util.List;
import java.util.Set;

public final class TestData {

    private TestData() {}

    public static final ItemStats NO_STATS = new ItemStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    public static final PlayerStats AVERAGE_STATS = new PlayerStats(80, 50, 2000, 2000, 100, 0, 0, 0, 10);

    public static Item item(String name, ItemStats stats, ItemTag... tags) {
        return new Item(name.hashCode(), name, 1000, stats, Set.of(tags));
    }

    public static ItemStats withFlatArmorPen(int pen) {
        return new ItemStats(0, 0, 0, 0, 0, 0, pen, 0, 0, 0, 0, 0);
    }

    public static ItemStats withArmor(int armor) {
        return new ItemStats(0, 0, armor, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public static ItemStats withMagicResist(int mr) {
        return new ItemStats(0, 0, 0, mr, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public static Player player(String champion, ChampionClass championClass, boolean isEnemy,
                                List<Item> items, PlayerStats stats) {
        return new Player(champion, champion, championClass, 13, isEnemy, items, stats,
                Role.MID, new KDA(0, 0, 0), 0, false, false);
    }

    public static Player estimated(Player p) {
        return new Player(p.name(), p.championName(), p.championClass(), p.level(), p.isEnemy(), p.items(),
                p.stats(), p.role(), p.kda(), p.totalGold(), true, p.isDead());
    }

    public static PlayerStats stats(int armor, int magicResist, int attackDamage, int abilityPower) {
        return new PlayerStats(armor, magicResist, 2000, 2000, attackDamage, abilityPower, 0, 0, 10);
    }

    public static GameState game(Player active, List<Player> enemies) {
        return new GameState(600, active, List.of(), enemies, GameMode.SUMMONERS_RIFT);
    }
}
