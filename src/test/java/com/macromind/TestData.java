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

    public static Item itemWith(String name, int gold, ItemStats stats, ItemTag... tags) {
        return new Item(name.hashCode(), name, gold, stats, Set.of(tags));
    }

    /** Item stats with only the fields the rule tests care about. */
    public static ItemStats itemStats(int attackDamage, int abilityPower, int armor, int magicResist, int health,
                                      int lethality, int flatMagicPen, double attackSpeed, double abilityHaste) {
        return new ItemStats(attackDamage, abilityPower, armor, magicResist, health, 0, lethality, flatMagicPen,
                attackSpeed, abilityHaste, 0, 0);
    }

    public static Player withGold(Player p, int gold) {
        return new Player(p.name(), p.championName(), p.championClass(), p.level(), p.isEnemy(), p.items(),
                p.stats(), p.role(), p.kda(), gold, p.isEstimated(), p.isDead());
    }

    public static GameState gameAt(long seconds, Player active, List<Player> enemies) {
        return new GameState(seconds, active, List.of(), enemies, GameMode.SUMMONERS_RIFT);
    }

    public static Player withKda(Player p, int kills, int deaths, int assists) {
        return new Player(p.name(), p.championName(), p.championClass(), p.level(), p.isEnemy(), p.items(),
                p.stats(), p.role(), new KDA(kills, deaths, assists), p.totalGold(), p.isEstimated(), p.isDead());
    }
}
