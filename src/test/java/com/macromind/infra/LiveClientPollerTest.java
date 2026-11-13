package com.macromind.infra;

import com.macromind.Fixtures;
import com.macromind.domain.model.ChampionBaseStats;
import com.macromind.domain.model.ChampionClass;
import com.macromind.domain.service.StatEstimator;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LiveClientPollerTest {

    private static final GameStateMapper MAPPER = new GameStateMapper(
            new StatEstimator(name -> Optional.<ChampionBaseStats>empty()), List.of(), name -> ChampionClass.MARKSMAN);

    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String serve(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/liveclientdata/allgamedata", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void pollReturnsEmptyWhenNoGameIsRunning() {
        List<Exception> errors = new ArrayList<>();
        LiveClientPoller poller = new LiveClientPoller("https://127.0.0.1:1", MAPPER, errors::add);

        assertTrue(poller.poll().isEmpty());
        assertTrue(errors.isEmpty(), "a refused connection just means no game and must stay quiet");
    }

    @Test
    void refusesToDisableCertificateChecksForNonLocalHosts() {
        assertThrows(IllegalArgumentException.class, () -> new LiveClientPoller("https://example.com", null));
    }

    @Test
    void pollMapsAValidResponseToGameState() throws IOException {
        String url = serve(200, Fixtures.read(Fixtures.ALL_GAME_DATA));

        var state = new LiveClientPoller(url, MAPPER, e -> fail(e)).poll();

        assertTrue(state.isPresent());
        assertEquals(2, state.get().enemies().size());
    }

    @Test
    void pollReportsUnreadableResponsesInsteadOfSwallowingThem() throws IOException {
        List<Exception> errors = new ArrayList<>();
        String url = serve(200, "this is not json");

        assertTrue(new LiveClientPoller(url, MAPPER, errors::add).poll().isEmpty());
        assertEquals(1, errors.size());
    }

    @Test
    void pollReturnsEmptyWithoutErrorOnNon200() throws IOException {
        List<Exception> errors = new ArrayList<>();
        String url = serve(404, "{}");

        assertTrue(new LiveClientPoller(url, MAPPER, errors::add).poll().isEmpty());
        assertTrue(errors.isEmpty());
    }
}
