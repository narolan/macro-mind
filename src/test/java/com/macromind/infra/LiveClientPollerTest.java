package com.macromind.infra;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LiveClientPollerTest {

    @Test
    void pollReturnsEmptyWhenNoGameIsRunning() {
        LiveClientPoller poller = new LiveClientPoller("https://127.0.0.1:1", null);

        assertTrue(poller.poll().isEmpty());
    }

    @Test
    void refusesToDisableCertificateChecksForNonLocalHosts() {
        assertThrows(IllegalArgumentException.class, () -> new LiveClientPoller("https://example.com", null));
    }
}
