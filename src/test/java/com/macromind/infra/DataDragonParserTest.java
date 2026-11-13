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
}
