package com.macromind.api;

import com.macromind.domain.model.Alert;
import com.macromind.domain.model.BuildAdvice;
import com.macromind.domain.model.ObjectiveTimer;

import java.util.List;

/** The whole /advice response. */
record AdvicePayload(boolean visible, List<AdviceView> advice, String iconVersion, AppStatus status,
                     List<AlertView> alerts, List<TimerView> timers) {

    static AdvicePayload of(boolean visible, List<BuildAdvice> advice, List<Alert> alerts, List<ObjectiveTimer> timers,
                            String iconVersion, AppStatus status) {
        return new AdvicePayload(visible, AdviceCaps.forOverlay(advice).stream().map(AdviceView::from).toList(), iconVersion, status,
                alerts.stream().map(AlertView::from).toList(), timers.stream().map(TimerView::from).toList());
    }
}
