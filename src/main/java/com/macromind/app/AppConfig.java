package com.macromind.app;

import java.nio.file.Path;

/** The few settings the app has. Defaults suit a normal install; each can be overridden with a system property. */
record AppConfig(int port, long pollIntervalMillis, String liveClientUrl, Path cacheDir) {

    private static final int DEFAULT_PORT = 8765;
    private static final long DEFAULT_POLL_INTERVAL_MILLIS = 1000;
    private static final String DEFAULT_LIVE_CLIENT_URL = "https://127.0.0.1:2999";

    static AppConfig load() {
        return new AppConfig(
                Integer.getInteger("macromind.port", DEFAULT_PORT),
                Long.getLong("macromind.pollIntervalMillis", DEFAULT_POLL_INTERVAL_MILLIS),
                System.getProperty("macromind.liveClientUrl", DEFAULT_LIVE_CLIENT_URL),
                Path.of(System.getProperty("macromind.cacheDir",
                        Path.of(System.getProperty("user.home"), ".macromind", "cache").toString())));
    }
}
