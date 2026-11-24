package com.macromind.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.macromind.domain.model.ChampionClass;
import com.macromind.domain.model.GameMode;
import com.macromind.domain.model.GameState;
import com.macromind.domain.model.Item;
import com.macromind.domain.model.ItemStats;
import com.macromind.domain.model.KDA;
import com.macromind.domain.model.Player;
import com.macromind.domain.model.PlayerStats;
import com.macromind.domain.model.Role;
import com.macromind.domain.service.StatEstimator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/** Maps a Live Client /allgamedata response to a GameState. The active player is exact, everyone else is estimated. */
public final class GameStateMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Field names in the Live Client JSON.
    private static final String ACTIVE_PLAYER = "activePlayer";
    private static final String ALL_PLAYERS = "allPlayers";
    private static final String CHAMPION_STATS = "championStats";
    private static final String CHAMPION_NAME = "championName";
    private static final String TEAM = "team";
    private static final String IS_DEAD = "isDead";
    private static final String RIOT_ID = "riotId";
    private static final String RIOT_ID_GAME_NAME = "riotIdGameName";
    private static final String SUMMONER_NAME = "summonerName";
    private static final List<String> IDENTITY_FIELDS = List.of(RIOT_ID, RIOT_ID_GAME_NAME, SUMMONER_NAME);

    private final StatEstimator estimator;
    private final Map<Integer, Item> catalogue;
    private final Function<String, ChampionClass> classOf;

    public GameStateMapper(StatEstimator estimator, List<Item> catalogue, Function<String, ChampionClass> classOf) {
        this.estimator = estimator;
        this.catalogue = catalogue.stream().collect(Collectors.toUnmodifiableMap(Item::id, Function.identity(), (a, b) -> a));
        this.classOf = classOf;
    }

    public GameState toGameState(String allGameDataJson) {
        try {
            return map(MAPPER.readTree(allGameDataJson));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unreadable live client response", e);
        }
    }

    private GameState map(JsonNode root) {
        JsonNode active = root.path(ACTIVE_PLAYER);
        long gameTime = (long) root.path("gameData").path("gameTime").asDouble();
        GameMode gameMode = GameMode.fromMapNumber(root.path("gameData").path("mapNumber").asInt());
        Optional<JsonNode> activeEntry = findActiveEntry(root.path(ALL_PLAYERS), identities(active));
        Player activePlayer = activePlayer(active, activeEntry.orElse(null));

        // Without the active player's team there is no way to tell allies from enemies, so none are guessed.
        String activeTeam = activeEntry.map(entry -> entry.path(TEAM).asText()).orElse("");
        if (activeTeam.isEmpty()) {
            return new GameState(gameTime, activePlayer, List.of(), List.of(), gameMode);
        }

        List<Player> allies = new ArrayList<>();
        List<Player> enemies = new ArrayList<>();
        for (JsonNode other : root.path(ALL_PLAYERS)) {
            if (other == activeEntry.get()) {
                continue;
            }
            boolean isEnemy = !other.path(TEAM).asText().equals(activeTeam);
            (isEnemy ? enemies : allies).add(estimatedPlayer(other, isEnemy));
        }
        return new GameState(gameTime, activePlayer, allies, enemies, gameMode);
    }

    private static Optional<JsonNode> findActiveEntry(JsonNode allPlayers, Set<String> activeIds) {
        return StreamSupport.stream(allPlayers.spliterator(), false)
                .filter(player -> identities(player).stream().anyMatch(activeIds::contains))
                .findFirst();
    }

    private Player activePlayer(JsonNode active, JsonNode entry) {
        JsonNode s = active.path(CHAMPION_STATS);
        PlayerStats stats = new PlayerStats(
                intStat(s, "armor"), intStat(s, "magicResist"), intStat(s, "maxHealth"), intStat(s, "currentHealth"),
                intStat(s, "attackDamage"), intStat(s, "abilityPower"), intStat(s, "lifeSteal"), intStat(s, "spellVamp"),
                intStat(s, "healthRegenRate"),
                (int) (s.path("armorPenetrationFlat").asDouble() + s.path("physicalLethality").asDouble()),
                (int) (s.path("magicPenetrationFlat").asDouble() + s.path("magicLethality").asDouble()));
        JsonNode e = entry == null ? MAPPER.createObjectNode() : entry;
        String champion = e.path(CHAMPION_NAME).asText();
        return new Player(identity(active), champion, classOf.apply(champion), active.path("level").asInt(),
                false, items(e), stats, role(e), kda(e), intStat(active, "currentGold"),
                false, e.path(IS_DEAD).asBoolean());
    }

    private Player estimatedPlayer(JsonNode p, boolean isEnemy) {
        List<Item> items = items(p);
        String champion = p.path(CHAMPION_NAME).asText();
        int level = p.path("level").asInt(1);
        // items[].price is the cost of the last purchase step (Trinity Force reads 183), so the catalogue total is used
        int gold = items.stream().mapToInt(Item::goldValue).sum();
        return new Player(identity(p), champion, classOf.apply(champion), level, isEnemy, items,
                estimator.estimate(champion, level, items), role(p), kda(p), gold, true,
                p.path(IS_DEAD).asBoolean());
    }

    private List<Item> items(JsonNode player) {
        List<Item> items = new ArrayList<>();
        for (JsonNode i : player.path("items")) {
            int id = i.path("itemID").asInt();
            items.add(catalogue.getOrDefault(id, unknownItem(id, i.path("displayName").asText())));
        }
        return items;
    }

    private static Item unknownItem(int id, String name) {
        return new Item(id, name, 0, ItemStats.NONE, Set.of());
    }

    private static int intStat(JsonNode stats, String field) {
        return (int) stats.path(field).asDouble();
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
        String riotId = p.path(RIOT_ID).asText();
        return riotId.isEmpty() ? p.path(SUMMONER_NAME).asText() : riotId;
    }

    // The riotId format differs between endpoints, so match on every identity either side offers.
    private static Set<String> identities(JsonNode p) {
        Set<String> ids = new HashSet<>();
        for (String field : IDENTITY_FIELDS) {
            String value = p.path(field).asText();
            if (!value.isEmpty()) {
                ids.add(value);
                // activePlayer reports "Name#TAG" while allPlayers reports just "Name"
                ids.add(value.contains("#") ? value.substring(0, value.indexOf('#')) : value);
            }
        }
        return Set.copyOf(ids);
    }
}
