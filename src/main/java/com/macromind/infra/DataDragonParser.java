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
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure parsing of Riot Data Dragon JSON. Malformed input yields empty results, never an exception. */
final class DataDragonParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    private static final Pattern LETHALITY = Pattern.compile("(\\d+)\\s*Lethality");
    private static final Pattern FLAT_MAGIC_PEN = Pattern.compile("(?<![\\d%])(\\d+)\\s*Magic Penetration");
    private static final Pattern PERCENT_MAGIC_PEN = Pattern.compile("\\d+%\\s*Magic Penetration");
    private static final Pattern PERCENT_ARMOR_PEN = Pattern.compile("\\d+%\\s*(bonus\\s*)?Armor Penetration");
    private static final Pattern ABILITY_HASTE = Pattern.compile("(\\d+)\\s*Ability Haste");
    private static final Pattern PERCENT_HEALTH_HIT = Pattern.compile(
            "(max(imum)?|current|missing) Health[^.]{0,40}(damage|bonus)|(damage|bonus|burn)[^.]{0,60}\\d+%[^.]{0,25}(max(imum)?|current|missing) Health"
                    + "|\\d+%[^.]{0,25}(max(imum)?|current|missing) Health", Pattern.CASE_INSENSITIVE);
    private static final int STARTER_MAX_GOLD = 500;
    private static final List<String> NOT_ADVISEABLE_TAGS = List.of("Consumable", "Trinket", "Vision", "Jungle");

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
                firstInt(LETHALITY, text), firstInt(FLAT_MAGIC_PEN, text),
                s.path("PercentAttackSpeedMod").asDouble(), firstInt(ABILITY_HASTE, text),
                s.path("PercentLifeStealMod").asDouble() * 100,
                s.path("FlatCritChanceMod").asDouble(),
                omnivamp(node.path("description").asText()));
        int gold = node.path("gold").path("total").asInt();
        return new Item(Integer.parseInt(id), name, gold, stats, tagsFor(node, text, gold));
    }

    // Riot's description text is the only place item effects are written down, so tags are read from it.
    private static Set<ItemTag> tagsFor(JsonNode node, String text, int gold) {
        String name = node.path("name").asText();
        Set<ItemTag> tags = EnumSet.noneOf(ItemTag.class);
        List<String> riotTags = new ArrayList<>();
        node.path("tags").forEach(t -> riotTags.add(t.asText()));
        if (text.matches("(?s).*\\bWounds\\b.*")) {
            tags.add(ItemTag.GRIEVOUS_WOUNDS);
        }
        if (PERCENT_ARMOR_PEN.matcher(text).find()) {
            tags.add(ItemTag.ARMOR_PENETRATION_PERCENT);
        }
        if (LETHALITY.matcher(text).find()) {
            tags.add(ItemTag.ARMOR_PENETRATION_FLAT);
            tags.add(ItemTag.SHIELD_BREAK);
        }
        if (PERCENT_MAGIC_PEN.matcher(text).find()) {
            tags.add(ItemTag.MAGIC_PENETRATION_PERCENT);
        }
        if (FLAT_MAGIC_PEN.matcher(text).find()) {
            tags.add(ItemTag.MAGIC_PENETRATION_FLAT);
        }
        if (text.contains("Spell Shield")) {
            tags.add(ItemTag.SPELL_SHIELD);
        }
        if (PERCENT_HEALTH_HIT.matcher(text).find()) {
            tags.add(ItemTag.PERCENT_HEALTH_DAMAGE);
        }
        if (riotTags.contains("OnHit") || text.contains("On-Hit")) {
            tags.add(ItemTag.ON_HIT);
        }
        if (riotTags.contains("Tenacity")) {
            tags.add(ItemTag.TENACITY);
        }
        if (riotTags.contains("Slow")) {
            tags.add(ItemTag.SLOW_EFFECT);
        }
        if (text.matches("(?s).*(?i)cleanse.*") || name.equals("Quicksilver Sash")) {
            tags.add(ItemTag.CLEANSE);
        }
        if (text.contains("Heal and Shield Power")) {
            tags.add(ItemTag.ENCHANTER);
        }
        if (riotTags.contains("Boots")) {
            tags.add(ItemTag.BOOTS);
        }
        if (isBurstResistance(node.path("name").asText())) {
            tags.add(ItemTag.BURST_RESISTANCE);
        }
        if (node.has("into") && node.path("into").size() > 0) {
            tags.add(ItemTag.COMPONENT);
        }
        if (riotTags.contains("Lane") && gold <= STARTER_MAX_GOLD) {
            tags.add(ItemTag.STARTER);
        }
        return Set.copyOf(tags);
    }

    // ponytail: "burst resistance" (revive / damage shield) is a judgment, not a stat; hand-kept and short.
    private static final Set<String> BURST_RESISTANCE_ITEMS = Set.of(
            "Maw of Malmortius", "Guardian Angel", "Zhonya's Hourglass", "Immortal Shieldbow", "Sterak's Gage",
            "Gargoyle Stoneplate", "Death's Dance", "Hexdrinker", "Kaenic Rookern", "Edge of Night", "Banshee's Veil");

    private static boolean isBurstResistance(String name) {
        return BURST_RESISTANCE_ITEMS.contains(name);
    }

    private static String plainText(String description) {
        return description.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ");
    }

    private static int firstInt(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
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
