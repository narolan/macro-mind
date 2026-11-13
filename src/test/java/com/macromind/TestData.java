package com.macromind;

import com.macromind.domain.model.AdvicePriority;
import com.macromind.domain.model.BuildAdvice;
import com.macromind.domain.model.ChampionClass;
import com.macromind.domain.model.GameMode;
import com.macromind.domain.model.GameState;
import com.macromind.domain.model.Item;
import com.macromind.domain.model.ItemStats;
import com.macromind.domain.model.ItemTag;
import com.macromind.domain.model.KDA;
import com.macromind.domain.model.Player;
import com.macromind.domain.model.PlayerStats;
import com.macromind.domain.model.Role;
import com.macromind.domain.service.BuildAdvisor;

import java.util.List;
import java.util.Set;

/** Small builders for domain objects, shared by the unit tests. */
public final class TestData {

    private static final int DEFAULT_ITEM_GOLD = 1000;
    private static final int DEFAULT_LEVEL = 13;
    private static final long DEFAULT_GAME_SECONDS = 600;

    public static final ItemStats NO_STATS = ItemStats.NONE;
    public static final PlayerStats AVERAGE_STATS = new PlayerStats(80, 50, 2000, 2000, 100, 0, 0, 0, 10);

    private TestData() {}

    // items

    public static Item item(String name, ItemStats stats, ItemTag... tags) {
        return itemWith(name, DEFAULT_ITEM_GOLD, stats, tags);
    }

    public static Item itemWith(String name, int gold, ItemStats stats, ItemTag... tags) {
        return new Item(name.hashCode(), name, gold, stats, Set.of(tags));
    }

    public static ItemStats withFlatArmorPen(int pen) {
        return ItemStats.NONE.withFlatArmorPen(pen);
    }

    public static ItemStats withArmor(int armor) {
        return ItemStats.NONE.withArmor(armor);
    }

    public static ItemStats withMagicResist(int mr) {
        return ItemStats.NONE.withMagicResist(mr);
    }

    /** Item stats with only the fields the rule tests care about. */
    public static ItemStats itemStats(int attackDamage, int abilityPower, int armor, int magicResist, int health,
                                      int lethality, int flatMagicPen, double attackSpeed, double abilityHaste) {
        return new ItemStats(attackDamage, abilityPower, armor, magicResist, health, 0, lethality, flatMagicPen,
                attackSpeed, abilityHaste, 0, 0);
    }

    // players

    public static PlayerStats stats(int armor, int magicResist, int attackDamage, int abilityPower) {
        return new PlayerStats(armor, magicResist, 2000, 2000, attackDamage, abilityPower, 0, 0, 10);
    }

    public static Player player(String champion, ChampionClass championClass, boolean isEnemy,
                                List<Item> items, PlayerStats stats) {
        return new Player(champion, champion, championClass, DEFAULT_LEVEL, isEnemy, items, stats,
                Role.MID, new KDA(0, 0, 0), 0, false, false);
    }

    public static Player estimated(Player p) {
        return p.asEstimated();
    }

    public static Player withGold(Player p, int gold) {
        return p.withGold(gold);
    }

    public static Player withKda(Player p, int kills, int deaths, int assists) {
        return p.withKda(new KDA(kills, deaths, assists));
    }

    // games

    public static GameState game(Player active, List<Player> enemies) {
        return gameAt(DEFAULT_GAME_SECONDS, active, enemies);
    }

    public static GameState gameAt(long seconds, Player active, List<Player> enemies) {
        return new GameState(seconds, active, List.of(), enemies, GameMode.SUMMONERS_RIFT);
    }

    // advice

    public static List<String> names(List<BuildAdvice> advice) {
        return advice.stream().map(a -> a.recommendedItem().name()).toList();
    }

    /** Advice that is more urgent than the low-priority class-fit suggestions every game also gets. */
    public static List<BuildAdvice> urgent(List<BuildAdvice> advice) {
        return advice.stream().filter(a -> a.priority() != AdvicePriority.SITUATIONAL).toList();
    }

    /** Urgent advice for the active player against the enemies, at the given game time. */
    public static List<BuildAdvice> advise(long seconds, Player active, List<Player> enemies, Item... items) {
        return urgent(new BuildAdvisor(gameAt(seconds, active, enemies), List.of(items)).generateBuildAdvices());
    }
}
