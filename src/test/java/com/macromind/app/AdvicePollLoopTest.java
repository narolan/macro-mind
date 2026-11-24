package com.macromind.app;

import com.macromind.api.AppStatus;
import com.macromind.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.macromind.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class AdvicePollLoopTest {

    /** What the loop published, in order. */
    private record Published(boolean visible, List<BuildAdvice> advice, AppStatus status) {}

    private final List<Published> published = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    private static final Item GRIEVOUS = itemWith("Mortal Reminder", 3000, itemStats(35, 0, 0, 0, 0, 0, 0, 0, 0), ItemTag.GRIEVOUS_WOUNDS);

    private static GameState soraka(long time, boolean dead) {
        Player me = player("Me", ChampionClass.MARKSMAN, false, List.of(), AVERAGE_STATS).withDead(dead);
        Player soraka = player("Soraka", ChampionClass.SUPPORT, true, List.of(), stats(50, 50, 0, 100));
        return gameAt(time, me, List.of(soraka));
    }

    private AdvicePollLoop loop(java.util.function.Supplier<Optional<GameState>> poller) {
        return new AdvicePollLoop(poller, () -> List.of(GRIEVOUS), (visible, advice, status) -> published.add(new Published(visible, advice, status)),
                errors::add);
    }

    @Test
    void neverAdvisesAnItemTheCurrentModesShopDoesNotSell() {
        Item riftOnly = new Item(1, "Mortal Reminder", 3000, GRIEVOUS.stats(), GRIEVOUS.tags(), java.util.Set.of(11));
        GameState aram = new GameState(1000, soraka(1000, true).activePlayer(), List.of(), soraka(1000, true).enemies(), GameMode.ARAM);
        AdvicePollLoop loop = new AdvicePollLoop(() -> Optional.of(aram), () -> List.of(riftOnly, itemWith("Other", 1, NO_STATS)),
                (visible, advice, status) -> published.add(new Published(visible, advice, status)), errors::add);

        loop.pollOnce();

        assertTrue(published.get(0).advice().stream().noneMatch(a -> a.recommendedItem().name().equals("Mortal Reminder")));
    }

    @Test
    void publishesHiddenAndEmptyWhenNoGameIsRunning() {
        loop(Optional::empty).pollOnce();

        assertEquals(1, published.size());
        assertFalse(published.get(0).visible());
        assertTrue(published.get(0).advice().isEmpty());
    }

    @Test
    void publishesAdviceAndShowsItWhenTheActivePlayerIsDead() {
        loop(() -> Optional.of(soraka(1000, true))).pollOnce();

        assertTrue(published.get(0).visible());
        assertEquals("Mortal Reminder", published.get(0).advice().get(0).recommendedItem().name());
    }

    @Test
    void showsNewCriticalAdviceOnlyForTheWindowThenHidesIt() {
        List<Optional<GameState>> polls = List.of(Optional.of(soraka(1000, false)), Optional.of(soraka(1010, false)),
                Optional.of(soraka(1100, false)));
        AtomicInteger next = new AtomicInteger();
        AdvicePollLoop loop = loop(() -> polls.get(next.getAndIncrement()));

        loop.pollOnce();
        loop.pollOnce();
        loop.pollOnce();

        assertEquals(List.of(true, true, false), published.stream().map(Published::visible).toList());
    }

    @Test
    void aFailedPollDoesNotForgetWhenCriticalAdviceFirstAppeared() {
        List<Optional<GameState>> polls = List.of(Optional.of(soraka(1000, false)), Optional.empty(), Optional.of(soraka(1100, false)));
        AtomicInteger next = new AtomicInteger();
        AdvicePollLoop loop = loop(() -> polls.get(next.getAndIncrement()));

        loop.pollOnce();
        loop.pollOnce();
        loop.pollOnce();

        // had the memory been wiped by the failed poll, the advice would look new again at t=1100 and show
        assertEquals(List.of(true, false, false), published.stream().map(Published::visible).toList());
    }

    @Test
    void aNewGameStartingResetsWhatCountsAsNew() {
        List<Optional<GameState>> polls = List.of(Optional.of(soraka(1500, false)), Optional.of(soraka(5, false)));
        AtomicInteger next = new AtomicInteger();
        AdvicePollLoop loop = loop(() -> polls.get(next.getAndIncrement()));

        loop.pollOnce();
        loop.pollOnce();

        assertTrue(published.get(1).visible(), "the same advice in a brand new game is new again");
    }

    @Test
    void anExceptionIsReportedAndNeverEscapes() {
        AdvicePollLoop loop = loop(() -> { throw new IllegalStateException("boom"); });

        assertDoesNotThrow(loop::pollOnce);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("boom"));
        assertEquals(List.of(AppStatus.ERROR), published.stream().map(Published::status).toList());
        assertFalse(published.get(0).visible());
    }

    @Test
    void theLoopKeepsWorkingAfterAnException() {
        AtomicInteger calls = new AtomicInteger();
        AdvicePollLoop loop = loop(() -> {
            if (calls.getAndIncrement() == 0) {
                throw new IllegalStateException("boom");
            }
            return Optional.empty();
        });

        loop.pollOnce();
        loop.pollOnce();

        assertEquals(List.of(AppStatus.ERROR, AppStatus.NO_GAME), published.stream().map(Published::status).toList());
    }

    @Test
    void readsTheItemCatalogueEveryPollSoALateLoadIsPickedUp() {
        List<List<Item>> catalogues = List.of(List.of(), List.of(GRIEVOUS));
        AtomicInteger next = new AtomicInteger();
        AdvicePollLoop loop = new AdvicePollLoop(() -> Optional.of(soraka(1000, true)),
                () -> catalogues.get(next.getAndIncrement()), (visible, advice, status) -> published.add(new Published(visible, advice, status)), errors::add);

        loop.pollOnce();
        loop.pollOnce();

        assertTrue(published.get(0).advice().isEmpty());
        assertFalse(published.get(1).advice().isEmpty());
    }

    @Test
    void reportsNoGameAsNoGameAndAMatchAsInGame() {
        List<Optional<GameState>> polls = List.of(Optional.empty(), Optional.of(soraka(1000, false)));
        AtomicInteger next = new AtomicInteger();
        AdvicePollLoop loop = loop(() -> polls.get(next.getAndIncrement()));

        loop.pollOnce();
        loop.pollOnce();

        assertEquals(List.of(AppStatus.NO_GAME, AppStatus.IN_GAME), published.stream().map(Published::status).toList());
    }

    @Test
    void reportsNoDataWhenTheItemCatalogueIsEmptyAndDoesNotEvenPollTheGame() {
        AtomicInteger polls = new AtomicInteger();
        AdvicePollLoop loop = new AdvicePollLoop(() -> { polls.incrementAndGet(); return Optional.of(soraka(1000, true)); },
                List::of, (visible, advice, status) -> published.add(new Published(visible, advice, status)), errors::add);

        loop.pollOnce();

        assertEquals(List.of(AppStatus.NO_DATA), published.stream().map(Published::status).toList());
        assertFalse(published.get(0).visible());
        assertEquals(0, polls.get());
    }

    @Test
    void recoversToInGameAfterAnError() {
        AtomicInteger calls = new AtomicInteger();
        AdvicePollLoop loop = loop(() -> {
            if (calls.getAndIncrement() == 0) {
                throw new IllegalStateException("boom");
            }
            return Optional.of(soraka(1000, true));
        });

        loop.pollOnce();
        loop.pollOnce();

        assertEquals(List.of(AppStatus.ERROR, AppStatus.IN_GAME), published.stream().map(Published::status).toList());
    }
}
