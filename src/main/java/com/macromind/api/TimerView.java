package com.macromind.api;

import com.macromind.domain.model.ObjectiveTimer;

/** One objective countdown as the overlay receives it. The field names are a contract with overlay/src/viewModel.js. */
record TimerView(String name, long secondsLeft) {

    static TimerView from(ObjectiveTimer timer) {
        return new TimerView(timer.name(), timer.secondsLeft());
    }
}
