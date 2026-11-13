package com.macromind.infra;

import com.macromind.domain.model.ChampionBaseStats;
import com.macromind.domain.model.Item;
import com.macromind.domain.model.ItemTag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataDragonParserTest {

    private static String fixture(String name) throws IOException {
        return Files.readString(Path.of("src/test/resources/fixtures/" + name));
    }

    @Test
    void parsesChampionBaseStats() throws IOException {
        Map<String, ChampionBaseStats> champions = DataDragonParser.parseChampions(fixture("ddragon-champion.json"));

        assertEquals(new ChampionBaseStats(600, 100, 30, 4, 32, 2, 60, 3), champions.get("Malphite"));
    }

    @Test
    void parsesItemStatsAndPrice() throws IOException {
        List<Item> items = DataDragonParser.parseItems(fixture("ddragon-item.json"));

        Item thornmail = items.stream().filter(i -> i.name().equals("Thornmail")).findFirst().orElseThrow();
        assertEquals(3075, thornmail.id());
        assertEquals(2700, thornmail.goldValue());
        assertEquals(70, thornmail.stats().armor());
        assertEquals(350, thornmail.stats().health());
    }

    @Test
    void derivesGrievousWoundsTagFromDescription() throws IOException {
        List<Item> items = DataDragonParser.parseItems(fixture("ddragon-item.json"));

        Item morello = items.stream().filter(i -> i.name().equals("Morellonomicon")).findFirst().orElseThrow();
        assertTrue(morello.tags().contains(ItemTag.GRIEVOUS_WOUNDS));
    }

    @Test
    void returnsEmptyForMalformedJsonInsteadOfThrowing() {
        assertTrue(DataDragonParser.parseItems("not json").isEmpty());
        assertTrue(DataDragonParser.parseChampions("not json").isEmpty());
    }

    @Test
    void mapsFirstDataDragonTagToChampionClass() throws IOException {
        var classes = DataDragonParser.parseChampionClasses(fixture("ddragon-champion.json"));

        assertEquals(com.macromind.domain.model.ChampionClass.TANK, classes.get("Malphite"));
    }

    @Test
    void keepsOnlyPurchasableSummonersRiftItems() throws IOException {
        List<Item> items = DataDragonParser.parseItems(fixture("ddragon-item.json"));

        assertEquals(java.util.Set.of(3075, 3146, 3165), items.stream().map(Item::id).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void keysChampionsByDisplayNameAsWellAsId() throws IOException {
        String json = fixture("ddragon-champion.json");

        assertTrue(DataDragonParser.parseChampions(json).containsKey("Miss Fortune"));
        assertEquals(com.macromind.domain.model.ChampionClass.MARKSMAN,
                DataDragonParser.parseChampionClasses(json).get("Miss Fortune"));
    }

    @Test
    void parsesOmnivampFromItemDescription() throws IOException {
        List<Item> items = DataDragonParser.parseItems(fixture("ddragon-item.json"));

        Item gunblade = items.stream().filter(i -> i.name().equals("Hextech Gunblade")).findFirst().orElseThrow();
        assertEquals(10, gunblade.stats().omnivamp());
    }

    private static Map<String, Item> realItems() throws IOException {
        return DataDragonParser.parseItems(fixture("ddragon-item-real.json")).stream()
                .collect(java.util.stream.Collectors.toMap(Item::name, i -> i, (a, b) -> a));
    }

    private static void assertTags(Item item, ItemTag... expected) {
        for (ItemTag tag : expected) {
            assertTrue(item.tags().contains(tag), item.name() + " should have " + tag + " but has " + item.tags());
        }
    }

    @Test
    void derivesPenetrationAndWoundsTagsFromRealDescriptions() throws IOException {
        Map<String, Item> items = realItems();

        assertTags(items.get("Mortal Reminder"), ItemTag.GRIEVOUS_WOUNDS, ItemTag.ARMOR_PENETRATION_PERCENT);
        assertTags(items.get("Void Staff"), ItemTag.MAGIC_PENETRATION_PERCENT);
        assertTags(items.get("Bramble Vest"), ItemTag.GRIEVOUS_WOUNDS, ItemTag.COMPONENT);
        assertTags(items.get("Executioner's Calling"), ItemTag.GRIEVOUS_WOUNDS, ItemTag.COMPONENT);
        assertTags(items.get("Thornmail"), ItemTag.GRIEVOUS_WOUNDS);
    }

    @Test
    void parsesLethalityAndFlatMagicPenetrationIntoStatsAndTags() throws IOException {
        Map<String, Item> items = realItems();

        assertTags(items.get("Serpent's Fang"), ItemTag.ARMOR_PENETRATION_FLAT, ItemTag.SHIELD_BREAK);
        assertEquals(15, items.get("Serpent's Fang").stats().flatArmorPen());
        assertTags(items.get("Sorcerer's Shoes"), ItemTag.MAGIC_PENETRATION_FLAT, ItemTag.BOOTS);
        assertEquals(12, items.get("Sorcerer's Shoes").stats().flatMagicPen());
    }

    @Test
    void derivesShieldPercentHealthTenacityAndOnHitTags() throws IOException {
        Map<String, Item> items = realItems();

        assertTags(items.get("Banshee's Veil"), ItemTag.SPELL_SHIELD);
        assertTags(items.get("Edge of Night"), ItemTag.SPELL_SHIELD, ItemTag.ARMOR_PENETRATION_FLAT);
        assertTags(items.get("Blade of The Ruined King"), ItemTag.PERCENT_HEALTH_DAMAGE, ItemTag.ON_HIT);
        assertTags(items.get("Liandry's Torment"), ItemTag.PERCENT_HEALTH_DAMAGE);
        assertTags(items.get("Mercury's Treads"), ItemTag.TENACITY, ItemTag.BOOTS);
        assertTags(items.get("Maw of Malmortius"), ItemTag.BURST_RESISTANCE);
    }

    @Test
    void parsesAbilityHasteIntoCooldownReduction() throws IOException {
        assertEquals(10, realItems().get("Ionian Boots of Lucidity").stats().cooldownReduction(), 0.001);
    }

    @Test
    void dropsConsumablesWardsTrinketsAndJunglePets() throws IOException {
        Map<String, Item> items = realItems();

        for (String name : List.of("Health Potion", "Control Ward", "Stealth Ward", "Gustwalker Hatchling", "Cappa Juice")) {
            assertFalse(items.containsKey(name), name + " must not be adviseable");
        }
    }

    @Test
    void marksLaneStartersButNotExpensiveLaneItems() throws IOException {
        Map<String, Item> items = realItems();

        assertTags(items.get("Doran's Blade"), ItemTag.STARTER);
        assertFalse(items.get("Atma's Reckoning").tags().contains(ItemTag.STARTER));
    }

    @Test
    void keepsOnlyTheLowestIdWhenModeCopiesShareAName() throws IOException {
        List<Item> thornmails = DataDragonParser.parseItems(fixture("ddragon-item-real.json")).stream()
                .filter(i -> i.name().equals("Thornmail")).toList();

        assertEquals(1, thornmails.size());
        assertEquals(3075, thornmails.get(0).id());
    }

    @Test
    void tagsEnchanterItemsFromHealAndShieldPower() throws IOException {
        Map<String, Item> items = realItems();

        assertTags(items.get("Ardent Censer"), ItemTag.ENCHANTER);
        assertTags(items.get("Staff of Flowing Water"), ItemTag.ENCHANTER);
        assertFalse(items.get("Infinity Edge").tags().contains(ItemTag.ENCHANTER));
    }

    @Test
    void tagsCleanseItemsFromRealDescriptions() throws IOException {
        Map<String, Item> items = realItems();

        assertTrue(items.containsKey("Quicksilver Sash"), "the cleanse component must stay in the catalogue");
        assertTags(items.get("Quicksilver Sash"), ItemTag.CLEANSE, ItemTag.COMPONENT);
        assertFalse(items.get("Infinity Edge").tags().contains(ItemTag.CLEANSE));
    }
}
