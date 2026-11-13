package com.macromind.app;

import com.macromind.domain.model.BuildAdvice;
import com.macromind.domain.model.GameState;
import com.macromind.domain.model.Item;
import com.macromind.domain.service.BuildAdvisor;
import com.macromind.domain.service.OverlayVisibility;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** One cycle of the app: read the game, work out the advice and whether to show it, and publish the result. */
final class AdvicePollLoop {

    private final Supplier<Optional<GameState>> poller;
    private final Supplier<List<Item>> catalogue;
    private final BiConsumer<Boolean, List<BuildAdvice>> publisher;
    private final Consumer<String> onError;

    // When each CRITICAL item first appeared; touched only by the single polling thread.
    private Map<Integer, Long> firstSeen = Map.of();

    AdvicePollLoop(Supplier<Optional<GameState>> poller, Supplier<List<Item>> catalogue,
                   BiConsumer<Boolean, List<BuildAdvice>> publisher, Consumer<String> onError) {
        this.poller = poller;
        this.catalogue = catalogue;
        this.publisher = publisher;
        this.onError = onError;
    }

    /** Runs one cycle. Never throws: an escaped exception would silently cancel a scheduled loop. */
    void pollOnce() {
        try {
            Optional<GameState> state = poller.get();
            if (state.isEmpty()) {
                // firstSeen is kept: one failed poll must not make old CRITICAL advice look new again
                publisher.accept(false, List.of());
                return;
            }
            publish(state.get());
        } catch (Throwable e) {
            onError.accept("Poll failed: " + e);
        }
    }

    private void publish(GameState state) {
        List<BuildAdvice> advice = new BuildAdvisor(state, catalogue.get()).generateBuildAdvices();
        firstSeen = OverlayVisibility.track(firstSeen, advice, state.gameTimeInSeconds());
        publisher.accept(OverlayVisibility.shouldShow(state, advice, firstSeen), advice);
    }
}
