package com.macromind.domain.service;

import com.macromind.domain.model.AdvicePriority;
import com.macromind.domain.model.Alert;
import com.macromind.domain.model.AlertKind;
import com.macromind.domain.model.BuildAdvice;
import com.macromind.domain.model.GameState;
import com.macromind.domain.model.Item;
import com.macromind.domain.model.ItemTag;
import com.macromind.domain.model.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Warns when the most dangerous living enemy (by {@link EnemyThreat}) hits a power spike: ultimate levels
 * 6, 11 and 16, or a changed count of completed big items. A "completed big item" is worth at least
 * {@value #BIG_ITEM_GOLD} gold and is not tagged COMPONENT (builds into something), BOOTS or STARTER.
 * Each spike has its own key, so it is announced once; at most {@value #MAX_ALERTS} alerts per poll.
 */
final class SpikeRule implements AlertRule {

    static final int BIG_ITEM_GOLD = 2200;
    static final int MAX_ALERTS = 2;
    private static final int[] SPIKE_LEVELS = {6, 11, 16};

    @Override
    public List<Alert> evaluate(GameState state, List<BuildAdvice> advice) {
        List<Player> alive = state.enemies().stream().filter(e -> !e.isDead()).toList();
        Player target = new EnemyThreat(alive).mostDangerous();
        if (target == null) {
            return List.of();
        }
        List<Alert> alerts = new ArrayList<>();
        int level = highestReachedLevel(target.level());
        if (level > 0) {
            alerts.add(spike(target, "lvl" + level, target.championName() + " hit level " + level));
        }
        long bigItems = target.items().stream().filter(SpikeRule::isCompletedBigItem).count();
        if (bigItems > 0) {
            alerts.add(spike(target, "items:" + bigItems, target.championName() + " now has " + bigItems + " big items"));
        }
        return alerts.stream().limit(MAX_ALERTS).toList();
    }

    private static int highestReachedLevel(int level) {
        int reached = 0;
        for (int spikeLevel : SPIKE_LEVELS) {
            if (level >= spikeLevel) {
                reached = spikeLevel;
            }
        }
        return reached;
    }

    private static boolean isCompletedBigItem(Item item) {
        return item.goldValue() >= BIG_ITEM_GOLD && !item.hasAnyTag(ItemTag.COMPONENT, ItemTag.BOOTS, ItemTag.STARTER);
    }

    private static Alert spike(Player enemy, String suffix, String message) {
        return new Alert(AlertKind.SPIKE, "spike:" + enemy.championName() + ":" + suffix, message, AdvicePriority.RECOMMENDED);
    }
}
