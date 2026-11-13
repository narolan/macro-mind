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
    void appliesGrievousWoundsTagFromCuratedTable() throws IOException {
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

        assertEquals(java.util.Set.of(3075, 3165), items.stream().map(Item::id).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void keysChampionsByDisplayNameAsWellAsId() throws IOException {
        String json = fixture("ddragon-champion.json");

        assertTrue(DataDragonParser.parseChampions(json).containsKey("Miss Fortune"));
        assertEquals(com.macromind.domain.model.ChampionClass.MARKSMAN,
                DataDragonParser.parseChampionClasses(json).get("Miss Fortune"));
    }
}
