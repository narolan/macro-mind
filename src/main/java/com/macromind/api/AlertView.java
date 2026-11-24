package com.macromind.api;

import com.macromind.domain.model.Alert;

/** One alert as the overlay receives it. The field names are a contract with overlay/src/viewModel.js. */
record AlertView(String kind, String message, String priority) {

    static AlertView from(Alert alert) {
        return new AlertView(alert.kind().name(), alert.message(), alert.priority().name());
    }
}
