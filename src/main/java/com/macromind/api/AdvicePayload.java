package com.macromind.api;

import com.macromind.domain.model.BuildAdvice;

import java.util.List;

/** The whole /advice response. */
record AdvicePayload(boolean visible, List<AdviceView> advice, String iconVersion, AppStatus status) {

    static AdvicePayload of(boolean visible, List<BuildAdvice> advice, String iconVersion, AppStatus status) {
        return new AdvicePayload(visible, AdviceCaps.forOverlay(advice).stream().map(AdviceView::from).toList(), iconVersion, status);
    }
}
