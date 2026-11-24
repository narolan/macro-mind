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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

/** The JSON shape is a contract with the overlay (overlay/src/viewModel.js reads exactly these fields). */
class AdviceContractTest {

    private AdviceServer server;

    @AfterEach
    void stop() {
        server.stop();
    }

    private JsonNode fetch() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + "/advice")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.headers().firstValue("Content-Type").orElse(""));
        return new ObjectMapper().readTree(response.body());
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> names = new TreeSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    @Test
    void topLevelPayloadHasExactlyVisibleAdviceAndIconVersion() throws Exception {
        server = new AdviceServer(0, "16.22.1");

        JsonNode json = fetch();

        assertEquals(Set.of("visible", "advice", "iconVersion", "status", "alerts", "timers"), fieldNames(json));
        assertTrue(json.get("visible").isBoolean());
        assertTrue(json.get("advice").isArray());
        assertTrue(json.get("iconVersion").isTextual());
    }

    @Test
    void everyAdviceEntryHasExactlyTheFieldsAndTypesTheOverlayReads() throws Exception {
        server = new AdviceServer(0, "16.22.1");
        Item item = new Item(3075, "Thornmail", 2450, ItemStats.NONE, Set.of());
        Player zed = player("Zed", ChampionClass.ASSASSIN, true, List.of(), AVERAGE_STATS);
        server.publish(true, List.of(
                new BuildAdvice(item, "why", AdvicePriority.CRITICAL, AdviceCategory.UTILITY, zed, Confidence.ESTIMATED, 5),
                new BuildAdvice(item, "why2", AdvicePriority.SITUATIONAL, AdviceCategory.OFFENSIVE, null, Confidence.EXACT, 5)));

        JsonNode advice = fetch().get("advice");

        for (JsonNode entry : advice) {
            assertEquals(Set.of("item", "itemId", "reasoning", "priority", "category", "confidence", "target"), fieldNames(entry));
            assertTrue(entry.get("item").isTextual());
            assertTrue(entry.get("itemId").isInt());
            assertTrue(entry.get("reasoning").isTextual());
            assertTrue(entry.get("priority").isTextual());
            assertTrue(entry.get("category").isTextual());
            assertTrue(entry.get("confidence").isTextual());
        }
        assertEquals("Zed", advice.get(0).get("target").asText());
        assertTrue(advice.get(1).get("target").isNull(), "a missing target must be an explicit null, not an absent field");
    }

    @Test
    void enumsAreSentAsTheirNamesTheOverlayMatchesOn() throws Exception {
        server = new AdviceServer(0);
        Item item = new Item(1, "A", 1, ItemStats.NONE, Set.of());
        List<BuildAdvice> all = new ArrayList<>();
        for (AdvicePriority priority : AdvicePriority.values()) {
            all.add(new BuildAdvice(item, "r" + priority, priority, AdviceCategory.UTILITY, null, Confidence.EXACT, 0));
        }
        server.publish(true, all);

        Set<String> priorities = StreamSupport.stream(fetch().get("advice").spliterator(), false)
                .map(entry -> entry.get("priority").asText()).collect(Collectors.toSet());

        assertEquals(Set.of("CRITICAL", "RECOMMENDED", "SITUATIONAL"), priorities);
    }

    @Test
    void topLevelKeysComeInAStableOrder() throws Exception {
        server = new AdviceServer(0, "16.22.1");
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + "/advice")).build(),
                HttpResponse.BodyHandlers.ofString());

        assertTrue(response.body().startsWith("{\"visible\":false,\"advice\":[],\"iconVersion\":\"16.22.1\",\"status\":\"NO_GAME\",\"alerts\":[],\"timers\":[]}"), response.body());
    }

    @Test
    void servesRequestsAddressedToLocalhostAsWellAsTheNumericAddress() throws Exception {
        server = new AdviceServer(0);

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + "/advice")).build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
    }

    @Test
    void statusIsSentAsItsNameAndStartsAsNoGame() throws Exception {
        server = new AdviceServer(0);

        assertEquals("NO_GAME", fetch().get("status").asText());
    }

    @Test
    void everyStatusRoundTripsThroughThePayload() throws Exception {
        server = new AdviceServer(0);
        for (AppStatus status : AppStatus.values()) {
            server.publish(false, List.of(), status);

            assertEquals(status.name(), fetch().get("status").asText());
        }
    }

    @Test
    void publishingWithoutAStatusMeansInGame() throws Exception {
        server = new AdviceServer(0);
        server.publish(true, List.of());

        assertEquals("IN_GAME", fetch().get("status").asText());
    }

    @Test
    void alertsAndTimersHaveExactlyTheFieldsTheOverlayReads() throws Exception {
        server = new AdviceServer(0);
        server.publish(true, List.of(), List.of(new Alert(AlertKind.BUY_NOW, "k", "Buy now", AdvicePriority.CRITICAL)),
                List.of(new ObjectiveTimer("Baron", 25)), AppStatus.IN_GAME);

        JsonNode json = fetch();

        JsonNode alert = json.get("alerts").get(0);
        assertEquals(Set.of("kind", "message", "priority"), fieldNames(alert));
        assertEquals("BUY_NOW", alert.get("kind").asText());
        assertEquals("CRITICAL", alert.get("priority").asText());
        JsonNode timer = json.get("timers").get(0);
        assertEquals(Set.of("name", "secondsLeft"), fieldNames(timer));
        assertEquals(25, timer.get("secondsLeft").asInt());
    }
}
