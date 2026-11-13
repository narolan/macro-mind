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
    void returnsAtMostFiveAdviceEntriesCriticalFirst() throws Exception {
        server = new AdviceServer(0);
        List<BuildAdvice> many = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) {
            many.add(new BuildAdvice(item("Rec" + i, NO_STATS), "r", AdvicePriority.RECOMMENDED,
                    AdviceCategory.DEFENSIVE, null, Confidence.EXACT, 0));
        }
        many.add(new BuildAdvice(item("Crit", NO_STATS), "c", AdvicePriority.CRITICAL,
                AdviceCategory.DEFENSIVE, null, Confidence.EXACT, 0));

        server.publish(true, many);
        JsonNode json = get();

        assertEquals(5, json.get("advice").size());
        assertEquals("Crit", json.get("advice").get(0).get("item").asText());
    }
}
