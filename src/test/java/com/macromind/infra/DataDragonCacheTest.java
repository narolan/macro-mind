package com.macromind.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DataDragonCacheTest {

    private static void touch(Path dir, String version) throws IOException {
        Files.writeString(dir.resolve(version + "-item.json"), "{}");
        Files.writeString(dir.resolve(version + "-championFull.json"), "{}");
    }

    @Test
    void readReturnsEmptyForAFileThatWasNeverWritten(@TempDir Path dir) throws IOException {
        assertEquals(Optional.empty(), new DataDragonCache(dir).read("16.1.1", "item.json"));
    }

    @Test
    void writeThenReadRoundTripsAndLeavesNoTempFiles(@TempDir Path dir) throws IOException {
        DataDragonCache cache = new DataDragonCache(dir.resolve("nested"));

        cache.write("16.1.1", "item.json", "{\"data\":1}");

        assertEquals(Optional.of("{\"data\":1}"), cache.read("16.1.1", "item.json"));
        try (var files = Files.list(dir.resolve("nested"))) {
            assertEquals(1, files.count(), "only the final file may remain, no .tmp leftovers");
        }
    }

    @Test
    void newestVersionComparesNumericallyNotAlphabetically(@TempDir Path dir) throws IOException {
        touch(dir, "16.9.1");
        touch(dir, "16.10.1");
        touch(dir, "15.24.1");

        assertEquals(Optional.of("16.10.1"), new DataDragonCache(dir).newestVersion());
    }

    @Test
    void newestVersionSkipsVersionsMissingOneOfTheTwoFiles(@TempDir Path dir) throws IOException {
        touch(dir, "16.9.1");
        Files.writeString(dir.resolve("16.10.1-item.json"), "{}");

        assertEquals(Optional.of("16.9.1"), new DataDragonCache(dir).newestVersion());
    }

    @Test
    void newestVersionIgnoresFilesThatAreNotVersions(@TempDir Path dir) throws IOException {
        touch(dir, "16.9.1");
        touch(dir, "latest");
        touch(dir, "../etc");

        assertEquals(Optional.of("16.9.1"), new DataDragonCache(dir).newestVersion());
    }

    @Test
    void newestVersionIsEmptyWhenTheDirectoryDoesNotExist(@TempDir Path dir) {
        assertEquals(Optional.empty(), new DataDragonCache(dir.resolve("missing")).newestVersion());
    }

    @Test
    void discardRemovesBothFilesOfAVersion(@TempDir Path dir) throws IOException {
        touch(dir, "16.9.1");
        DataDragonCache cache = new DataDragonCache(dir);

        cache.discard("16.9.1");

        assertEquals(Optional.empty(), cache.read("16.9.1", "item.json"));
        assertEquals(Optional.empty(), cache.read("16.9.1", "championFull.json"));
    }

    @Test
    void refusesAVersionThatCouldEscapeTheCacheDirectory(@TempDir Path dir) {
        DataDragonCache cache = new DataDragonCache(dir);

        assertThrows(IllegalArgumentException.class, () -> cache.read("../../etc", "item.json"));
        assertThrows(IllegalArgumentException.class, () -> cache.write("16.1.1/../x", "item.json", "x"));
    }
}
