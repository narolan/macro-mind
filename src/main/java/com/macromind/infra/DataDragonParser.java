package com.macromind.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.macromind.domain.model.ChampionBaseStats;
import com.macromind.domain.model.Item;
import com.macromind.domain.model.ItemStats;
import com.macromind.domain.model.ItemTag;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure parsing of Riot Data Dragon JSON. Malformed input yields empty results, never an exception. */
final class DataDragonParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ponytail: Data Dragon carries no gameplay tags, so tags come from this hand-kept table; extend per patch.
    private static final Map<String, Set<ItemTag>> TAGS_BY_ITEM_NAME = Map.ofEntries(
            Map.entry("Morellonomicon", EnumSet.of(ItemTag.GRIEVOUS_WOUNDS)),
            Map.entry("Mortal Reminder", EnumSet.of(ItemTag.GRIEVOUS_WOUNDS, ItemTag.ARMOR_PENETRATION_PERCENT)),
            Map.entry("Executioner's Calling", EnumSet.of(ItemTag.GRIEVOUS_WOUNDS)),
            Map.entry("Chempunk Chainsword", EnumSet.of(ItemTag.GRIEVOUS_WOUNDS)),
            Map.entry("Thornmail", EnumSet.of(ItemTag.GRIEVOUS_WOUNDS)),
            Map.entry("Bramble Vest", EnumSet.of(ItemTag.GRIEVOUS_WOUNDS)),
            Map.entry("Oblivion Orb", EnumSet.of(ItemTag.GRIEVOUS_WOUNDS)),
            Map.entry("Void Staff", EnumSet.of(ItemTag.MAGIC_PENETRATION_PERCENT)),
            Map.entry("Lord Dominik's Regards", EnumSet.of(ItemTag.ARMOR_PENETRATION_PERCENT)),
            Map.entry("Blade of The Ruined King", EnumSet.of(ItemTag.PERCENT_HEALTH_DAMAGE, ItemTag.ON_HIT)),
            Map.entry("Liandry's Torment", EnumSet.of(ItemTag.PERCENT_HEALTH_DAMAGE)),
            Map.entry("Banshee's Veil", EnumSet.of(ItemTag.SPELL_SHIELD)),
            Map.entry("Maw of Malmortius", EnumSet.of(ItemTag.BURST_RESISTANCE))
    );

    private DataDragonParser() {}

    static Map<String, ChampionBaseStats> parseChampions(String json) {
        Map<String, ChampionBaseStats> result = new HashMap<>();
        JsonNode data = dataNode(json);
        data.fields().forEachRemaining(entry -> {
            JsonNode s = entry.getValue().path("stats");
            result.put(entry.getKey(), new ChampionBaseStats(
                    s.path("hp").asDouble(), s.path("hpperlevel").asDouble(),
                    s.path("armor").asDouble(), s.path("armorperlevel").asDouble(),
                    s.path("spellblock").asDouble(), s.path("spellblockperlevel").asDouble(),
                    s.path("attackdamage").asDouble(), s.path("attackdamageperlevel").asDouble()));
        });
        return Map.copyOf(result);
    }

    static List<Item> parseItems(String json) {
        List<Item> result = new ArrayList<>();
        dataNode(json).fields().forEachRemaining(entry -> {
            JsonNode node = entry.getValue();
            String name = node.path("name").asText();
            JsonNode s = node.path("stats");
            ItemStats stats = new ItemStats(
                    s.path("FlatPhysicalDamageMod").asInt(), s.path("FlatMagicDamageMod").asInt(),
                    s.path("FlatArmorMod").asInt(), s.path("FlatSpellBlockMod").asInt(),
                    s.path("FlatHPPoolMod").asInt(), s.path("FlatMPPoolMod").asInt(),
                    0, 0,
                    s.path("PercentAttackSpeedMod").asDouble(), 0,
                    s.path("PercentLifeStealMod").asDouble() * 100,
                    s.path("FlatCritChanceMod").asDouble());
            result.add(new Item(Integer.parseInt(entry.getKey()), name, node.path("gold").path("total").asInt(),
                    stats, TAGS_BY_ITEM_NAME.getOrDefault(name, Set.of())));
        });
        return List.copyOf(result);
    }

    private static JsonNode dataNode(String json) {
        try {
            return MAPPER.readTree(json).path("data");
        } catch (Exception e) {
            return MAPPER.createObjectNode();
        }
    }
}
