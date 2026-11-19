package com.macromind.app;

import com.macromind.api.AdviceServer;
import com.macromind.domain.service.StatEstimator;
import com.macromind.infra.DataDragonClient;
import com.macromind.infra.GameStateMapper;
import com.macromind.infra.LiveClientPoller;

import com.macromind.domain.model.GameState;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** The composition root: builds the pieces, wires them together and starts the polling loop. */
public final class AppRunner {

    private static final System.Logger LOG = System.getLogger("macromind");

    private AppRunner() {}

    public static void start() throws IOException {
        AppConfig config = AppConfig.load();
        DataDragonClient dragon = new DataDragonClient(config.cacheDir());
        dragon.load();
        if (!dragon.isLoaded()) {
            LOG.log(System.Logger.Level.WARNING, "No item catalogue: running without advice. Restart once Data Dragon is reachable.");
        }

        GameStateMapper mapper = new GameStateMapper(new StatEstimator(dragon::championBaseStats),
                dragon.items(), dragon::championClass);
        AtomicReference<String> lastError = new AtomicReference<>();
        AtomicBoolean unusableResponse = new AtomicBoolean();
        LiveClientPoller liveClient = new LiveClientPoller(config.liveClientUrl(), mapper, e -> {
            unusableResponse.set(true);
            logOnce(lastError, "Live client response could not be used: " + e);
        });
        AdviceServer server = new AdviceServer(config.port(), dragon.version());
        LOG.log(System.Logger.Level.INFO, "MacroMind advice on http://127.0.0.1:{0}/advice", String.valueOf(server.port()));

        AdvicePollLoop loop = new AdvicePollLoop(() -> poll(liveClient, unusableResponse), dragon::items, server::publish,
                message -> logOnce(lastError, message));
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(namedThread("macromind-poll"));
        scheduler.scheduleWithFixedDelay(loop::pollOnce, 0, config.pollIntervalMillis(), TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdownNow();
            server.stop();
        }, "macromind-shutdown"));
    }

    // The poller reports an unusable response through its callback and returns empty; turn that into an exception so
    // the loop publishes ERROR instead of mistaking a broken integration for "no game running".
    private static Optional<GameState> poll(LiveClientPoller liveClient, AtomicBoolean unusableResponse) {
        unusableResponse.set(false);
        Optional<GameState> state = liveClient.poll();
        if (state.isEmpty() && unusableResponse.get()) {
            throw new IllegalStateException("the game client's response could not be read");
        }
        return state;
    }

    // The same failure repeats every second; print it once until it changes.
    private static void logOnce(AtomicReference<String> last, String message) {
        if (!message.equals(last.getAndSet(message))) {
            LOG.log(System.Logger.Level.WARNING, message);
        }
    }

    private static ThreadFactory namedThread(String name) {
        return runnable -> new Thread(runnable, name);
    }
}
