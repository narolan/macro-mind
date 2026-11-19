package com.macromind.domain.service;

import com.macromind.domain.model.*;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Lowest-priority (SITUATIONAL) class-fit advice; the ranker places it below threat and composition advice.
 * Stops once the build is complete.
 */
final class CoreItemRules {

    static final int MAX_ITEM_SLOTS = 6;

    private final GameState gameState;
    private final AdviceFactory factory;
    private final Player me;

    CoreItemRules(GameState gameState, AdviceFactory factory) {
        this.gameState = gameState;
        this.factory = factory;
        this.me = gameState.activePlayer();
    }

    List<BuildAdvice> advise() {
        List<BuildAdvice> boots = defaultBoots();
        if (gameState.enemies().isEmpty() || gameState.gameTimeInSeconds() < AdviceThresholds.MID_GAME_SECONDS || isBuildComplete()) {
            return boots;
        }
        List<BuildAdvice> classAdvice = factory.create(classFit(), reasoning(),
                AdvicePriority.SITUATIONAL, categoryFor(), null);
        return Stream.concat(boots.stream(), classAdvice.stream()).toList();
    }

    private boolean isBuildComplete() {
        long finished = me.items().stream().filter(i -> !i.hasTag(ItemTag.BOOTS)).count();
        return finished >= MAX_ITEM_SLOTS - 1;
    }

    private List<BuildAdvice> defaultBoots() {
        if (me.hasBoots() || gameState.gameTimeInSeconds() < AdviceThresholds.BOOTS_MIN_SECONDS) {
            return List.of();
        }
        return factory.create(i -> i.hasTag(ItemTag.BOOTS) && fitsBootsFor(i),
                "You have no boots yet. These suit your champion.",
                AdvicePriority.SITUATIONAL, AdviceCategory.UTILITY, null);
    }

    // Marksmen want attack speed boots, magic users flat magic penetration; everyone else takes haste.
    private boolean fitsBootsFor(Item boots) {
        return switch (me.championClass()) {
            case MARKSMAN -> boots.stats().attackSpeed() > 0;
            case MAGE -> boots.hasTag(ItemTag.MAGIC_PENETRATION_FLAT);
            default -> boots.stats().cooldownReduction() > 0;
        };
    }

    private Predicate<Item> classFit() {
        // boots are advised by their own rules
        return ((Predicate<Item>) i -> !i.hasTag(ItemTag.BOOTS)).and(switch (me.championClass()) {
            case MARKSMAN -> ItemFit::givesPhysicalDamage;
            case MAGE -> ItemFit::givesAbilityPower;
            case TANK -> ItemFit::isTankItem;
            case BRUISER -> i -> (i.stats().attackDamage() > 0 && i.stats().health() > 0) || i.stats().health() >= 300;
            case ASSASSIN -> i -> me.dealsMagicDamage() ? i.stats().abilityPower() > 0 : i.stats().attackDamage() > 0;
            case SUPPORT -> i -> i.hasTag(ItemTag.ENCHANTER);
        });
    }

    private String reasoning() {
        return "Nothing urgent to counter. This item fits a " + me.championClass().name().toLowerCase() + " build.";
    }

    private AdviceCategory categoryFor() {
        return switch (me.championClass()) {
            case MARKSMAN, MAGE, ASSASSIN -> AdviceCategory.OFFENSIVE;
            case TANK -> AdviceCategory.DEFENSIVE;
            case BRUISER, SUPPORT -> AdviceCategory.UTILITY;
        };
    }
}
