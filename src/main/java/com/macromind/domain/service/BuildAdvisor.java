package com.macromind.domain.service;

import com.macromind.domain.model.*;

import java.util.ArrayList;
import java.util.List;

public record BuildAdvisor(
        GameState gameState,
        List<Item> possibleItems
) {

    public List<BuildAdvice> generateBuildAdvices() {
        List<BuildAdvice> advices = new ArrayList<>();
        Player activePlayer = gameState.activePlayer();
        if(gameState.hasEnemyWithHeavyHealing() && !activePlayer.hasGrievousWoundsItem()) {
            advices.addAll(generateAntiHealAdvice());
        }
        if((gameState.getHighestArmorEnemy() - activePlayer.getFlatArmorPen()) > 150) {
            advices.addAll(generateAntiArmorAdvice());
        }
        if((gameState.getHighestMagicResistEnemy() - activePlayer.getFlatMagicPen()) > 150) {
            advices.addAll(generateAntiMagicResistAdvice());
        }
        if(gameState.getHighestHpEnemy() > 3500) {
            if(gameState.getHighestHpEnemy() > 5000) {
                advices.addAll(generatePercentHealthDamageAdvice(AdvicePriority.CRITICAL));
            } else {
                advices.addAll(generatePercentHealthDamageAdvice(AdvicePriority.RECOMMENDED));
            }
        }
        if(gameState.getEnemyAdRatio() > 0.7) {
            advices.addAll(generateHeavyAdEnemyThreatAdvice(activePlayer));
        }
        if(gameState.getEnemyAdRatio() < 0.3) {
            advices.addAll(generateHeavyApEnemyThreatAdvice(activePlayer));
        }
        return advices;
    }

    private List<BuildAdvice> generateHeavyAdEnemyThreatAdvice(Player activePlayer) {
        return switch(activePlayer.championClass()) {
            case MARKSMAN -> {
                if(!gameState.isActivePlayerSquishy()) {
                    yield List.of();
                }
                yield possibleItems().stream()
                    .filter(item -> item.stats().attackDamage() > 0 || item.stats().attackSpeed() > 0 || item.stats().criticalStrikeChance() > 0)
                    .filter(item -> item.stats().armor() > 0 || item.tags().contains(ItemTag.BURST_RESISTANCE) || item.tags().contains(ItemTag.SPELL_SHIELD))
                    .map(item -> new BuildAdvice(
                            item,
                            "Enemy team has heavy AD threat. Consider buying this item to survive their damage.",
                            AdvicePriority.CRITICAL,
                            AdviceCategory.DEFENSIVE,
                            gameState.highestAdThreat(),
                            gameState.gameTimeInSeconds()
                    ))
                    .toList();
            }
            case MAGE -> {
                if(!gameState.isActivePlayerSquishy()) {
                    yield List.of();
                }
                yield possibleItems().stream()
                        .filter(item -> item.stats().abilityPower() > 0 || item.stats().cooldownReduction() > 0)
                        .filter(item -> item.stats().armor() > 0 || item.tags().contains(ItemTag.SPELL_SHIELD) || item.tags().contains(ItemTag.BURST_RESISTANCE))
                        .map(item -> new BuildAdvice(
                                item,
                                "Enemy team has heavy AD threat. Consider buying this item to survive their damage.",
                                AdvicePriority.CRITICAL,
                                AdviceCategory.DEFENSIVE,
                                gameState.highestAdThreat(),
                                gameState.gameTimeInSeconds()
                        ))
                        .toList();
            }
            case TANK -> possibleItems().stream()
                    .filter(item -> item.stats().health() > 0 || item.stats().armor() > 0)
                    .map(item -> new BuildAdvice(
                            item,
                            "Enemy team has heavy AD threat. Consider buying this item to survive their damage.",
                            AdvicePriority.RECOMMENDED,
                            AdviceCategory.DEFENSIVE,
                            gameState.highestAdThreat(),
                            gameState.gameTimeInSeconds()
                    ))
                    .toList();
            case BRUISER -> {
                if(gameState().isActivePlayerSquishy()) {
                    yield possibleItems().stream()
                            .filter(item -> item.stats().armor() > 0)
                            .filter(item -> item.stats().health() > 0 || item.stats().attackDamage() > 0 || item.tags().contains(ItemTag.BURST_RESISTANCE))
                            .map(item -> new BuildAdvice(
                                    item,
                                    "Enemy team has heavy AD threat. Consider buying this item to survive their damage.",
                                    AdvicePriority.RECOMMENDED,
                                    AdviceCategory.DEFENSIVE,
                                    gameState.highestAdThreat(),
                                    gameState.gameTimeInSeconds()
                            ))
                            .toList();
                }
                yield List.of();
            }
            case ASSASSIN -> {
                if(activePlayer.stats().attackDamage() > 150) {
                    yield possibleItems().stream()
                            .filter(item -> item.name().equals("Guardian Angel"))
                            .map(item -> new BuildAdvice(
                                    item,
                                    "Enemy team has heavy AD threat. Consider buying this item to survive their damage.",
                                    AdvicePriority.RECOMMENDED,
                                    AdviceCategory.DEFENSIVE,
                                    gameState.highestAdThreat(),
                                    gameState.gameTimeInSeconds()
                            ))
                            .toList();
                } else if(activePlayer.stats().abilityPower() > 150) {
                    yield possibleItems().stream()
                            .filter(item -> item.name().equals("Zhonya's Hourglass"))
                            .map(item -> new BuildAdvice(
                                    item,
                                    "Enemy team has heavy AD threat. Consider buying this item to survive their damage.",
                                    AdvicePriority.RECOMMENDED,
                                    AdviceCategory.DEFENSIVE,
                                    gameState.highestAdThreat(),
                                    gameState.gameTimeInSeconds()
                            ))
                            .toList();
                }
                yield List.of();
            }
            case SUPPORT -> List.of();
        };
    }

    private List<BuildAdvice> generateHeavyApEnemyThreatAdvice(Player activePlayer) {
        return switch (activePlayer.championClass()) {
            case MARKSMAN -> {
                if(!gameState.isActivePlayerSquishy()) {
                    yield List.of();
                }
                yield possibleItems().stream()
                        .filter(item -> item.stats().attackDamage() > 0 || item.stats().attackSpeed() > 0 || item.stats().criticalStrikeChance() > 0)
                        .filter(item -> item.stats().magicResist() > 0 || item.tags().contains(ItemTag.SPELL_SHIELD) || item.tags().contains(ItemTag.BURST_RESISTANCE))
                        .map(item -> new BuildAdvice(
                                item,
                                "Enemy team has heavy AP threat. Consider buying this item to survive their damage.",
                                AdvicePriority.CRITICAL,
                                AdviceCategory.DEFENSIVE,
                                gameState.highestApThreat(),
                                gameState.gameTimeInSeconds()
                        ))
                        .toList();
            }
            case MAGE -> {
                if(!gameState.isActivePlayerSquishy()) {
                    yield List.of();
                }
                yield possibleItems().stream()
                        .filter(item -> item.stats().abilityPower() > 0 || item.stats().cooldownReduction() > 0)
                        .filter(item -> item.stats().magicResist() > 0 || item.tags().contains(ItemTag.SPELL_SHIELD) || item.tags().contains(ItemTag.BURST_RESISTANCE))
                        .map(item -> new BuildAdvice(
                                item,
                                "Enemy team has heavy AP threat. Consider buying this item to survive their damage.",
                                AdvicePriority.CRITICAL,
                                AdviceCategory.DEFENSIVE,
                                gameState.highestApThreat(),
                                gameState.gameTimeInSeconds()
                        ))
                        .toList();
            }
            case TANK -> possibleItems().stream()
                    .filter(item -> item.stats().health() > 0 || item.stats().magicResist() > 0)
                    .map(item -> new BuildAdvice(
                            item,
                            "Enemy team has heavy AP threat. Consider buying this item to survive their damage.",
                            AdvicePriority.RECOMMENDED,
                            AdviceCategory.DEFENSIVE,
                            gameState.highestApThreat(),
                            gameState.gameTimeInSeconds()
                    ))
                    .toList();
            case BRUISER -> {
                if(gameState().isActivePlayerSquishy()) {
                    yield possibleItems().stream()
                            .filter(item -> item.stats().magicResist() > 0)
                            .filter(item -> item.stats().health() > 0 || item.stats().attackDamage() > 0 || item.tags().contains(ItemTag.BURST_RESISTANCE))
                            .map(item -> new BuildAdvice(
                                    item,
                                    "Enemy team has heavy AP threat. Consider buying this item to survive their damage.",
                                    AdvicePriority.RECOMMENDED,
                                    AdviceCategory.DEFENSIVE,
                                    gameState.highestApThreat(),
                                    gameState.gameTimeInSeconds()
                            ))
                            .toList();
                }
                yield List.of();
            }
            case ASSASSIN -> {
                if(activePlayer.stats().attackDamage() > 150) {
                    yield possibleItems().stream()
                            .filter(item -> item.name().equals("Guardian Angel"))
                            .map(item -> new BuildAdvice(
                                    item,
                                    "Enemy team has heavy AP threat. Consider buying this item to survive their damage.",
                                    AdvicePriority.RECOMMENDED,
                                    AdviceCategory.DEFENSIVE,
                                    gameState.highestApThreat(),
                                    gameState.gameTimeInSeconds()
                            ))
                            .toList();
                } else if(activePlayer.stats().abilityPower() > 150) {
                    yield possibleItems().stream()
                            .filter(item -> item.name().equals("Banshee's Veil") || item.name().equals("Zhonya's Hourglass"))
                            .map(item -> new BuildAdvice(
                                    item,
                                    "Enemy team has heavy AP threat. Consider buying this item to survive their damage.",
                                    AdvicePriority.RECOMMENDED,
                                    AdviceCategory.DEFENSIVE,
                                    gameState.highestApThreat(),
                                    gameState.gameTimeInSeconds()
                            ))
                            .toList();
                }
                yield List.of();
            }
            case SUPPORT -> List.of();
        };
    }

    private List<BuildAdvice> generateAntiHealAdvice() {
        return possibleItems().stream()
                .filter(item -> item.tags().contains(ItemTag.GRIEVOUS_WOUNDS))
                .map(item -> new BuildAdvice(
                        item,
                        "Enemy team has heavy healing. Consider buying this item to apply Grievous Wounds.",
                        AdvicePriority.CRITICAL,
                        AdviceCategory.UTILITY,
                        gameState.enemies().stream()
                                .filter(enemy -> enemy.stats().healthRegen() > 100 || enemy.stats().lifesteal() + enemy.stats().omnivamp() > 15)
                                .findFirst()
                                .orElse(null),
                        gameState.gameTimeInSeconds()
                ))
                .toList();
    }

    private List<BuildAdvice> generateAntiArmorAdvice() {
        return possibleItems.stream()
                .filter(item -> item.tags().contains(ItemTag.ARMOR_PENETRATION_PERCENT))
                .map(item -> new BuildAdvice(
                        item,
                        "Enemy team has high armor. Consider buying this item to penetrate their defenses.",
                        AdvicePriority.RECOMMENDED,
                        AdviceCategory.OFFENSIVE,
                        gameState.enemies().stream()
                                .filter(enemy -> enemy.stats().armor() > 150)
                                .findFirst()
                                .orElse(null),
                        gameState.gameTimeInSeconds()
                ))
                .toList();
    }

    private List<BuildAdvice> generateAntiMagicResistAdvice() {
        return possibleItems.stream()
                .filter(item -> item.tags().contains(ItemTag.MAGIC_PENETRATION_PERCENT))
                .map(item -> new BuildAdvice(
                        item,
                        "Enemy team has high magic resistance. Consider buying this item to penetrate their defenses.",
                        AdvicePriority.RECOMMENDED,
                        AdviceCategory.OFFENSIVE,
                        gameState.enemies().stream()
                                .filter(enemy -> enemy.stats().magicResist() > 150)
                                .findFirst()
                                .orElse(null),
                        gameState.gameTimeInSeconds()
                ))
                .toList();
    }

    private List<BuildAdvice> generatePercentHealthDamageAdvice(AdvicePriority advicePriority) {
        return possibleItems.stream()
                .filter(item -> item.tags().contains(ItemTag.PERCENT_HEALTH_DAMAGE))
                .map(item -> new BuildAdvice(
                        item,
                        "Enemy team has high health. Consider buying this item to deal percentage health damage.",
                        advicePriority,
                        AdviceCategory.OFFENSIVE,
                        gameState.enemies().stream()
                                .filter(enemy -> enemy.stats().maxHealth() > 3500)
                                .findFirst()
                                .orElse(null),
                        gameState.gameTimeInSeconds()
                ))
                .toList();
    }

}
