package com.macromind.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.macromind.domain.model.ItemTag;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Derives gameplay tags for an item. Riot's description text is the only place item effects are written down, so
 * most tags are read from it; a few come from Riot's own tags, and "burst resistance" is a short hand-kept list.
 */
final class ItemTagger {

    // Stat patterns are shared with the parser, which reads the same numbers into ItemStats.
    static final Pattern LETHALITY = Pattern.compile("(\\d+)\\s*Lethality");
    static final Pattern FLAT_MAGIC_PEN = Pattern.compile("(?<![\\d%])(\\d+)\\s*Magic Penetration");
    static final Pattern OMNIVAMP = Pattern.compile("(\\d+)%\\s*Omnivamp");
    static final Pattern ABILITY_HASTE = Pattern.compile("(\\d+)\\s*Ability Haste");

    private static final Pattern WOUNDS = Pattern.compile("\\bWounds\\b");
    private static final Pattern CLEANSE = Pattern.compile("cleanse", Pattern.CASE_INSENSITIVE);
    private static final Pattern PERCENT_MAGIC_PEN = Pattern.compile("\\d+%\\s*Magic Penetration");
    private static final Pattern PERCENT_ARMOR_PEN = Pattern.compile("\\d+%\\s*(bonus\\s*)?Armor Penetration");
    private static final Pattern PERCENT_HEALTH_HIT = Pattern.compile(
            "(max(imum)?|current|missing) Health[^.]{0,40}(damage|bonus)"
                    + "|(damage|bonus|burn)[^.]{0,60}\\d+%[^.]{0,25}(max(imum)?|current|missing) Health"
                    + "|\\d+%[^.]{0,25}(max(imum)?|current|missing) Health", Pattern.CASE_INSENSITIVE);

    private static final int STARTER_MAX_GOLD = 500;
    private static final String QUICKSILVER_SASH = "Quicksilver Sash";

    // ponytail: "burst resistance" (revive / damage shield) is a judgment, not a stat; hand-kept and short.
    private static final Set<String> BURST_RESISTANCE_ITEMS = Set.of(
            "Maw of Malmortius", "Guardian Angel", "Zhonya's Hourglass", "Immortal Shieldbow", "Sterak's Gage",
            "Gargoyle Stoneplate", "Death's Dance", "Hexdrinker", "Kaenic Rookern", "Edge of Night", "Banshee's Veil");

    private ItemTagger() {}

    /** Tags for one Data Dragon item node; text is its description with markup removed. */
    static Set<ItemTag> tagsFor(JsonNode node, String text, String name, int gold) {
        Set<ItemTag> tags = EnumSet.noneOf(ItemTag.class);
        tags.addAll(textTags(text, name));
        tags.addAll(riotTags(node, name, gold));
        return Set.copyOf(tags);
    }

    private static Set<ItemTag> textTags(String text, String name) {
        Set<ItemTag> tags = EnumSet.noneOf(ItemTag.class);
        addIf(tags, WOUNDS.matcher(text).find(), ItemTag.GRIEVOUS_WOUNDS);
        addIf(tags, PERCENT_ARMOR_PEN.matcher(text).find(), ItemTag.ARMOR_PENETRATION_PERCENT);
        if (LETHALITY.matcher(text).find()) {
            tags.add(ItemTag.ARMOR_PENETRATION_FLAT);
            tags.add(ItemTag.SHIELD_BREAK);
        }
        addIf(tags, PERCENT_MAGIC_PEN.matcher(text).find(), ItemTag.MAGIC_PENETRATION_PERCENT);
        addIf(tags, FLAT_MAGIC_PEN.matcher(text).find(), ItemTag.MAGIC_PENETRATION_FLAT);
        addIf(tags, text.contains("Spell Shield"), ItemTag.SPELL_SHIELD);
        addIf(tags, PERCENT_HEALTH_HIT.matcher(text).find(), ItemTag.PERCENT_HEALTH_DAMAGE);
        addIf(tags, text.contains("On-Hit"), ItemTag.ON_HIT);
        addIf(tags, CLEANSE.matcher(text).find() || name.equals(QUICKSILVER_SASH), ItemTag.CLEANSE);
        addIf(tags, text.contains("Heal and Shield Power"), ItemTag.ENCHANTER);
        addIf(tags, BURST_RESISTANCE_ITEMS.contains(name), ItemTag.BURST_RESISTANCE);
        return tags;
    }

    private static Set<ItemTag> riotTags(JsonNode node, String name, int gold) {
        Set<String> riot = new HashSet<>();
        node.path("tags").forEach(tag -> riot.add(tag.asText()));
        Set<ItemTag> tags = EnumSet.noneOf(ItemTag.class);
        addIf(tags, riot.contains("OnHit"), ItemTag.ON_HIT);
        addIf(tags, riot.contains("Tenacity"), ItemTag.TENACITY);
        addIf(tags, riot.contains("Slow"), ItemTag.SLOW_EFFECT);
        addIf(tags, riot.contains("Boots"), ItemTag.BOOTS);
        addIf(tags, node.path("into").size() > 0, ItemTag.COMPONENT);
        addIf(tags, riot.contains("Lane") && gold <= STARTER_MAX_GOLD, ItemTag.STARTER);
        return tags;
    }

    private static void addIf(Set<ItemTag> tags, boolean condition, ItemTag tag) {
        if (condition) {
            tags.add(tag);
        }
    }
}
