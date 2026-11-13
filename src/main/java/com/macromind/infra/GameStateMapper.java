package com.macromind.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.macromind.domain.model.*;
import com.macromind.domain.service.StatEstimator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Maps a Live Client /allgamedata response to a GameState. The active player is exact, everyone else is estimated. */
public class GameStateMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final StatEstimator estimator;
    private final Map<Integer, Item> catalogue;
    private final Function<String, ChampionClass> classOf;

    public GameStateMapper(StatEstimator estimator, List<Item> catalogue, Function<String, ChampionClass> classOf) {
        this.estimator = estimator;
        this.catalogue = catalogue.stream().collect(Collectors.toMap(Item::id, i -> i, (a, b) -> a));
        this.classOf = classOf;
    }

    public GameState toGameState(String allGameDataJson) {
        try {
            return map(MAPPER.readTree(allGameDataJson));
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Unreadable live client response", e);
        }
    }

    private GameState map(JsonNode root) {
        JsonNode active = root.path("activePlayer");
        String activeId = firstNonEmpty(active.path("riotId").asText(), active.path("summonerName").asText());
        JsonNode activeEntry = null;
        for (JsonNode p : root.path("allPlayers")) {
            if (identity(p).equals(activeId)) {
                activeEntry = p;
            }
        }
        String activeTeam = activeEntry == null ? "" : activeEntry.path("team").asText();

        List<Player> allies = new ArrayList<>();
        List<Player> enemies = new ArrayList<>();
        for (JsonNode p : root.path("allPlayers")) {
            if (p == activeEntry) {
                continue;
            }
            boolean isEnemy = !activeTeam.isEmpty() && !p.path("team").asText().equals(activeTeam);
            if (activeTeam.isEmpty()) {
                continue;
            }
            (isEnemy ? enemies : allies).add(estimatedPlayer(p, isEnemy));
        }
        return new GameState((long) root.path("gameData").path("gameTime").asDouble(),
                activePlayer(active, activeEntry), List.copyOf(allies), List.copyOf(enemies), GameMode.SUMMONERS_RIFT);
    }

    private Player activePlayer(JsonNode active, JsonNode entry) {
        JsonNode s = active.path("championStats");
        PlayerStats stats = new PlayerStats(
                (int) s.path("armor").asDouble(), (int) s.path("magicResist").asDouble(),
                (int) s.path("maxHealth").asDouble(), (int) s.path("currentHealth").asDouble(),
                (int) s.path("attackDamage").asDouble(), (int) s.path("abilityPower").asDouble(),
                (int) s.path("lifeSteal").asDouble(), (int) s.path("spellVamp").asDouble(),
                (int) s.path("healthRegenRate").asDouble());
        JsonNode e = entry == null ? MAPPER.createObjectNode() : entry;
        String champion = e.path("championName").asText();
        return new Player(identity(active), champion, classOf.apply(champion), active.path("level").asInt(),
                false, items(e), stats, role(e), kda(e), (int) active.path("currentGold").asDouble(),
                false, e.path("isDead").asBoolean());
    }

    private Player estimatedPlayer(JsonNode p, boolean isEnemy) {
        List<Item> items = items(p);
        String champion = p.path("championName").asText();
        int level = p.path("level").asInt(1);
        int gold = 0;
        for (JsonNode i : p.path("items")) {
            gold += i.path("price").asInt() * Math.max(1, i.path("count").asInt(1));
        }
        return new Player(identity(p), champion, classOf.apply(champion), level, isEnemy, items,
                estimator.estimate(champion, level, items), role(p), kda(p), gold, true,
                p.path("isDead").asBoolean());
    }

    private List<Item> items(JsonNode player) {
        List<Item> items = new ArrayList<>();
        for (JsonNode i : player.path("items")) {
            int id = i.path("itemID").asInt();
            items.add(catalogue.getOrDefault(id, new Item(id, i.path("displayName").asText(),
                    i.path("price").asInt(), new ItemStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), Set.of())));
        }
        return List.copyOf(items);
    }

    private static KDA kda(JsonNode player) {
        JsonNode s = player.path("scores");
        return new KDA(s.path("kills").asInt(), s.path("deaths").asInt(), s.path("assists").asInt());
    }

    private static Role role(JsonNode player) {
        return switch (player.path("position").asText()) {
            case "TOP" -> Role.TOP;
            case "JUNGLE" -> Role.JUNGLE;
            case "BOTTOM" -> Role.ADC;
            case "UTILITY" -> Role.SUPPORT;
            default -> Role.MID;
        };
    }

    private static String identity(JsonNode p) {
        return firstNonEmpty(p.path("riotId").asText(), p.path("summonerName").asText());
    }

    private static String firstNonEmpty(String a, String b) {
        return a.isEmpty() ? b : a;
    }
}
