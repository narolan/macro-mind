package com.macromind.infra;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.macromind.domain.model.GameState;

/** Polls the local League client. Certificate checks are skipped ONLY for loopback hosts. */
public class LiveClientPoller {

    private static final Set<String> LOOPBACK_HOSTS = Set.of("127.0.0.1", "localhost");
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final String baseUrl;
    private final GameStateMapper mapper;
    private final HttpClient http;
    private final Consumer<Exception> onError;

    public LiveClientPoller(String baseUrl, GameStateMapper mapper) {
        this(baseUrl, mapper, e -> System.err.println("Live client response could not be used: " + e));
    }

    /** onError hears about unusable responses; a missing game (refused connection, non-200) stays silent. */
    public LiveClientPoller(String baseUrl, GameStateMapper mapper, Consumer<Exception> onError) {
        if (!LOOPBACK_HOSTS.contains(URI.create(baseUrl).getHost())) {
            throw new IllegalArgumentException("Live client must be a loopback host: " + baseUrl);
        }
        this.baseUrl = baseUrl;
        this.mapper = mapper;
        this.onError = onError;
        this.http = HttpClient.newBuilder().sslContext(trustAllContext()).connectTimeout(TIMEOUT).build();
    }

    /** Empty when no game is running or the response is unusable. Never throws. */
    public Optional<GameState> poll() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/liveclientdata/allgamedata"))
                    .timeout(TIMEOUT).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            return Optional.of(mapper.toGameState(response.body()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (java.io.IOException e) {
            return Optional.empty();
        } catch (RuntimeException e) {
            onError.accept(e);
            return Optional.empty();
        }
    }

    private static SSLContext trustAllContext() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[]{new TrustEverything()}, null);
            return context;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // Extended trust manager also skips hostname verification (the self-signed cert has no 127.0.0.1 SAN).
    private static final class TrustEverything extends X509ExtendedTrustManager {
        public void checkClientTrusted(X509Certificate[] c, String a) { }
        public void checkServerTrusted(X509Certificate[] c, String a) { }
        public void checkClientTrusted(X509Certificate[] c, String a, Socket s) { }
        public void checkServerTrusted(X509Certificate[] c, String a, Socket s) { }
        public void checkClientTrusted(X509Certificate[] c, String a, SSLEngine e) { }
        public void checkServerTrusted(X509Certificate[] c, String a, SSLEngine e) { }
        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    }
}
