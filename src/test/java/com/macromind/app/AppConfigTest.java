package com.macromind.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    private static final String[] KEYS = {"macromind.port", "macromind.pollIntervalMillis", "macromind.liveClientUrl", "macromind.cacheDir"};

    @AfterEach
    void clearOverrides() {
        for (String key : KEYS) {
            System.clearProperty(key);
        }
    }

    @Test
    void defaultsSuitANormalInstall() {
        AppConfig config = AppConfig.load();

        assertEquals(8765, config.port());
        assertEquals(1000, config.pollIntervalMillis());
        assertEquals("https://127.0.0.1:2999", config.liveClientUrl());
        assertEquals(Path.of(System.getProperty("user.home"), ".macromind", "cache"), config.cacheDir());
    }

    @Test
    void everySettingCanBeOverriddenWithASystemProperty() {
        System.setProperty("macromind.port", "9911");
        System.setProperty("macromind.pollIntervalMillis", "250");
        System.setProperty("macromind.liveClientUrl", "https://localhost:2999");
        System.setProperty("macromind.cacheDir", "/tmp/mm-cache");

        AppConfig config = AppConfig.load();

        assertEquals(9911, config.port());
        assertEquals(250, config.pollIntervalMillis());
        assertEquals("https://localhost:2999", config.liveClientUrl());
        assertEquals(Path.of("/tmp/mm-cache"), config.cacheDir());
    }

    @Test
    void anUnreadableNumberFallsBackToTheDefault() {
        System.setProperty("macromind.port", "not-a-number");

        assertEquals(8765, AppConfig.load().port());
    }
}
