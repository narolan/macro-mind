package com.macromind.app;

import com.macromind.api.AppStatus;
import com.macromind.domain.model.BuildAdvice;
import com.macromind.domain.model.GameState;
import com.macromind.domain.model.Item;
import com.macromind.domain.service.BuildAdvisor;
import com.macromind.domain.service.OverlayVisibility;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** One cycle of the app: read the game, work out the advice and whether to show it, and publish the result. */
final class AdvicePollLoop {

    /** Where results go: whether to show the overlay, the advice, and what state the app is in. */
    @FunctionalInterface
    interface Publisher {
        void publish(boolean visible, List<BuildAdvice> advice, AppStatus status);
    }

    private final Supplier<Optional<GameState>> poller;
    private final Supplier<List<Item>> catalogue;
    private final Publisher publisher;
    private final Consumer<String> onError;

    // When each CRITICAL item first appeared; touched only by the single polling thread.
    private Map<Integer, Long> firstSeen = Map.of();

    AdvicePollLoop(Supplier<Optional<GameState>> poller, Supplier<List<Item>> catalogue,
                   Publisher publisher, Consumer<String> onError) {
        this.poller = poller;
        this.catalogue = catalogue;
        this.publisher = publisher;
        this.onError = onError;
    }

    /** Runs one cycle. Never throws: an escaped exception would silently cancel a scheduled loop. */
    void pollOnce() {
        try {
            List<Item> items = catalogue.get();
            if (items.isEmpty()) {
                publisher.publish(false, List.of(), AppStatus.NO_DATA); // nothing can be advised without Riot's item data
                return;
            }
            Optional<GameState> state = poller.get();
            if (state.isEmpty()) {
                // firstSeen is kept: one failed poll must not make old CRITICAL advice look new again
                publisher.publish(false, List.of(), AppStatus.NO_GAME);
                return;
            }
            publish(state.get(), items);
        } catch (Throwable e) {
            onError.accept("Poll failed: " + e);
            publisher.publish(false, List.of(), AppStatus.ERROR);
        }
    }

    /** Only the items this mode's shop sells; if that leaves nothing the full list is used rather than no advice. */
    private static List<Item> soldIn(GameState state, List<Item> items) {
        List<Item> sold = items.stream().filter(item -> item.isAvailableIn(state.gameMode())).toList();
        return sold.isEmpty() ? items : sold;
    }

    private void publish(GameState state, List<Item> items) {
        List<BuildAdvice> advice = new BuildAdvisor(state, soldIn(state, items)).generateBuildAdvices();
        firstSeen = OverlayVisibility.track(firstSeen, advice, state.gameTimeInSeconds());
        publisher.publish(OverlayVisibility.shouldShow(state, advice, firstSeen), advice, AppStatus.IN_GAME);
    }
}
