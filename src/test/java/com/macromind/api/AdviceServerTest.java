package com.macromind.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.macromind.domain.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class AdviceServerTest {

    private AdviceServer server;

    @AfterEach
    void stop() {
        server.stop();
    }

    private JsonNode get() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + "/advice")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        return new ObjectMapper().readTree(response.body());
    }

    @Test
    void adviceEndpointReturnsEmptyHiddenWhenNoGame() throws Exception {
        server = new AdviceServer(0);

        JsonNode json = get();

        assertFalse(json.get("visible").asBoolean());
        assertEquals(0, json.get("advice").size());
    }

    @Test
    void serializesNullTargetEnemy() throws Exception {
        server = new AdviceServer(0);
        BuildAdvice advice = new BuildAdvice(item("Void Staff", NO_STATS), "pen", AdvicePriority.CRITICAL,
                AdviceCategory.OFFENSIVE, null, Confidence.ESTIMATED, 5);

        server.publish(true, List.of(advice));
        JsonNode json = get();

        assertTrue(json.get("visible").asBoolean());
        JsonNode first = json.get("advice").get(0);
        assertEquals("Void Staff", first.get("item").asText());
        assertEquals("ESTIMATED", first.get("confidence").asText());
        assertTrue(first.get("target").isNull());
    }

    @Test
    void putsCriticalAdviceFirst() throws Exception {
        server = new AdviceServer(0);
        List<BuildAdvice> many = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            many.add(new BuildAdvice(item("Rec" + i, NO_STATS), "r" + i, AdvicePriority.RECOMMENDED,
                    AdviceCategory.DEFENSIVE, null, Confidence.EXACT, 0));
        }
        many.add(new BuildAdvice(item("Crit", NO_STATS), "c", AdvicePriority.CRITICAL,
                AdviceCategory.DEFENSIVE, null, Confidence.EXACT, 0));

        server.publish(true, many);

        assertEquals("Crit", get().get("advice").get(0).get("item").asText());
    }

    @Test
    void servesItemIdsAndTheIconVersionSoTheOverlayCanShowIcons() throws Exception {
        server = new AdviceServer(0, "16.22.1");
        Item item = new Item(3075, "Thornmail", 2450, NO_STATS, java.util.Set.of());
        server.publish(true, List.of(new BuildAdvice(item, "r", AdvicePriority.CRITICAL, AdviceCategory.DEFENSIVE,
                null, Confidence.EXACT, 0)));

        JsonNode json = get();

        assertEquals("16.22.1", json.get("iconVersion").asText());
        assertEquals(3075, json.get("advice").get(0).get("itemId").asInt());
    }

    @Test
    void iconVersionIsEmptyWhenDataDragonNeverLoaded() throws Exception {
        server = new AdviceServer(0);

        assertEquals("", get().get("iconVersion").asText());
    }

    private static BuildAdvice advice(String item, String reason, AdvicePriority priority) {
        return new BuildAdvice(item(item, NO_STATS), reason, priority, AdviceCategory.DEFENSIVE, null, Confidence.EXACT, 0);
    }

    @Test
    void keepsAtMostFourEntriesPerReasonSoOtherReasonsStillReachTheOverlay() throws Exception {
        server = new AdviceServer(0);
        List<BuildAdvice> advice = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) {
            advice.add(advice("Heal" + i, "heavy healing", AdvicePriority.CRITICAL));
        }
        advice.add(advice("Boots", "needs boots", AdvicePriority.RECOMMENDED));
        advice.add(advice("Core", "fits your class", AdvicePriority.SITUATIONAL));

        server.publish(true, advice);
        JsonNode entries = get().get("advice");

        long healing = java.util.stream.StreamSupport.stream(entries.spliterator(), false)
                .filter(e -> e.get("reasoning").asText().equals("heavy healing")).count();
        assertEquals(4, healing);
        assertEquals(6, entries.size());
        assertEquals("heavy healing", entries.get(0).get("reasoning").asText());
    }

    @Test
    void neverSendsMoreThanTwelveEntriesOverall() throws Exception {
        server = new AdviceServer(0);
        List<BuildAdvice> advice = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) {
            advice.add(advice("Item" + i, "reason " + i, AdvicePriority.RECOMMENDED));
        }

        server.publish(true, advice);

        assertEquals(12, get().get("advice").size());
    }
}
