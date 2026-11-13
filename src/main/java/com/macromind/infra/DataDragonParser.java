package com.macromind.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.macromind.domain.model.ChampionBaseStats;
import com.macromind.domain.model.ChampionClass;
import com.macromind.domain.model.Item;
import com.macromind.domain.model.ItemStats;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure parsing of Riot Data Dragon JSON. Malformed input yields empty results, never an exception. */
final class DataDragonParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> NOT_ADVISEABLE_TAGS = List.of("Consumable", "Trinket", "Vision", "Jungle");
    private static final Map<String, ChampionClass> CLASS_BY_RIOT_TAG = Map.of(
            "Marksman", ChampionClass.MARKSMAN, "Mage", ChampionClass.MAGE, "Tank", ChampionClass.TANK,
            "Fighter", ChampionClass.BRUISER, "Assassin", ChampionClass.ASSASSIN, "Support", ChampionClass.SUPPORT);

    private DataDragonParser() {}

    static Map<String, ChampionBaseStats> parseChampions(String json) {
        Map<String, ChampionBaseStats> result = new HashMap<>();
        dataNode(json).properties().forEach(entry -> {
            JsonNode s = entry.getValue().path("stats");
            putByIdAndName(result, entry, new ChampionBaseStats(
                    s.path("hp").asDouble(), s.path("hpperlevel").asDouble(),
                    s.path("armor").asDouble(), s.path("armorperlevel").asDouble(),
                    s.path("spellblock").asDouble(), s.path("spellblockperlevel").asDouble(),
                    s.path("attackdamage").asDouble(), s.path("attackdamageperlevel").asDouble()));
        });
        return Map.copyOf(result);
    }

    static Map<String, ChampionClass> parseChampionClasses(String json) {
        Map<String, ChampionClass> result = new HashMap<>();
        dataNode(json).properties().forEach(entry -> {
            ChampionClass championClass = CLASS_BY_RIOT_TAG.get(entry.getValue().path("tags").path(0).asText());
            if (championClass == null) {
                return;
            }
            putByIdAndName(result, entry, championClass);
        });
        return Map.copyOf(result);
    }

    // The live client reports display names ("Miss Fortune"); Data Dragon keys by id ("MissFortune"). Index both.
    private static <T> void putByIdAndName(Map<String, T> result, Map.Entry<String, JsonNode> entry, T value) {
        result.put(entry.getKey(), value);
        result.put(entry.getValue().path("name").asText(entry.getKey()), value);
    }

    // Several modes ship a copy of the same item under another id; the lowest id is the Summoner's Rift one.
    static List<Item> parseItems(String json) {
        Map<String, Item> byName = new LinkedHashMap<>();
        dataNode(json).properties().stream()
                .sorted(java.util.Comparator.comparingInt(e -> idOrMax(e.getKey())))
                .forEach(entry -> {
                    JsonNode node = entry.getValue();
                    String name = node.path("name").asText();
                    if (isBuyableOnSummonersRift(node, name) && isAdviseable(node)) {
                        byName.putIfAbsent(name, toItem(entry.getKey(), node, name));
                    }
                });
        return List.copyOf(byName.values());
    }

    private static int idOrMax(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private static boolean isAdviseable(JsonNode node) {
        for (JsonNode tag : node.path("tags")) {
            if (NOT_ADVISEABLE_TAGS.contains(tag.asText())) {
                return false;
            }
        }
        return true;
    }

    private static Item toItem(String id, JsonNode node, String name) {
        String text = plainText(node.path("description").asText());
        JsonNode s = node.path("stats");
        ItemStats stats = new ItemStats(
                s.path("FlatPhysicalDamageMod").asInt(), s.path("FlatMagicDamageMod").asInt(),
                s.path("FlatArmorMod").asInt(), s.path("FlatSpellBlockMod").asInt(),
                s.path("FlatHPPoolMod").asInt(), s.path("FlatMPPoolMod").asInt(),
                firstInt(ItemTagger.LETHALITY, text), firstInt(ItemTagger.FLAT_MAGIC_PEN, text),
                s.path("PercentAttackSpeedMod").asDouble(), firstInt(ItemTagger.ABILITY_HASTE, text),
                s.path("PercentLifeStealMod").asDouble() * 100,
                s.path("FlatCritChanceMod").asDouble(),
                firstInt(ItemTagger.OMNIVAMP, text));
        int gold = node.path("gold").path("total").asInt();
        return new Item(Integer.parseInt(id), name, gold, stats, ItemTagger.tagsFor(node, text, name, gold));
    }

    private static String plainText(String description) {
        return description.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ");
    }

    private static int firstInt(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    // Real item.json holds Arena/ARAM/event copies and unbuyable entries; only these can be advised.
    private static boolean isBuyableOnSummonersRift(JsonNode node, String name) {
        return !name.isEmpty()
                && node.path("maps").path("11").asBoolean(false)
                && node.path("gold").path("purchasable").asBoolean(false)
                && node.path("inStore").asBoolean(true)
                && !node.has("requiredChampion")
                && !node.has("requiredAlly");
    }

    private static JsonNode dataNode(String json) {
        try {
            return MAPPER.readTree(json).path("data");
        } catch (JsonProcessingException | IllegalArgumentException e) {
            return MAPPER.createObjectNode();
        }
    }
}
