package com.macromind;

import com.macromind.api.AdviceServer;
import com.macromind.domain.model.BuildAdvice;
import com.macromind.domain.model.GameState;
import com.macromind.domain.service.BuildAdvisor;
import com.macromind.domain.service.OverlayVisibility;
import com.macromind.domain.service.StatEstimator;
import com.macromind.infra.DataDragonClient;
import com.macromind.infra.GameStateMapper;
import com.macromind.infra.LiveClientPoller;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class Main {

    private static final int PORT = 8765;
    private static final long POLL_INTERVAL_MS = 1000;
    private static final String LIVE_CLIENT = "https://127.0.0.1:2999";

    private Main() {}

    // The same failure repeats every second; print it once until it changes.
    private static void logOnce(AtomicReference<String> last, String message) {
        if (!message.equals(last.getAndSet(message))) {
            System.err.println(message);
        }
    }

    public static void main(String[] args) throws Exception {
        DataDragonClient dragon = new DataDragonClient(Path.of(System.getProperty("user.home"), ".macromind", "cache"));
        dragon.load();
        if (!dragon.isLoaded()) {
            System.err.println("No item catalogue: running without advice. Restart once Data Dragon is reachable.");
        }
        GameStateMapper mapper = new GameStateMapper(new StatEstimator(dragon::championBaseStats),
                dragon.items(), dragon::championClass);
        AtomicReference<String> lastError = new AtomicReference<>();
        LiveClientPoller poller = new LiveClientPoller(LIVE_CLIENT, mapper, e -> logOnce(lastError, "Live client response could not be used: " + e));
        AdviceServer server = new AdviceServer(PORT);
        System.out.println("MacroMind advice on http://127.0.0.1:" + PORT + "/advice");

        AtomicReference<Map<Integer, Long>> firstSeen = new AtomicReference<>(Map.of());
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            try {
                Optional<GameState> state = poller.poll();
                if (state.isEmpty()) {
                    // keep firstSeen: a single failed poll must not re-trigger old CRITICAL advice
                    server.publish(false, List.of());
                    return;
                }
                List<BuildAdvice> advice = new BuildAdvisor(state.get(), dragon.items()).generateBuildAdvices();
                firstSeen.set(OverlayVisibility.track(firstSeen.get(), advice, state.get().gameTimeInSeconds()));
                server.publish(OverlayVisibility.shouldShow(state.get(), advice, firstSeen.get()), advice);
            } catch (Throwable e) {
                // a thrown exception would silently cancel the schedule, so nothing may escape this task
                System.err.println("Poll failed: " + e);
            }
        }, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }
}
