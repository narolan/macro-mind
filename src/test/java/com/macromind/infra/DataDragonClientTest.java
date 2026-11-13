package com.macromind.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DataDragonClientTest {

    private static final String UNREACHABLE = "http://127.0.0.1:1";

    private static void warmCache(Path dir, String version) throws IOException {
        Files.createDirectories(dir);
        Files.copy(Path.of("src/test/resources/fixtures/ddragon-champion.json"), dir.resolve(version + "-championFull.json"));
        Files.copy(Path.of("src/test/resources/fixtures/ddragon-item.json"), dir.resolve(version + "-item.json"));
    }

    @Test
    void loadsNewestCachedVersionWhenVersionListIsUnreachable(@TempDir Path dir) throws IOException {
        warmCache(dir, "16.9.1");
        warmCache(dir, "16.10.1");
        DataDragonClient client = new DataDragonClient(dir, UNREACHABLE);

        client.load();

        assertEquals(3, client.items().size());
        assertTrue(client.championBaseStats("Miss Fortune").isPresent());
    }

    @Test
    void staysEmptyWithoutCacheAndWithoutNetwork(@TempDir Path dir) {
        DataDragonClient client = new DataDragonClient(dir, UNREACHABLE);

        client.load();

        assertTrue(client.items().isEmpty());
    }

    @Test
    void ignoresCorruptCacheFiles(@TempDir Path dir) throws IOException {
        warmCache(dir, "16.9.1");
        Files.writeString(dir.resolve("16.9.1-item.json"), "{\"data\": tru");
        DataDragonClient client = new DataDragonClient(dir, UNREACHABLE);

        client.load();

        assertTrue(client.items().isEmpty());
    }
}
