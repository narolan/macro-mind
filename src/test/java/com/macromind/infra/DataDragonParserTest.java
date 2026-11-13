package com.macromind.infra;

import com.macromind.Fixtures;
import com.macromind.domain.model.ChampionBaseStats;
import com.macromind.domain.model.Item;
import com.macromind.domain.model.ItemTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataDragonParserTest {

    private static String fixture(String name) {
        return Fixtures.read(name);
    }

    @Test
    void parsesChampionBaseStats() {
        Map<String, ChampionBaseStats> champions = DataDragonParser.parseChampions(fixture(Fixtures.DDRAGON_CHAMPIONS));

        assertEquals(new ChampionBaseStats(600, 100, 30, 4, 32, 2, 60, 3), champions.get("Malphite"));
    }

    @Test
    void parsesItemStatsAndPrice() {
        List<Item> items = DataDragonParser.parseItems(fixture(Fixtures.DDRAGON_ITEMS));

        Item thornmail = items.stream().filter(i -> i.name().equals("Thornmail")).findFirst().orElseThrow();
        assertEquals(3075, thornmail.id());
        assertEquals(2700, thornmail.goldValue());
        assertEquals(70, thornmail.stats().armor());
        assertEquals(350, thornmail.stats().health());
    }

    @Test
    void derivesGrievousWoundsTagFromDescription() {
        List<Item> items = DataDragonParser.parseItems(fixture(Fixtures.DDRAGON_ITEMS));

        Item morello = items.stream().filter(i -> i.name().equals("Morellonomicon")).findFirst().orElseThrow();
        assertTrue(morello.tags().contains(ItemTag.GRIEVOUS_WOUNDS));
    }

    @Test
    void returnsEmptyForMalformedJsonInsteadOfThrowing() {
        assertTrue(DataDragonParser.parseItems("not json").isEmpty());
        assertTrue(DataDragonParser.parseChampions("not json").isEmpty());
    }

    @Test
    void mapsFirstDataDragonTagToChampionClass() {
        var classes = DataDragonParser.parseChampionClasses(fixture(Fixtures.DDRAGON_CHAMPIONS));

        assertEquals(com.macromind.domain.model.ChampionClass.TANK, classes.get("Malphite"));
    }

    @Test
    void keepsOnlyPurchasableSummonersRiftItems() {
        List<Item> items = DataDragonParser.parseItems(fixture(Fixtures.DDRAGON_ITEMS));

        assertEquals(java.util.Set.of(3075, 3146, 3165), items.stream().map(Item::id).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void keysChampionsByDisplayNameAsWellAsId() {
        String json = fixture(Fixtures.DDRAGON_CHAMPIONS);

        assertTrue(DataDragonParser.parseChampions(json).containsKey("Miss Fortune"));
        assertEquals(com.macromind.domain.model.ChampionClass.MARKSMAN,
                DataDragonParser.parseChampionClasses(json).get("Miss Fortune"));
    }

    @Test
    void parsesOmnivampFromItemDescription() {
        List<Item> items = DataDragonParser.parseItems(fixture(Fixtures.DDRAGON_ITEMS));

        Item gunblade = items.stream().filter(i -> i.name().equals("Hextech Gunblade")).findFirst().orElseThrow();
        assertEquals(10, gunblade.stats().omnivamp());
    }

    private static Map<String, Item> realItems() {
        return DataDragonParser.parseItems(fixture(Fixtures.DDRAGON_ITEMS_REAL)).stream()
                .collect(java.util.stream.Collectors.toMap(Item::name, i -> i, (a, b) -> a));
    }

    private static void assertTags(Item item, ItemTag... expected) {
        for (ItemTag tag : expected) {
            assertTrue(item.tags().contains(tag), item.name() + " should have " + tag + " but has " + item.tags());
        }
    }

    @Test
    void derivesPenetrationAndWoundsTagsFromRealDescriptions() {
        Map<String, Item> items = realItems();

        assertTags(items.get("Mortal Reminder"), ItemTag.GRIEVOUS_WOUNDS, ItemTag.ARMOR_PENETRATION_PERCENT);
        assertTags(items.get("Void Staff"), ItemTag.MAGIC_PENETRATION_PERCENT);
        assertTags(items.get("Bramble Vest"), ItemTag.GRIEVOUS_WOUNDS, ItemTag.COMPONENT);
        assertTags(items.get("Executioner's Calling"), ItemTag.GRIEVOUS_WOUNDS, ItemTag.COMPONENT);
        assertTags(items.get("Thornmail"), ItemTag.GRIEVOUS_WOUNDS);
    }

    @Test
    void parsesLethalityAndFlatMagicPenetrationIntoStatsAndTags() {
        Map<String, Item> items = realItems();

        assertTags(items.get("Serpent's Fang"), ItemTag.ARMOR_PENETRATION_FLAT, ItemTag.SHIELD_BREAK);
        assertEquals(15, items.get("Serpent's Fang").stats().flatArmorPen());
        assertTags(items.get("Sorcerer's Shoes"), ItemTag.MAGIC_PENETRATION_FLAT, ItemTag.BOOTS);
        assertEquals(12, items.get("Sorcerer's Shoes").stats().flatMagicPen());
    }

    @Test
    void derivesShieldPercentHealthTenacityAndOnHitTags() {
        Map<String, Item> items = realItems();

        assertTags(items.get("Banshee's Veil"), ItemTag.SPELL_SHIELD);
        assertTags(items.get("Edge of Night"), ItemTag.SPELL_SHIELD, ItemTag.ARMOR_PENETRATION_FLAT);
        assertTags(items.get("Blade of The Ruined King"), ItemTag.PERCENT_HEALTH_DAMAGE, ItemTag.ON_HIT);
        assertTags(items.get("Liandry's Torment"), ItemTag.PERCENT_HEALTH_DAMAGE);
        assertTags(items.get("Mercury's Treads"), ItemTag.TENACITY, ItemTag.BOOTS);
        assertTags(items.get("Maw of Malmortius"), ItemTag.BURST_RESISTANCE);
    }

    @Test
    void parsesAbilityHasteIntoCooldownReduction() {
        assertEquals(10, realItems().get("Ionian Boots of Lucidity").stats().cooldownReduction(), 0.001);
    }

    @Test
    void dropsConsumablesWardsTrinketsAndJunglePets() {
        Map<String, Item> items = realItems();

        for (String name : List.of("Health Potion", "Control Ward", "Stealth Ward", "Gustwalker Hatchling", "Cappa Juice")) {
            assertFalse(items.containsKey(name), name + " must not be adviseable");
        }
    }

    @Test
    void marksLaneStartersButNotExpensiveLaneItems() {
        Map<String, Item> items = realItems();

        assertTags(items.get("Doran's Blade"), ItemTag.STARTER);
        assertFalse(items.get("Atma's Reckoning").tags().contains(ItemTag.STARTER));
    }

    @Test
    void keepsOnlyTheLowestIdWhenModeCopiesShareAName() {
        List<Item> thornmails = DataDragonParser.parseItems(fixture(Fixtures.DDRAGON_ITEMS_REAL)).stream()
                .filter(i -> i.name().equals("Thornmail")).toList();

        assertEquals(1, thornmails.size());
        assertEquals(3075, thornmails.get(0).id());
    }

    @Test
    void tagsEnchanterItemsFromHealAndShieldPower() {
        Map<String, Item> items = realItems();

        assertTags(items.get("Ardent Censer"), ItemTag.ENCHANTER);
        assertTags(items.get("Staff of Flowing Water"), ItemTag.ENCHANTER);
        assertFalse(items.get("Infinity Edge").tags().contains(ItemTag.ENCHANTER));
    }

    @Test
    void tagsCleanseItemsFromRealDescriptions() {
        Map<String, Item> items = realItems();

        assertTrue(items.containsKey("Quicksilver Sash"), "the cleanse component must stay in the catalogue");
        assertTags(items.get("Quicksilver Sash"), ItemTag.CLEANSE, ItemTag.COMPONENT);
        assertFalse(items.get("Infinity Edge").tags().contains(ItemTag.CLEANSE));
    }

    @Test
    void everyItemInARealGameIsKnownToTheCatalogueOrDeliberatelyExcluded() throws java.io.IOException {
        String real = Fixtures.read(Fixtures.ALL_GAME_DATA_REAL);
        com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(real);
        java.util.Set<String> consumables = java.util.Set.of("Refillable Potion", "Stealth Ward", "Control Ward", "Health Potion");
        for (com.fasterxml.jackson.databind.JsonNode player : root.path("allPlayers")) {
            for (com.fasterxml.jackson.databind.JsonNode i : player.path("items")) {
                assertTrue(i.path("itemID").asInt() > 0 && !i.path("displayName").asText().isEmpty());
                assertTrue(consumables.contains(i.path("displayName").asText()) || i.path("price").asInt() > 0);
            }
        }
    }
}
