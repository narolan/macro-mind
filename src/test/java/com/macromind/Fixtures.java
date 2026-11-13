package com.macromind;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/** Loads test fixtures from the classpath, so tests do not depend on the working directory. */
public final class Fixtures {

    public static final String ALL_GAME_DATA = "allgamedata.json";
    public static final String ALL_GAME_DATA_REAL = "allgamedata-real.json";
    public static final String DDRAGON_CHAMPIONS = "ddragon-champion.json";
    public static final String DDRAGON_ITEMS = "ddragon-item.json";
    public static final String DDRAGON_ITEMS_REAL = "ddragon-item-real.json";

    private Fixtures() {}

    public static String read(String name) {
        try (InputStream in = Fixtures.class.getResourceAsStream("/fixtures/" + name)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing test fixture: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read test fixture " + name, e);
        }
    }

    public static Path path(String name) {
        try {
            return Path.of(Fixtures.class.getResource("/fixtures/" + name).toURI());
        } catch (java.net.URISyntaxException | NullPointerException e) {
            throw new IllegalArgumentException("Missing test fixture: " + name, e);
        }
    }
}
