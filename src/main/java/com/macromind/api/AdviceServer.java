package com.macromind.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.macromind.domain.model.BuildAdvice;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Serves GET /advice on loopback only. The latest result is swapped in as an immutable value. */
public class AdviceServer {

    private static final int MAX_ADVICE = 5;
    // The Tauri webview origin differs per platform (tauri://localhost, http(s)://tauri.localhost on Windows).
    private static final java.util.Set<String> OVERLAY_ORIGINS =
            java.util.Set.of("tauri://localhost", "http://tauri.localhost", "https://tauri.localhost");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpServer server;
    private final AtomicReference<byte[]> latest = new AtomicReference<>(render(false, List.of()));

    public AdviceServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/advice", exchange -> {
            // A foreign Host header means DNS rebinding: refuse before anything is read.
            if (!allowedHosts().contains(exchange.getRequestHeaders().getFirst("Host"))) {
                exchange.sendResponseHeaders(403, -1);
                exchange.close();
                return;
            }
            byte[] body = latest.get();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            String origin = exchange.getRequestHeaders().getFirst("Origin");
            if (origin != null && OVERLAY_ORIGINS.contains(origin)) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
            }
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    private java.util.Set<String> allowedHosts() {
        return java.util.Set.of("127.0.0.1:" + port(), "localhost:" + port());
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public void publish(boolean visible, List<BuildAdvice> advice) {
        latest.set(render(visible, advice));
    }

    public void stop() {
        server.stop(0);
    }

    private static byte[] render(boolean visible, List<BuildAdvice> advice) {
        // Rules can match most of the catalogue; the overlay only has room for the most urgent few.
        List<Map<String, Object>> items = advice.stream()
                .sorted(java.util.Comparator.comparing(BuildAdvice::priority))
                .limit(MAX_ADVICE)
                .map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("item", a.recommendedItem().name());
            m.put("reasoning", a.reasoning());
            m.put("priority", a.priority().name());
            m.put("category", a.category().name());
            m.put("confidence", a.confidence().name());
            m.put("target", a.targetEnemy() == null ? null : a.targetEnemy().championName());
            return m;
        }).toList();
        try {
            return MAPPER.writeValueAsString(Map.of("visible", visible, "advice", items)).getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
