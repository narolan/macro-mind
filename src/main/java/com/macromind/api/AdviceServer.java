package com.macromind.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.macromind.domain.model.BuildAdvice;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/** Serves GET /advice on loopback only. The latest result is swapped in as an immutable value. */
public class AdviceServer {

    private static final int HTTP_OK = 200;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int NO_BODY = -1;

    // Origins of the Tauri webview, kept for browser-based clients. The Electron overlay polls from its main
    // process with node:http and needs no CORS.
    private static final Set<String> OVERLAY_ORIGINS =
            Set.of("tauri://localhost", "http://tauri.localhost", "https://tauri.localhost");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpServer server;
    private final Set<String> allowedHosts;
    private final String iconVersion;
    private final AtomicReference<byte[]> latest;

    /** For tests and callers without Data Dragon data: no item icons. */
    public AdviceServer(int port) throws IOException {
        this(port, "");
    }

    public AdviceServer(int port, String iconVersion) throws IOException {
        this.iconVersion = iconVersion;
        this.latest = new AtomicReference<>(render(false, List.of()));
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        this.allowedHosts = Set.of("127.0.0.1:" + port(), "localhost:" + port());
        server.createContext("/advice", this::handleAdvice);
        server.start();
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

    private void handleAdvice(HttpExchange exchange) throws IOException {
        try (exchange) {
            // A foreign Host header means DNS rebinding: refuse before anything is read.
            if (!allowedHosts.contains(exchange.getRequestHeaders().getFirst("Host"))) {
                exchange.sendResponseHeaders(HTTP_FORBIDDEN, NO_BODY);
                return;
            }
            byte[] body = latest.get();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            String origin = exchange.getRequestHeaders().getFirst("Origin");
            if (origin != null && OVERLAY_ORIGINS.contains(origin)) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
            }
            exchange.sendResponseHeaders(HTTP_OK, body.length);
            exchange.getResponseBody().write(body);
        }
    }

    private byte[] render(boolean visible, List<BuildAdvice> advice) {
        try {
            return JSON.writeValueAsString(AdvicePayload.of(visible, advice, iconVersion)).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize the advice payload", e);
        }
    }
}
