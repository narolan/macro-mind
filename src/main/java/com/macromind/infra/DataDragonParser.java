package com.macromind.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.macromind.domain.model.ChampionBaseStats;
import com.macromind.domain.model.ChampionClass;
import com.macromind.domain.model.Item;
import com.macromind.domain.model.ItemStats;
import com.macromind.domain.model.ItemTag;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        data.properties().forEach(entry -> {
            JsonNode s = entry.getValue().path("stats");
            ChampionBaseStats stats = new ChampionBaseStats(
                    s.path("hp").asDouble(), s.path("hpperlevel").asDouble(),
                    s.path("armor").asDouble(), s.path("armorperlevel").asDouble(),
                    s.path("spellblock").asDouble(), s.path("spellblockperlevel").asDouble(),
                    s.path("attackdamage").asDouble(), s.path("attackdamageperlevel").asDouble());
            // The live client reports display names ("Miss Fortune"); Data Dragon keys by id ("MissFortune").
            result.put(entry.getKey(), stats);
            result.put(entry.getValue().path("name").asText(entry.getKey()), stats);
        });
        return Map.copyOf(result);
    }

    static Map<String, ChampionClass> parseChampionClasses(String json) {
        Map<String, ChampionClass> result = new HashMap<>();
        dataNode(json).properties().forEach(entry -> {
            String firstTag = entry.getValue().path("tags").path(0).asText();
            ChampionClass championClass = switch (firstTag) {
                case "Marksman" -> ChampionClass.MARKSMAN;
                case "Mage" -> ChampionClass.MAGE;
                case "Tank" -> ChampionClass.TANK;
                case "Fighter" -> ChampionClass.BRUISER;
                case "Assassin" -> ChampionClass.ASSASSIN;
                case "Support" -> ChampionClass.SUPPORT;
                default -> null;
            };
            if (championClass != null) {
                result.put(entry.getKey(), championClass);
                result.put(entry.getValue().path("name").asText(entry.getKey()), championClass);
            }
        });
        return Map.copyOf(result);
    }

    static List<Item> parseItems(String json) {
        List<Item> result = new ArrayList<>();
        dataNode(json).properties().forEach(entry -> {
            JsonNode node = entry.getValue();
            String name = node.path("name").asText();
            if (!isBuyableOnSummonersRift(node, name)) {
                return;
            }
            JsonNode s = node.path("stats");
            ItemStats stats = new ItemStats(
                    s.path("FlatPhysicalDamageMod").asInt(), s.path("FlatMagicDamageMod").asInt(),
                    s.path("FlatArmorMod").asInt(), s.path("FlatSpellBlockMod").asInt(),
                    s.path("FlatHPPoolMod").asInt(), s.path("FlatMPPoolMod").asInt(),
                    0, 0,
                    s.path("PercentAttackSpeedMod").asDouble(), 0,
                    s.path("PercentLifeStealMod").asDouble() * 100,
                    s.path("FlatCritChanceMod").asDouble(),
                    omnivamp(node.path("description").asText()));
            result.add(new Item(Integer.parseInt(entry.getKey()), name, node.path("gold").path("total").asInt(),
                    stats, TAGS_BY_ITEM_NAME.getOrDefault(name, Set.of())));
        });
        return List.copyOf(result);
    }

    private static final Pattern OMNIVAMP = Pattern.compile("(\\d+)%\\s*Omnivamp");

    // Omnivamp has no stat key in Data Dragon; it only appears in the description text ("10% Omnivamp").
    private static int omnivamp(String description) {
        Matcher matcher = OMNIVAMP.matcher(description.replaceAll("<[^>]*>", " "));
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
        } catch (Exception e) {
            return MAPPER.createObjectNode();
        }
    }
}
