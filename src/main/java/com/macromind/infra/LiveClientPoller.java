package com.macromind.infra;

import com.macromind.domain.model.GameState;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/** Polls the local League client. Certificate checks are skipped ONLY for loopback hosts. */
public class LiveClientPoller {

    private static final Set<String> LOOPBACK_HOSTS = Set.of("127.0.0.1", "localhost");
    private static final String ALL_GAME_DATA_PATH = "/liveclientdata/allgamedata";
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final URI allGameDataUri;
    private final GameStateMapper mapper;
    private final HttpClient http;
    private final Consumer<Exception> onError;

    public LiveClientPoller(String baseUrl, GameStateMapper mapper) {
        this(baseUrl, mapper, e -> System.err.println("Live client response could not be used: " + e));
    }

    /** onError hears about unusable responses; a missing game (refused connection, non-200) stays silent. */
    public LiveClientPoller(String baseUrl, GameStateMapper mapper, Consumer<Exception> onError) {
        URI base = URI.create(baseUrl);
        String host = base.getHost();
        if (host == null || !LOOPBACK_HOSTS.contains(host)) {
            throw new IllegalArgumentException("Live client must be a loopback host: " + baseUrl);
        }
        this.allGameDataUri = URI.create(baseUrl + ALL_GAME_DATA_PATH);
        this.mapper = mapper;
        this.onError = onError;
        this.http = HttpClient.newBuilder().sslContext(LoopbackTlsContext.create()).connectTimeout(TIMEOUT).build();
    }

    /** Empty when no game is running or the response is unusable. Never throws. */
    public Optional<GameState> poll() {
        try {
            HttpRequest request = HttpRequest.newBuilder(allGameDataUri).timeout(TIMEOUT).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            return Optional.of(mapper.toGameState(response.body()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (IOException e) {
            return Optional.empty(); // refused connection and the like: simply no game running
        } catch (RuntimeException e) {
            onError.accept(e);
            return Optional.empty();
        }
    }
}
